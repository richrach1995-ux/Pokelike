"""Referential-integrity validation for the generated content.

The runtime trusts its assets: `ContentRepository` does not defensively check
every id, because doing so on 250 species × 13 learnset entries would cost
startup time.  This validator is what earns that trust — it runs in the build
and in CI, and it fails the build on any dangling reference.

It also cross-checks against the Kotlin sources, so content can never reference
an ability effect, a move-effect type or an enum value the engine does not
implement.
"""

from __future__ import annotations

import os
import re

from common import (
    ELEMENTS, RARITIES, REPO_ROOT, STATUSES, WEATHERS, WORLDS, Strings,
)

DOMAIN_SRC = os.path.join(REPO_ROOT, "domain", "src", "main", "kotlin", "com", "runeveil", "saga", "domain")


def _kotlin_source(*parts: str) -> str:
    path = os.path.join(DOMAIN_SRC, *parts)
    with open(path, encoding="utf-8") as handle:
        return handle.read()


def _known_ability_effects() -> set[str]:
    """Parses ``KNOWN_EFFECT_IDS`` out of AbilityEffects.kt."""
    source = _kotlin_source("battle", "AbilityEffects.kt")
    block = re.search(r"KNOWN_EFFECT_IDS: Set<String> = setOf\((.*?)\)", source, re.S)
    if not block:
        raise RuntimeError("could not parse KNOWN_EFFECT_IDS from AbilityEffects.kt")
    return set(re.findall(r'"([a-z_]+)"', block.group(1)))


def _known_move_effects() -> set[str]:
    """Parses the MoveEffect subclass names out of Move.kt."""
    source = _kotlin_source("model", "battle", "Move.kt")
    return set(re.findall(r"data (?:class|object) (\w+)\s*[(:]", source))


def _known_enum(path_parts: tuple[str, ...], enum_name: str) -> set[str]:
    source = _kotlin_source(*path_parts)
    block = re.search(rf"enum class {enum_name}\b.*?\{{(.*?)\n\}}", source, re.S)
    if not block:
        raise RuntimeError(f"could not parse enum {enum_name}")
    return set(re.findall(r"^\s{4}([A-Z][A-Z_0-9]*)\s*[(,;]", block.group(1), re.M))


def validate(content: dict, strings: Strings) -> list[str]:
    errors: list[str] = []

    move_ids = {m["id"] for m in content["moves"]}
    species_ids = {s["id"] for s in content["monsters"]}
    item_ids = {i["id"] for i in content["items"]}
    ability_ids = {a["id"] for a in content["abilities"]}
    npc_ids = {n["id"] for n in content["npcs"]}
    quest_ids = {q["id"] for q in content["quests"]}
    faction_ids = {f["id"] for f in content["factions"]}
    shop_ids = {s["id"] for s in content["shops"]}
    talent_tree_ids = set(content["talents"].keys())
    location_ids = {
        loc["id"] for region in content["regions"] for loc in region["locations"]
    }
    table_ids = {t["id"] for t in content["encounters"]}
    cutscene_ids = {c["id"] for c in content["cutscenes"]}
    dialogue_ids = {d["id"] for d in content["dialogues"]}
    team_ids = {t["id"] for t in content["trainers"]}

    def check(condition: bool, message: str) -> None:
        if not condition:
            errors.append(message)

    def key_exists(key, owner):
        if key and key not in strings.de:
            errors.append(f"{owner}: fehlender Übersetzungsschlüssel '{key}'")

    # --- uniqueness ------------------------------------------------------
    for name, collection in (
        ("moves", content["moves"]), ("monsters", content["monsters"]),
        ("items", content["items"]), ("npcs", content["npcs"]),
        ("quests", content["quests"]), ("recipes", content["recipes"]),
    ):
        ids = [entry["id"] for entry in collection]
        duplicates = {entry for entry in ids if ids.count(entry) > 1}
        check(not duplicates, f"{name}: doppelte IDs {sorted(duplicates)[:5]}")

    dex_numbers = [s["dexNumber"] for s in content["monsters"]]
    check(len(dex_numbers) == len(set(dex_numbers)), "monsters: doppelte Dex-Nummern")

    # --- moves -----------------------------------------------------------
    known_move_effects = _known_move_effects()
    for move in content["moves"]:
        check(move["element"] in ELEMENTS, f"move {move['id']}: unbekanntes Element")
        check(move["category"] in ("PHYSICAL", "MAGICAL", "SUPPORT"),
              f"move {move['id']}: unbekannte Kategorie")
        check(0 <= move["accuracy"] <= 100, f"move {move['id']}: Genauigkeit außerhalb 0..100")
        check(move["maxPp"] > 0, f"move {move['id']}: AP müssen positiv sein")
        check(-7 <= move["priority"] <= 5, f"move {move['id']}: Priorität außerhalb -7..5")
        check(move["power"] >= 0, f"move {move['id']}: negative Stärke")
        if move["category"] == "SUPPORT":
            check(move["power"] == 0, f"move {move['id']}: Support-Attacke mit Stärke")
        else:
            check(move["power"] > 0 or move["effects"],
                  f"move {move['id']}: Angriff ohne Stärke und ohne Effekt")
        key_exists(move["nameKey"], f"move {move['id']}")
        key_exists(move["descriptionKey"], f"move {move['id']}")
        for effect in move["effects"]:
            check(effect["type"] in known_move_effects,
                  f"move {move['id']}: unbekannter Effekt '{effect['type']}'")
            if effect["type"] == "InflictStatus":
                check(effect["condition"] in STATUSES,
                      f"move {move['id']}: unbekannter Status")
            if effect["type"] == "SetWeather":
                check(effect["weather"] in WEATHERS,
                      f"move {move['id']}: unbekanntes Wetter")

    # --- abilities -------------------------------------------------------
    known_effects = _known_ability_effects()
    for ability in content["abilities"]:
        check(ability["effectId"] in known_effects,
              f"ability {ability['id']}: Effekt '{ability['effectId']}' ist in "
              f"AbilityEffects.kt nicht implementiert")
        key_exists(ability["nameKey"], f"ability {ability['id']}")

    # --- monsters --------------------------------------------------------
    family_stages: dict[str, set[int]] = {}
    for species in content["monsters"]:
        prefix = f"monster {species['id']}"
        check(species["primaryElement"] in ELEMENTS, f"{prefix}: unbekanntes Primärelement")
        check(species["secondaryElement"] in ELEMENTS or species["secondaryElement"] is None,
              f"{prefix}: unbekanntes Sekundärelement")
        check(species["secondaryElement"] != species["primaryElement"],
              f"{prefix}: identisches Primär- und Sekundärelement")
        check(species["rarity"] in RARITIES, f"{prefix}: unbekannte Seltenheit")
        check(species["nativeWorld"] in WORLDS, f"{prefix}: unbekannte Welt")
        check(1 <= species["catchRate"] <= 255, f"{prefix}: Fangrate außerhalb 1..255")
        total = sum(species["baseStats"].values())
        check(250 <= total <= 780, f"{prefix}: Basiswertsumme {total} außerhalb 250..780")
        for stat, value in species["baseStats"].items():
            check(5 <= value <= 190, f"{prefix}: Wert {stat}={value} außerhalb 5..190")
        check(species["talentTreeId"] in talent_tree_ids, f"{prefix}: unbekannter Talentbaum")
        key_exists(species["nameKey"], prefix)
        key_exists(species["loreKey"], prefix)

        for ability_id in species["abilityIds"]:
            check(ability_id in ability_ids, f"{prefix}: unbekannte Fähigkeit {ability_id}")
        if species["hiddenAbilityId"]:
            check(species["hiddenAbilityId"] in ability_ids,
                  f"{prefix}: unbekannte versteckte Fähigkeit")

        check(bool(species["learnset"]), f"{prefix}: leere Lernliste")
        check(any(entry["level"] <= 1 for entry in species["learnset"]),
              f"{prefix}: kennt auf Stufe 1 keine Attacke")
        for entry in species["learnset"]:
            check(entry["moveId"] in move_ids, f"{prefix}: unbekannte Attacke {entry['moveId']}")
            check(1 <= entry["level"] <= 100, f"{prefix}: Lernstufe außerhalb 1..100")
        for move_id in species["tutorMoveIds"] + species["eggMoveIds"]:
            check(move_id in move_ids, f"{prefix}: unbekannte Attacke {move_id}")

        for evolution in species["evolutions"]:
            check(evolution["targetSpeciesId"] in species_ids,
                  f"{prefix}: Entwicklung zu unbekannter Art {evolution['targetSpeciesId']}")
            check(evolution["targetSpeciesId"] != species["id"],
                  f"{prefix}: Entwicklung auf sich selbst")
            if evolution.get("requiredItemId"):
                check(evolution["requiredItemId"] in item_ids,
                      f"{prefix}: Entwicklung braucht unbekannten Gegenstand")
            key_exists(evolution["descriptionKey"], prefix)

        family_stages.setdefault(species["familyId"], set()).add(species["stage"])

    for family_id, stages in family_stages.items():
        check(min(stages) == 1, f"Familie {family_id}: beginnt nicht mit Stufe 1")
        check(stages == set(range(1, max(stages) + 1)),
              f"Familie {family_id}: Lücke in den Entwicklungsstufen {sorted(stages)}")

    # --- items and recipes ----------------------------------------------
    for item in content["items"]:
        key_exists(item["nameKey"], f"item {item['id']}")
        check(item["price"] >= 0, f"item {item['id']}: negativer Preis")
        check(item["stackLimit"] >= 1, f"item {item['id']}: Stapelgröße < 1")
        if item["category"] == "RUNE_ORB":
            check("orbSpec" in item, f"item {item['id']}: Runenkugel ohne orbSpec")
        if item["category"] == "EQUIPMENT":
            check("equipmentSpec" in item, f"item {item['id']}: Ausrüstung ohne equipmentSpec")

    for recipe in content["recipes"]:
        check(recipe["resultItemId"] in item_ids,
              f"recipe {recipe['id']}: unbekanntes Ergebnis")
        for ingredient in recipe["ingredients"]:
            check(ingredient in item_ids, f"recipe {recipe['id']}: unbekannte Zutat {ingredient}")
        check(0.0 < recipe["successChance"] <= 1.0,
              f"recipe {recipe['id']}: Erfolgschance außerhalb (0,1]")

    # --- world -----------------------------------------------------------
    for region in content["regions"]:
        prefix = f"region {region['id']}"
        check(region["world"] in WORLDS, f"{prefix}: unbekannte Welt")
        check(region["minLevel"] < region["maxLevel"], f"{prefix}: ungültiger Stufenbereich")
        key_exists(region["nameKey"], prefix)
        region_locations = {loc["id"] for loc in region["locations"]}
        check(any(loc["type"] == "TOWN" for loc in region["locations"]),
              f"{prefix}: keine Siedlung")
        check(any(loc["type"] == "SANCTUARY" for loc in region["locations"]),
              f"{prefix}: kein Heiligtum")
        for loc in region["locations"]:
            key_exists(loc["nameKey"], f"location {loc['id']}")
            if loc.get("encounterTableId"):
                check(loc["encounterTableId"] in table_ids,
                      f"location {loc['id']}: unbekannte Begegnungstabelle")
                check(loc.get("encounterRate", 0) > 0,
                      f"location {loc['id']}: Tabelle ohne Begegnungsrate")
            if loc.get("shopId"):
                check(loc["shopId"] in shop_ids, f"location {loc['id']}: unbekannter Laden")
        for link in region["links"]:
            check(link["fromLocationId"] in region_locations,
                  f"{prefix}: Verbindung von unbekanntem Ort {link['fromLocationId']}")
            check(link["toLocationId"] in region_locations,
                  f"{prefix}: Verbindung zu unbekanntem Ort {link['toLocationId']}")
        # Every location must be reachable from the first one.
        reachable = {region["locations"][0]["id"]}
        changed = True
        while changed:
            changed = False
            for link in region["links"]:
                if link["fromLocationId"] in reachable and link["toLocationId"] not in reachable:
                    reachable.add(link["toLocationId"])
                    changed = True
                if link.get("bidirectional", True) and link["toLocationId"] in reachable \
                        and link["fromLocationId"] not in reachable:
                    reachable.add(link["fromLocationId"])
                    changed = True
        unreachable = region_locations - reachable
        check(not unreachable, f"{prefix}: nicht erreichbare Orte {sorted(unreachable)}")

    for table in content["encounters"]:
        check(bool(table["entries"]), f"encounter {table['id']}: leer")
        for entry in table["entries"]:
            check(entry["speciesId"] in species_ids,
                  f"encounter {table['id']}: unbekannte Art {entry['speciesId']}")
            check(entry["minLevel"] <= entry["maxLevel"],
                  f"encounter {table['id']}: min > max")
            check(entry["weight"] > 0, f"encounter {table['id']}: Gewicht <= 0")

    for shop in content["shops"]:
        for item_id in shop["itemIds"]:
            check(item_id in item_ids, f"shop {shop['id']}: unbekannter Gegenstand {item_id}")

    for chest in content["chests"]:
        check(chest["locationId"] in location_ids, f"chest {chest['id']}: unbekannter Ort")
        for item_id in chest["itemIds"]:
            check(item_id in item_ids, f"chest {chest['id']}: unbekannter Gegenstand {item_id}")

    # --- npcs, trainers, dialogue ---------------------------------------
    for npc in content["npcs"]:
        prefix = f"npc {npc['id']}"
        check(npc["locationId"] in location_ids, f"{prefix}: unbekannter Ort {npc['locationId']}")
        key_exists(npc["nameKey"], prefix)
        if npc.get("factionId"):
            check(npc["factionId"] in faction_ids, f"{prefix}: unbekannte Fraktion")
        if npc.get("shopId"):
            check(npc["shopId"] in shop_ids, f"{prefix}: unbekannter Laden")
        if npc.get("trainerTeamId"):
            check(npc["trainerTeamId"] in team_ids, f"{prefix}: unbekanntes Team")
        for tree_id in npc["dialogueTreeIds"]:
            check(tree_id in dialogue_ids, f"{prefix}: unbekannter Dialogbaum")

    for team in content["trainers"]:
        check(team["npcId"] in npc_ids, f"trainer {team['id']}: unbekannter NPC")
        check(bool(team["members"]), f"trainer {team['id']}: leeres Team")
        for member in team["members"]:
            check(member["speciesId"] in species_ids,
                  f"trainer {team['id']}: unbekannte Art {member['speciesId']}")
            check(1 <= member["level"] <= 100, f"trainer {team['id']}: Stufe außerhalb 1..100")

    for tree in content["dialogues"]:
        node_ids = {node["id"] for node in tree["nodes"]}
        check(tree["entryNodeId"] in node_ids,
              f"dialogue {tree['id']}: Startknoten fehlt")
        for node in tree["nodes"]:
            key_exists(node["textKey"], f"dialogue {tree['id']}")
            if node.get("nextNodeId"):
                check(node["nextNodeId"] in node_ids,
                      f"dialogue {tree['id']}: Knoten {node['id']} zeigt ins Leere")
            for choice in node["choices"]:
                key_exists(choice["textKey"], f"dialogue {tree['id']}")
                if choice.get("nextNodeId"):
                    check(choice["nextNodeId"] in node_ids,
                          f"dialogue {tree['id']}: Auswahl {choice['id']} zeigt ins Leere")

    # --- story -----------------------------------------------------------
    for chapter in content["chapters"]:
        prefix = f"chapter {chapter['number']}"
        key_exists(chapter["titleKey"], prefix)
        for quest_id in chapter["mainQuestIds"]:
            check(quest_id in quest_ids, f"{prefix}: unbekannte Hauptquest {quest_id}")
        for cutscene_id in (chapter["openingCutsceneId"], chapter["closingCutsceneId"]):
            check(cutscene_id in cutscene_ids, f"{prefix}: unbekannte Zwischensequenz")

    for quest in content["quests"]:
        prefix = f"quest {quest['id']}"
        key_exists(quest["nameKey"], prefix)
        check(bool(quest["objectives"]), f"{prefix}: keine Ziele")
        for objective in quest["objectives"]:
            key_exists(objective["descriptionKey"], prefix)
            check(objective["requiredCount"] >= 1, f"{prefix}: Zielanzahl < 1")
        for prerequisite in quest["prerequisiteQuestIds"]:
            check(prerequisite in quest_ids, f"{prefix}: unbekannte Vorbedingung {prerequisite}")
        if quest.get("nextQuestId"):
            check(quest["nextQuestId"] in quest_ids, f"{prefix}: unbekannte Folgequest")
        if quest.get("giverNpcId"):
            check(quest["giverNpcId"] in npc_ids, f"{prefix}: unbekannter Auftraggeber")
        for reward in quest["rewards"]:
            if reward["type"] == "Items":
                for item_id in reward["itemIds"]:
                    check(item_id in item_ids, f"{prefix}: unbekannte Belohnung {item_id}")
            if reward["type"] == "Reputation":
                check(reward["factionId"] in faction_ids, f"{prefix}: unbekannte Fraktion")

    for ending in content["endings"]:
        check(ending["cutsceneId"] in cutscene_ids,
              f"ending {ending['id']}: unbekannte Zwischensequenz")
        key_exists(ending["titleKey"], f"ending {ending['id']}")

    # --- localisation completeness ---------------------------------------
    missing_en = set(strings.de) - set(strings.en)
    check(not missing_en, f"i18n: {len(missing_en)} Schlüssel ohne englische Übersetzung")

    # --- content volume guarantees ---------------------------------------
    check(len(content["monsters"]) >= 250,
          f"zu wenige Monster: {len(content['monsters'])} < 250")
    check(len(content["moves"]) >= 300, f"zu wenige Attacken: {len(content['moves'])} < 300")
    check(len(content["npcs"]) >= 100, f"zu wenige NPCs: {len(content['npcs'])} < 100")
    check(len(content["regions"]) == 9, "es müssen genau neun Regionen sein")

    return errors
