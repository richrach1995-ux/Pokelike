"""Generates abilities, items, runes, equipment, recipes, talent trees,
titles and achievements."""

from __future__ import annotations

from common import ELEMENT_DE, ELEMENT_EN, ELEMENTS, ROLE_WEIGHTS, Strings, slug

# ---------------------------------------------------------------------------
# Abilities
# ---------------------------------------------------------------------------
# Every entry maps to an effect id implemented in AbilityEffects.kt. The build
# script cross-checks this list against the Kotlin source, so the two can never
# drift apart.

ABILITIES = [
    ("ember_hide", "Glutfell", "Ember Hide", "Halbiert erlittenen Feuerschaden.",
     "Halves incoming Fire damage.", "ON_TAKING_DAMAGE"),
    ("frost_hide", "Frostfell", "Frost Hide", "Halbiert erlittenen Eisschaden.",
     "Halves incoming Ice damage.", "ON_TAKING_DAMAGE"),
    ("storm_hide", "Sturmfell", "Storm Hide", "Halbiert erlittenen Donnerschaden.",
     "Halves incoming Thunder damage.", "ON_TAKING_DAMAGE"),
    ("stone_hide", "Steinhaut", "Stone Hide", "Halbiert erlittenen Erdschaden.",
     "Halves incoming Earth damage.", "ON_TAKING_DAMAGE"),
    ("gale_hide", "Windhaut", "Gale Hide", "Halbiert erlittenen Windschaden.",
     "Halves incoming Wind damage.", "ON_TAKING_DAMAGE"),
    ("rune_ward", "Runenschutz", "Rune Ward", "Verringert sehr effektive Treffer um ein Viertel.",
     "Reduces super-effective hits by a quarter.", "ON_TAKING_DAMAGE"),
    ("giant_frame", "Riesenwuchs", "Giant Frame",
     "Verringert allen Schaden um 10 % und macht immun gegen Wetterschaden.",
     "Reduces all damage by 10 % and grants immunity to weather chip damage.", "ON_TAKING_DAMAGE"),
    ("bulwark", "Bollwerk", "Bulwark", "Verringert physischen Schaden um ein Viertel.",
     "Reduces physical damage by a quarter.", "ON_TAKING_DAMAGE"),
    ("spirit_veil", "Geisterschleier", "Spirit Veil", "Verringert magischen Schaden um ein Viertel.",
     "Reduces magical damage by a quarter.", "ON_TAKING_DAMAGE"),
    ("runic_bulwark", "Runenbollwerk", "Runic Bulwark",
     "Halbiert Schaden, solange die KP voll sind.",
     "Halves damage while at full HP.", "ON_TAKING_DAMAGE"),
    ("iron_will", "Eiserner Wille", "Iron Will", "Macht immun gegen Schlaf und Verwirrung.",
     "Grants immunity to sleep and confusion.", "ON_STATUS_APPLIED"),
    ("clear_mind", "Klarer Geist", "Clear Mind", "Macht immun gegen Verwirrung.",
     "Grants immunity to confusion.", "ON_STATUS_APPLIED"),
    ("divine_grace", "Göttliche Gnade", "Divine Grace",
     "Macht immun gegen Flüche und verkürzt Statusveränderungen um eine Runde.",
     "Grants curse immunity and shortens status conditions by one turn.", "ON_STATUS_APPLIED"),
    ("chaos_heart", "Chaosherz", "Chaos Heart",
     "Immun gegen Flüche; Chaos-Attacken richten 20 % mehr Schaden an.",
     "Curse immune; Chaos moves deal 20 % more damage.", "ON_DEALING_DAMAGE"),
    ("cleansing_light", "Reinigendes Licht", "Cleansing Light",
     "Heilt die eigene Statusveränderung mit 30 % Chance pro Runde.",
     "30 % chance each turn to cure its own status condition.", "ON_TURN_END"),
    ("regrowth", "Nachwuchs", "Regrowth", "Regeneriert jede Runde ein Sechzehntel der KP.",
     "Regenerates one sixteenth of its HP each turn.", "ON_TURN_END"),
    ("sun_drinker", "Sonnentrinker", "Sun Drinker",
     "Heilt bei Hitze stark und ignoriert deren Schaden.",
     "Heals strongly in a heatwave and ignores its damage.", "ON_TURN_END"),
    ("moon_drinker", "Mondtrinker", "Moon Drinker", "Heilt nachts jede Runde.",
     "Heals each turn at night.", "ON_TURN_END"),
    ("storm_born", "Sturmgeboren", "Storm Born", "Bei Regen und Gewitter deutlich schneller.",
     "Much faster in rain and thunderstorms.", "PASSIVE_STAT"),
    ("snow_walker", "Schneegänger", "Snow Walker",
     "Im Schnee schneller, schwerer zu treffen und immun gegen Schneeschaden.",
     "Faster, harder to hit and immune to chip damage in snow.", "PASSIVE_STAT"),
    ("sand_veil", "Sandschleier", "Sand Veil",
     "Im Sandsturm schwerer zu treffen und besser geschützt.",
     "Harder to hit and better protected in a sandstorm.", "PASSIVE_STAT"),
    ("gale_step", "Windschritt", "Gale Step", "Erhöht dauerhaft die Ausweichrate.",
     "Permanently raises evasion.", "PASSIVE_STAT"),
    ("venom_touch", "Giftberührung", "Venom Touch",
     "Berührungsangriffe auf dieses Monster können vergiften.",
     "Contact moves against it may poison the attacker.", "ON_TAKING_DAMAGE"),
    ("ember_touch", "Glutberührung", "Ember Touch",
     "Berührungsangriffe auf dieses Monster können verbrennen.",
     "Contact moves against it may burn the attacker.", "ON_TAKING_DAMAGE"),
    ("frost_touch", "Frostberührung", "Frost Touch",
     "Berührungsangriffe auf dieses Monster können einfrieren.",
     "Contact moves against it may freeze the attacker.", "ON_TAKING_DAMAGE"),
    ("curse_touch", "Fluchberührung", "Curse Touch",
     "Berührungsangriffe auf dieses Monster können verfluchen.",
     "Contact moves against it may curse the attacker.", "ON_TAKING_DAMAGE"),
    ("thorn_mail", "Dornenpanzer", "Thorn Mail",
     "Wirft ein Achtel des Schadens von Berührungsangriffen zurück.",
     "Reflects one eighth of the damage of contact moves.", "ON_TAKING_DAMAGE"),
    ("soul_siphon", "Seelenzehrer", "Soul Siphon",
     "Heilt sich um ein Achtel des zugefügten Schadens.",
     "Heals for one eighth of the damage it deals.", "ON_AFTER_DAMAGE"),
    ("arcane_focus", "Arkane Sammlung", "Arcane Focus", "Magische Attacken richten 15 % mehr Schaden an.",
     "Magical moves deal 15 % more damage.", "ON_DEALING_DAMAGE"),
    ("brute_force", "Rohe Gewalt", "Brute Force", "Physische Attacken richten 15 % mehr Schaden an.",
     "Physical moves deal 15 % more damage.", "ON_DEALING_DAMAGE"),
    ("berserker", "Berserker", "Berserker",
     "Unter einem Drittel der KP richten physische Attacken 50 % mehr Schaden an.",
     "Below a third HP, physical moves deal 50 % more damage.", "ON_DEALING_DAMAGE"),
    ("last_stand", "Letzter Stand", "Last Stand",
     "Überlebt einen tödlichen Treffer pro Kampf mit einem KP.",
     "Survives one lethal hit per battle with 1 HP.", "ON_FAINT"),
    ("keen_eye", "Scharfes Auge", "Keen Eye",
     "Die eigene Genauigkeit kann nicht gesenkt werden; erhöhte kritische Trefferchance.",
     "Its accuracy cannot be lowered; raised critical-hit rate.", "PASSIVE_STAT"),
    ("rune_siphon", "Runenzehr", "Rune Siphon", "Stellt gelegentlich Attackenpunkte wieder her.",
     "Occasionally restores move points.", "ON_TURN_END"),
    ("call_rain", "Regenruf", "Rain Call", "Ruft beim Betreten des Feldes Regen herbei.",
     "Summons rain when it enters the field.", "ON_BATTLE_START"),
    ("call_snow", "Schneeruf", "Snow Call", "Ruft beim Betreten des Feldes Schnee herbei.",
     "Summons snow when it enters the field.", "ON_BATTLE_START"),
    ("call_sand", "Sandruf", "Sand Call", "Ruft beim Betreten des Feldes einen Sandsturm herbei.",
     "Summons a sandstorm when it enters the field.", "ON_BATTLE_START"),
    ("call_heat", "Hitzeruf", "Heat Call", "Ruft beim Betreten des Feldes Hitze herbei.",
     "Summons a heatwave when it enters the field.", "ON_BATTLE_START"),
    ("call_aurora", "Nordlichtruf", "Aurora Call", "Ruft beim Betreten des Feldes ein Nordlicht herbei.",
     "Summons an aurora when it enters the field.", "ON_BATTLE_START"),
    ("intimidating_roar", "Einschüchterndes Brüllen", "Intimidating Roar",
     "Senkt beim Betreten des Feldes den Angriff aller Gegner.",
     "Lowers every opponent's Attack when it enters the field.", "ON_BATTLE_START"),
    ("lucky_star", "Glücksstern", "Lucky Star",
     "Erhöht kritische Treffer und die Fangchance des Trainers.",
     "Raises critical hits and the trainer's capture chance.", "PASSIVE_STAT"),
    ("swift_strike", "Schnellschlag", "Swift Strike",
     "Attacken mit Erstschlag richten 20 % mehr Schaden an.",
     "Priority moves deal 20 % more damage.", "ON_DEALING_DAMAGE"),
]


def build_abilities(strings: Strings) -> list[dict]:
    result = []
    for effect_id, name_de, name_en, desc_de, desc_en, trigger in ABILITIES:
        ability_id = f"ab_{effect_id}"
        strings.add(f"{ability_id}_name", name_de, name_en)
        strings.add(f"{ability_id}_desc", desc_de, desc_en)
        result.append({
            "id": ability_id,
            "nameKey": f"{ability_id}_name",
            "descriptionKey": f"{ability_id}_desc",
            "trigger": trigger,
            "effectId": effect_id,
            "magnitude": 1.0,
        })
    return result


# ---------------------------------------------------------------------------
# Items
# ---------------------------------------------------------------------------

def _item(item_id, category, price, name_de, name_en, desc_de, desc_en, strings,
          effects=None, stack=99, in_battle=False, on_monster=True, consumed=True,
          orb=None, equipment=None, rune=None, tier=1, quest_id=None):
    strings.add(f"{item_id}_name", name_de, name_en)
    strings.add(f"{item_id}_desc", desc_de, desc_en)
    payload = {
        "id": item_id,
        "nameKey": f"{item_id}_name",
        "descriptionKey": f"{item_id}_desc",
        "category": category,
        "price": price,
        "stackLimit": stack,
        "usableInBattle": in_battle,
        "usableOnMonster": on_monster,
        "consumedOnUse": consumed,
        "effects": effects or [],
        "iconKey": f"ic_{item_id}",
        "rarityTier": tier,
    }
    if orb:
        payload["orbSpec"] = orb
    if equipment:
        payload["equipmentSpec"] = equipment
    if rune:
        payload["runeSpec"] = rune
    if quest_id:
        payload["questId"] = quest_id
    return payload


ORB_GRADES = [
    ("wood", "Holz-Runenkugel", "Wooden Rune Orb", "WOOD", 1.0, 120, "NONE", 1.0),
    ("stone", "Stein-Runenkugel", "Stone Rune Orb", "STONE", 1.35, 320, "LOW_TARGET_HP", 1.6),
    ("iron", "Eisen-Runenkugel", "Iron Rune Orb", "IRON", 1.7, 700, "TARGET_HAS_STATUS", 1.7),
    ("silver", "Silber-Runenkugel", "Silver Rune Orb", "SILVER", 2.1, 1400, "AT_NIGHT", 2.0),
    ("gold", "Gold-Runenkugel", "Golden Rune Orb", "GOLD", 2.6, 2600, "HIGH_LEVEL_TARGET", 1.9),
    ("mythic", "Mythische Runenkugel", "Mythic Rune Orb", "MYTHIC", 3.2, 5200, "LONG_BATTLE", 2.2),
    ("divine", "Göttliche Runenkugel", "Divine Rune Orb", "DIVINE", 3.9, 9000, "MATCHING_WORLD", 2.4),
    ("legend", "Legendäre Runenkugel", "Legendary Rune Orb", "LEGENDARY", 4.8, 0, "LEGENDARY_TARGET", 2.8),
]

HEALING = [
    ("elixier_klein", "Kleines Elixier", "Minor Elixir", 200, [{"type": "RestoreHp", "amount": 60}],
     "Stellt 60 KP eines Monsters wieder her.", "Restores 60 HP to one monster.", 1),
    ("elixier", "Elixier", "Elixir", 500, [{"type": "RestoreHp", "amount": 150}],
     "Stellt 150 KP wieder her.", "Restores 150 HP.", 2),
    ("elixier_gross", "Großes Elixier", "Greater Elixir", 1100, [{"type": "RestoreHp", "amount": 320}],
     "Stellt 320 KP wieder her.", "Restores 320 HP.", 3),
    ("elixier_meister", "Meisterelixier", "Master Elixir", 2400,
     [{"type": "RestoreHpFraction", "fraction": 1.0}],
     "Stellt alle KP wieder her.", "Fully restores HP.", 4),
    ("balsam_brand", "Brandbalsam", "Burn Balm", 260,
     [{"type": "CureStatus", "conditions": ["BURN"]}],
     "Heilt Verbrennungen.", "Cures burns.", 1),
    ("balsam_gift", "Giftbalsam", "Antivenom", 260,
     [{"type": "CureStatus", "conditions": ["POISON"]}],
     "Heilt Vergiftungen.", "Cures poison.", 1),
    ("weckruf", "Weckruf", "Waking Horn", 240,
     [{"type": "CureStatus", "conditions": ["SLEEP"]}],
     "Weckt ein schlafendes Monster.", "Wakes a sleeping monster.", 1),
    ("tauwasser", "Tauwasser", "Thawing Water", 260,
     [{"type": "CureStatus", "conditions": ["FREEZE"]}],
     "Taut ein eingefrorenes Monster auf.", "Thaws a frozen monster.", 1),
    ("klarheitstee", "Klarheitstee", "Clarity Tea", 280,
     [{"type": "CureStatus", "conditions": ["CONFUSION"]}],
     "Beendet Verwirrung.", "Ends confusion.", 1),
    ("verband", "Runenverband", "Rune Bandage", 300,
     [{"type": "CureStatus", "conditions": ["BLEED"]}],
     "Stoppt Blutungen.", "Stops bleeding.", 1),
    ("bannwasser", "Bannwasser", "Warding Water", 420,
     [{"type": "CureStatus", "conditions": ["CURSE"]}],
     "Bricht einen Fluch.", "Breaks a curse.", 2),
    ("nervenharz", "Nervenharz", "Nerve Resin", 300,
     [{"type": "CureStatus", "conditions": ["PARALYSIS"]}],
     "Löst Lähmungen.", "Cures paralysis.", 1),
    ("allheilkraut", "Allheilkraut", "Panacea Herb", 900, [{"type": "CureStatus", "conditions": []}],
     "Heilt jede Statusveränderung.", "Cures every status condition.", 3),
    ("lebensfunke", "Lebensfunke", "Life Spark", 1500, [{"type": "Revive", "hpFraction": 0.5}],
     "Belebt ein besiegtes Monster mit der Hälfte seiner KP wieder.",
     "Revives a fainted monster with half its HP.", 3),
    ("lebensflamme", "Lebensflamme", "Life Flame", 3200, [{"type": "Revive", "hpFraction": 1.0}],
     "Belebt ein besiegtes Monster vollständig wieder.",
     "Fully revives a fainted monster.", 4),
    ("aether_klein", "Kleiner Äther", "Minor Aether", 400, [{"type": "RestorePp", "amount": 10}],
     "Stellt 10 AP einer Attacke wieder her.", "Restores 10 PP of one move.", 2),
    ("aether_gross", "Großer Äther", "Greater Aether", 1600,
     [{"type": "RestorePp", "amount": 20, "allMoves": True}],
     "Stellt 20 AP aller Attacken wieder her.", "Restores 20 PP of every move.", 3),
]

VITAMINS = [
    ("kraftmet", "Kraftmet", "Might Mead", "attack", "Angriff", "Attack"),
    ("wehrmet", "Wehrmet", "Ward Mead", "defense", "Verteidigung", "Defense"),
    ("weisheitsmet", "Weisheitsmet", "Wisdom Mead", "magic", "Magie", "Magic"),
    ("bannmet", "Bannmet", "Warding Mead", "resistance", "Magieabwehr", "Resistance"),
    ("windmet", "Windmet", "Wind Mead", "speed", "Initiative", "Speed"),
    ("gluecksmet", "Glücksmet", "Fortune Mead", "luck", "Glück", "Luck"),
    ("lebensmet", "Lebensmet", "Vital Mead", "hp", "KP", "HP"),
]

BATTLE_BOOSTERS = [
    ("kampfruf_angriff", "Kampfruf: Zorn", "War Cry: Wrath", "ATTACK"),
    ("kampfruf_wehr", "Kampfruf: Wehr", "War Cry: Ward", "DEFENSE"),
    ("kampfruf_magie", "Kampfruf: Seidr", "War Cry: Seidr", "MAGIC"),
    ("kampfruf_bann", "Kampfruf: Bann", "War Cry: Banish", "RESISTANCE"),
    ("kampfruf_hast", "Kampfruf: Hast", "War Cry: Haste", "SPEED"),
]

EVOLUTION_STONES = [
    ("stein_glut", "Glutstein", "Ember Stone", "FIRE"),
    ("stein_flut", "Flutstein", "Tide Stone", "WATER"),
    ("stein_frost", "Froststein", "Frost Stone", "ICE"),
    ("stein_sturm", "Sturmstein", "Gale Stone", "WIND"),
    ("stein_fels", "Felsstein", "Crag Stone", "EARTH"),
    ("stein_hain", "Hainstein", "Grove Stone", "NATURE"),
    ("stein_blitz", "Blitzstein", "Bolt Stone", "THUNDER"),
    ("stein_glanz", "Glanzstein", "Gleam Stone", "LIGHT"),
    ("stein_schatten", "Schattenstein", "Umbra Stone", "SHADOW"),
    ("stein_geist", "Geisterstein", "Wraith Stone", "SPIRIT"),
    ("stein_rune", "Runenstein", "Rune Stone", "RUNE"),
    ("stein_chaos", "Chaosstein", "Chaos Stone", "CHAOS"),
    ("stein_erz", "Erzstein", "Ore Stone", "METAL"),
    ("stein_asen", "Asenstein", "Aesir Stone", "DIVINE"),
]

MATERIALS = [
    ("erz_eisen", "Eisenerz", "Iron Ore", 60, 1),
    ("erz_silber", "Silbererz", "Silver Ore", 180, 2),
    ("erz_gold", "Golderz", "Gold Ore", 420, 3),
    ("erz_sternenstahl", "Sternenstahl", "Star Steel", 1400, 4),
    ("holz_esche", "Eschenholz", "Ash Wood", 40, 1),
    ("holz_weltenast", "Weltenast", "World Bough", 1800, 4),
    ("leder_wolf", "Wolfsleder", "Wolf Leather", 80, 1),
    ("leder_drache", "Drachenleder", "Drake Leather", 900, 3),
    ("schuppe_wyrm", "Wyrmschuppe", "Wyrm Scale", 520, 3),
    ("feder_walkuere", "Walkürenfeder", "Valkyrie Feather", 1100, 4),
    ("kraut_heil", "Heilkraut", "Healing Herb", 30, 1),
    ("kraut_bann", "Bannkraut", "Warding Herb", 70, 1),
    ("kraut_nacht", "Nachtkraut", "Night Herb", 120, 2),
    ("pilz_nifl", "Niflpilz", "Nifl Fungus", 150, 2),
    ("harz_muspel", "Muspelharz", "Muspel Resin", 210, 2),
    ("kristall_frost", "Frostkristall", "Frost Crystal", 260, 2),
    ("kristall_glut", "Glutkristall", "Ember Crystal", 260, 2),
    ("kristall_sturm", "Sturmkristall", "Storm Crystal", 260, 2),
    ("kristall_seele", "Seelenkristall", "Soul Crystal", 640, 3),
    ("splitter_rune", "Runensplitter", "Rune Shard", 90, 1),
    ("scherbe_glyphe", "Glyphenscherbe", "Glyph Fragment", 240, 2),
    ("staub_stern", "Sternenstaub", "Stardust", 380, 3),
    ("asche_welt", "Weltasche", "World Ash", 720, 3),
    ("traene_vanen", "Vanenträne", "Vanir Tear", 1600, 4),
    ("horn_jotun", "Jötunhorn", "Jotun Horn", 980, 3),
    ("knochen_draug", "Draugknochen", "Draug Bone", 340, 2),
    ("faden_norne", "Nornenfaden", "Norn Thread", 2200, 5),
    ("blut_fenrir", "Wolfsblut", "Wolf Blood", 1300, 4),
    ("essenz_chaos", "Chaosessenz", "Chaos Essence", 2600, 5),
    ("gold_zwerg", "Zwergengold", "Dwarven Gold", 800, 3),
]

EQUIPMENT = [
    # (id, name_de, name_en, slot, price, tier, stats, extras)
    ("waffe_wanderstab", "Wanderstab", "Wanderer's Staff", "WEAPON", 400, 1, {"magic": 4}, {}),
    ("waffe_jagdspeer", "Jagdspeer", "Hunting Spear", "WEAPON", 700, 2, {"attack": 7}, {}),
    ("waffe_runenaxt", "Runenaxt", "Rune Axe", "WEAPON", 2200, 3, {"attack": 14, "luck": 4}, {}),
    ("waffe_seidrstab", "Seidrstab", "Seidr Staff", "WEAPON", 2400, 3, {"magic": 15}, {}),
    ("waffe_walkuerenschwert", "Walkürenschwert", "Valkyrie Sword", "WEAPON", 6800, 4,
     {"attack": 24, "speed": 6}, {"element": "DIVINE", "affinity": 8}),
    ("waffe_gluthammer", "Gluthammer", "Ember Hammer", "WEAPON", 6400, 4,
     {"attack": 26}, {"element": "FIRE", "affinity": 10}),
    ("ruestung_lederwams", "Lederwams", "Leather Jerkin", "ARMOR", 380, 1, {"defense": 5}, {}),
    ("ruestung_kettenhemd", "Kettenhemd", "Chain Shirt", "ARMOR", 900, 2, {"defense": 10, "hp": 8}, {}),
    ("ruestung_zwergenplatte", "Zwergenplatte", "Dwarven Plate", "ARMOR", 3200, 3,
     {"defense": 20, "hp": 14, "speed": -3}, {}),
    ("ruestung_schattenmantel", "Schattenmantel", "Shadow Cloak", "ARMOR", 3400, 3,
     {"resistance": 18, "speed": 6}, {"element": "SHADOW", "affinity": 6}),
    ("ruestung_asenharnisch", "Asenharnisch", "Aesir Harness", "ARMOR", 8200, 4,
     {"defense": 28, "resistance": 22, "hp": 20}, {}),
    ("helm_lederkappe", "Lederkappe", "Leather Cap", "HELMET", 260, 1, {"defense": 3}, {}),
    ("helm_eisenhelm", "Eisenhelm", "Iron Helm", "HELMET", 780, 2, {"defense": 8}, {}),
    ("helm_seherhelm", "Seherhelm", "Seer's Helm", "HELMET", 2600, 3,
     {"resistance": 12, "luck": 8}, {"capture": 5}),
    ("helm_drachenhelm", "Drachenhelm", "Drake Helm", "HELMET", 5600, 4,
     {"defense": 18, "attack": 8}, {}),
    ("relikt_seheraugen", "Seheraugen", "Seer's Eyes", "RELIC", 3000, 3, {"luck": 12},
     {"capture": 10}),
    ("relikt_wolfszahn", "Wolfszahn", "Wolf Fang", "RELIC", 1800, 2, {"attack": 9}, {}),
    ("relikt_nornenspindel", "Nornenspindel", "Norn's Spindle", "RELIC", 7400, 4,
     {"luck": 22, "magic": 10}, {"capture": 15}),
    ("relikt_schmiedemal", "Schmiedemal", "Forge Mark", "RELIC", 4200, 3, {"attack": 12, "defense": 12}, {}),
    ("relikt_stille_glocke", "Stille Glocke", "Silent Bell", "RELIC", 2800, 3, {},
     {"encounter": 0.5}),
    ("relikt_lockhorn", "Lockhorn", "Luring Horn", "RELIC", 2800, 3, {}, {"encounter": 1.6}),
    ("amulett_heilstein", "Heilstein-Amulett", "Healstone Amulet", "AMULET", 1200, 2, {"hp": 18}, {}),
    ("amulett_glutkern", "Glutkern-Amulett", "Emberheart Amulet", "AMULET", 2600, 3, {"attack": 10},
     {"element": "FIRE", "affinity": 8}),
    ("amulett_frostkern", "Frostkern-Amulett", "Frostheart Amulet", "AMULET", 2600, 3, {"resistance": 12},
     {"element": "ICE", "affinity": 8}),
    ("amulett_gluecksrune", "Glücksrunen-Amulett", "Fortune Rune Amulet", "AMULET", 5400, 4,
     {"luck": 25}, {"capture": 12}),
    ("amulett_weltenbaum", "Weltenbaum-Amulett", "World Tree Amulet", "AMULET", 9000, 5,
     {"hp": 30, "resistance": 18, "luck": 10}, {}),
]

KEY_ITEMS = [
    ("schluessel_runenwacht", "Siegel der Runenwacht", "Seal of the Rune Watch",
     "Beweist deinen Rang als Runenwächter und öffnet die Tore der Neun.",
     "Proves your rank as Rune Warden and opens the gates of the Nine."),
    ("schluessel_bifroest", "Bifröstscherbe", "Bifrost Shard",
     "Eine Scherbe der Regenbogenbrücke. Ermöglicht Weltensprünge.",
     "A shard of the rainbow bridge, allowing travel between worlds."),
    ("schluessel_helschluessel", "Helschlüssel", "Hel's Key",
     "Öffnet die Tore der Totenhalle.", "Opens the gates of the hall of the dead."),
    ("schluessel_schmiedebrief", "Schmiedebrief", "Forge Writ",
     "Gewährt Zutritt zu den Essen von Svartalfheim.",
     "Grants access to the forges of Svartalfheim."),
    ("schluessel_nornenkompass", "Nornenkompass", "Norn's Compass",
     "Weist den Weg zu verborgenen Orten.", "Points the way to hidden places."),
    ("werkzeug_flammenwiege", "Flammenwiege", "Flame Cradle",
     "Halbiert die Schritte, die ein Ei zum Schlüpfen benötigt.",
     "Halves the steps an egg needs to hatch."),
    ("werkzeug_gluecksrune", "Glücksrune", "Fortune Rune",
     "Erhöht die Chance, schillernde Monster zu finden, deutlich.",
     "Greatly raises the chance of finding shiny monsters."),
    ("werkzeug_sigill_ahnen", "Sigill der Ahnen", "Sigil of Lineage",
     "Bei der Zucht werden fünf statt drei Gene vererbt.",
     "Passes on five genes instead of three when breeding."),
    ("werkzeug_wesenszauber", "Wesenszauber", "Charm of Temper",
     "Das Ei erbt garantiert das Temperament der Mutter.",
     "The egg is guaranteed to inherit the mother's temperament."),
    ("werkzeug_bestiarium", "Bestiarium", "Bestiary",
     "Verzeichnet jedes gesehene und gefangene Wesen der Neun Welten.",
     "Records every creature of the Nine Worlds you have seen or caught."),
]


def build_items(strings: Strings) -> list[dict]:
    items: list[dict] = []

    for key, name_de, name_en, grade, multiplier, price, condition, bonus in ORB_GRADES:
        item_id = f"orb_{key}"
        items.append(_item(
            item_id, "RUNE_ORB", price, name_de, name_en,
            f"Eine Runenkugel der Güte {grade.title()}. Fangfaktor ×{multiplier}.",
            f"A rune orb of {grade.lower()} grade. Capture factor ×{multiplier}.",
            strings, in_battle=True, on_monster=False,
            orb={
                "grade": grade,
                "catchMultiplier": multiplier,
                "conditionalBonus": bonus,
                "condition": condition,
                "shakeAnimationKey": f"anim_orb_{key}",
            },
            tier=ORB_GRADES.index((key, name_de, name_en, grade, multiplier, price, condition, bonus)) + 1,
        ))

    for item_id, name_de, name_en, price, effects, desc_de, desc_en, tier in HEALING:
        items.append(_item(item_id, "HEALING", price, name_de, name_en, desc_de, desc_en,
                           strings, effects=effects, in_battle=True, tier=tier))

    for item_id, name_de, name_en, stat, stat_de, stat_en in VITAMINS:
        items.append(_item(
            item_id, "HEALING", 3800, name_de, name_en,
            f"Erhöht dauerhaft die Trainingswerte für {stat_de} und stärkt die Bindung.",
            f"Permanently raises {stat_en} training values and deepens the bond.",
            strings,
            effects=[{"type": "GrantTraining", "gains": {stat: 10}},
                     {"type": "GrantFriendship", "amount": 4}],
            tier=3,
        ))

    for item_id, name_de, name_en, stat in BATTLE_BOOSTERS:
        items.append(_item(
            item_id, "BATTLE", 900, name_de, name_en,
            "Erhöht im Kampf einen Wert um zwei Stufen.",
            "Raises one stat by two stages during battle.",
            strings, effects=[{"type": "BattleStatBoost", "stat": stat, "stages": 2}],
            in_battle=True, tier=2,
        ))

    for item_id, name_de, name_en, element in EVOLUTION_STONES:
        items.append(_item(
            item_id, "EVOLUTION", 3000, name_de, name_en,
            f"Ein Stein voll {ELEMENT_DE[element]}-Kraft. Löst bei bestimmten Monstern eine Entwicklung aus.",
            f"A stone brimming with {ELEMENT_EN[element].lower()} power. Triggers certain evolutions.",
            strings, effects=[{"type": "TriggerEvolution", "allowedSpeciesIds": []}], tier=3,
        ))

    for item_id, name_de, name_en, price, tier in MATERIALS:
        items.append(_item(
            item_id, "MATERIAL", price, name_de, name_en,
            "Ein Handwerksmaterial für Schmiede, Alchemie und Runenweberei.",
            "A crafting material for forges, alchemy and rune weaving.",
            strings, on_monster=False, tier=tier,
        ))

    for element in ELEMENTS:
        for tier, (prefix_de, prefix_en, bonus, price) in enumerate(
            [("Kleine", "Lesser", 6, 900), ("", "", 12, 2600), ("Große", "Greater", 20, 6400)], start=1
        ):
            item_id = f"rune_{'lesser_' if tier == 1 else 'greater_' if tier == 3 else ''}{element.lower()}"
            name_de = f"{prefix_de} {ELEMENT_DE[element]}rune".strip()
            name_en = f"{prefix_en} {ELEMENT_EN[element]} Rune".strip()
            items.append(_item(
                item_id, "RUNE", price, name_de, name_en,
                f"An ein Monster gebunden verstärkt sie {ELEMENT_DE[element]}-Attacken "
                f"und erhöht seine Werte.",
                f"Bound to a monster it empowers {ELEMENT_EN[element].lower()} moves and raises its stats.",
                strings,
                rune={
                    "element": element,
                    "statBonus": {"attack": bonus, "magic": bonus},
                    "bindCost": price // 4,
                    "tier": tier,
                },
                on_monster=True, consumed=False, tier=tier,
            ))

    items.append(_item(
        "rune_keen", "Rune des scharfen Blicks", "Rune of the Keen Eye", 4200, None, None,
        None, None, strings, tier=3,
    ) if False else _item(
        "rune_keen", "RUNE", 4200, "Rune des scharfen Blicks", "Rune of the Keen Eye",
        "Erhöht die Chance auf kritische Treffer deutlich.",
        "Greatly raises the critical-hit rate.",
        strings, rune={"element": None, "statBonus": {"luck": 15}, "bindCost": 1000, "tier": 3},
        consumed=False, tier=3,
    ))

    for (item_id, name_de, name_en, slot, price, tier, stats, extras) in EQUIPMENT:
        spec = {
            "slot": slot,
            "statBonus": stats,
            "levelRequirement": max(1, (tier - 1) * 12),
        }
        if "element" in extras:
            spec["elementAffinity"] = extras["element"]
            spec["affinityBonusPercent"] = extras.get("affinity", 0)
        if "capture" in extras:
            spec["captureBonusPercent"] = extras["capture"]
        if "encounter" in extras:
            spec["encounterRateModifier"] = extras["encounter"]
        bonus_text_de = ", ".join(f"+{v} {k}" for k, v in stats.items()) or "besondere Wirkung"
        items.append(_item(
            item_id, "EQUIPMENT", price, name_de, name_en,
            f"Ausrüstung für den Runenwächter ({bonus_text_de}).",
            f"Gear for the Rune Warden ({bonus_text_de}).",
            strings, equipment=spec, stack=1, on_monster=False, consumed=False, tier=tier,
        ))

    for item_id, name_de, name_en, desc_de, desc_en in KEY_ITEMS:
        items.append(_item(item_id, "KEY", 0, name_de, name_en, desc_de, desc_en,
                           strings, stack=1, on_monster=False, consumed=False, tier=5))

    # A single, story-locked guaranteed capture used at the end of chapter 9.
    items.append(_item(
        "orb_der_bindung", "Kugel der Bindung", "Orb of Binding", 0,
        "Ein einziger Wurf, dem sich nichts entzieht — nicht einmal ein Gott.",
        "A single throw nothing escapes — not even a god.",
        strings, effects=[{"type": "GuaranteedCapture"}], in_battle=True,
        on_monster=False, stack=1, tier=5,
    ) if False else _item(
        "orb_der_bindung", "RUNE_ORB", 0, "Kugel der Bindung", "Orb of Binding",
        "Ein einziger Wurf, dem sich nichts entzieht — nicht einmal ein Gott.",
        "A single throw nothing escapes — not even a god.",
        strings, effects=[{"type": "GuaranteedCapture"}], in_battle=True, on_monster=False,
        stack=1, tier=5,
        orb={"grade": "LEGENDARY", "catchMultiplier": 255.0, "conditionalBonus": 1.0,
             "condition": "NONE", "shakeAnimationKey": "anim_orb_binding"},
    ))

    seen = set()
    for item in items:
        if item["id"] in seen:
            raise ValueError(f"duplicate item id {item['id']}")
        seen.add(item["id"])
    return items


# ---------------------------------------------------------------------------
# Recipes
# ---------------------------------------------------------------------------

def build_recipes(strings: Strings, items: list[dict]) -> list[dict]:
    by_id = {item["id"]: item for item in items}
    recipes: list[dict] = []

    def add(recipe_id, name_de, name_en, station, ingredients, result, gold=0,
            amount=1, chance=1.0, tier=1, byproduct=None, byproduct_chance=0.0):
        if result not in by_id:
            raise ValueError(f"recipe {recipe_id} produces unknown item {result}")
        for ingredient in ingredients:
            if ingredient not in by_id:
                raise ValueError(f"recipe {recipe_id} needs unknown item {ingredient}")
        strings.add(f"rec_{recipe_id}_name", name_de, name_en)
        strings.add(
            f"rec_{recipe_id}_desc",
            "Ein Rezept der Neun Welten." if chance >= 1.0
            else f"Riskantes Rezept — Erfolgschance {int(chance * 100)} %.",
            "A recipe of the Nine Worlds." if chance >= 1.0
            else f"A risky recipe — {int(chance * 100)} % success chance.",
        )
        payload = {
            "id": f"rec_{recipe_id}",
            "nameKey": f"rec_{recipe_id}_name",
            "descriptionKey": f"rec_{recipe_id}_desc",
            "station": station,
            "ingredients": ingredients,
            "goldCost": gold,
            "resultItemId": result,
            "resultAmount": amount,
            "successChance": chance,
            "tier": tier,
        }
        if byproduct:
            payload["byproductItemId"] = byproduct
            payload["byproductChance"] = byproduct_chance
        recipes.append(payload)

    # --- alchemy ---------------------------------------------------------
    add("elixier_klein", "Kleines Elixier brauen", "Brew Minor Elixir", "ALCHEMY_TABLE",
        {"kraut_heil": 2}, "elixier_klein", gold=40)
    add("elixier", "Elixier brauen", "Brew Elixir", "ALCHEMY_TABLE",
        {"kraut_heil": 4, "kristall_frost": 1}, "elixier", gold=120, tier=2)
    add("elixier_gross", "Großes Elixier brauen", "Brew Greater Elixir", "ALCHEMY_TABLE",
        {"kraut_heil": 8, "kristall_seele": 1}, "elixier_gross", gold=300, tier=3)
    add("allheilkraut", "Allheilkraut mischen", "Mix Panacea Herb", "ALCHEMY_TABLE",
        {"kraut_bann": 3, "kraut_nacht": 2, "pilz_nifl": 1}, "allheilkraut", gold=260, tier=3)
    add("lebensfunke", "Lebensfunke binden", "Bind Life Spark", "ALCHEMY_TABLE",
        {"kristall_seele": 2, "staub_stern": 1}, "lebensfunke", gold=600, tier=3)
    add("lebensflamme", "Lebensflamme entfachen", "Kindle Life Flame", "ALCHEMY_TABLE",
        {"kristall_seele": 4, "traene_vanen": 1}, "lebensflamme", gold=1800, chance=0.85, tier=4)
    add("aether_klein", "Kleinen Äther destillieren", "Distil Minor Aether", "ALCHEMY_TABLE",
        {"splitter_rune": 3, "kraut_nacht": 1}, "aether_klein", gold=150, tier=2)
    add("aether_gross", "Großen Äther destillieren", "Distil Greater Aether", "ALCHEMY_TABLE",
        {"scherbe_glyphe": 3, "staub_stern": 1}, "aether_gross", gold=700, tier=3)
    for balm, herb in [("balsam_brand", "harz_muspel"), ("balsam_gift", "kraut_bann"),
                       ("bannwasser", "kraut_nacht"), ("nervenharz", "kraut_heil"),
                       ("tauwasser", "kristall_frost"), ("weckruf", "kraut_nacht"),
                       ("klarheitstee", "kraut_heil"), ("verband", "leder_wolf")]:
        add(balm, f"{by_id[balm]['id']} herstellen", f"Craft {balm}", "ALCHEMY_TABLE",
            {herb: 2}, balm, gold=60)

    # --- forge -----------------------------------------------------------
    add("waffe_jagdspeer", "Jagdspeer schmieden", "Forge Hunting Spear", "FORGE",
        {"erz_eisen": 4, "holz_esche": 2}, "waffe_jagdspeer", gold=200, tier=1)
    add("waffe_runenaxt", "Runenaxt schmieden", "Forge Rune Axe", "FORGE",
        {"erz_silber": 5, "splitter_rune": 4, "holz_esche": 3}, "waffe_runenaxt", gold=800, tier=2)
    add("waffe_gluthammer", "Gluthammer schmieden", "Forge Ember Hammer", "FORGE",
        {"erz_sternenstahl": 3, "kristall_glut": 4, "gold_zwerg": 2},
        "waffe_gluthammer", gold=2600, chance=0.80, tier=4, byproduct="asche_welt", byproduct_chance=0.4)
    add("waffe_walkuerenschwert", "Walkürenschwert schmieden", "Forge Valkyrie Sword", "FORGE",
        {"erz_sternenstahl": 4, "feder_walkuere": 3, "staub_stern": 2},
        "waffe_walkuerenschwert", gold=3200, chance=0.75, tier=4)
    add("ruestung_kettenhemd", "Kettenhemd schmieden", "Forge Chain Shirt", "FORGE",
        {"erz_eisen": 6, "leder_wolf": 3}, "ruestung_kettenhemd", gold=320)
    add("ruestung_zwergenplatte", "Zwergenplatte schmieden", "Forge Dwarven Plate", "FORGE",
        {"erz_silber": 8, "gold_zwerg": 3}, "ruestung_zwergenplatte", gold=1200, tier=3)
    add("ruestung_asenharnisch", "Asenharnisch schmieden", "Forge Aesir Harness", "FORGE",
        {"erz_sternenstahl": 6, "feder_walkuere": 4, "traene_vanen": 2},
        "ruestung_asenharnisch", gold=4200, chance=0.70, tier=5)
    add("helm_eisenhelm", "Eisenhelm schmieden", "Forge Iron Helm", "FORGE",
        {"erz_eisen": 3, "leder_wolf": 1}, "helm_eisenhelm", gold=180)
    add("helm_drachenhelm", "Drachenhelm schmieden", "Forge Drake Helm", "FORGE",
        {"schuppe_wyrm": 5, "leder_drache": 3, "erz_gold": 2}, "helm_drachenhelm",
        gold=2200, chance=0.85, tier=4)
    add("relikt_schmiedemal", "Schmiedemal prägen", "Strike Forge Mark", "FORGE",
        {"gold_zwerg": 4, "erz_gold": 3}, "relikt_schmiedemal", gold=1400, tier=3)

    # --- rune loom -------------------------------------------------------
    # Each element draws on the crystal that matches it best.
    element_crystal = {
        "ICE": "kristall_frost", "WATER": "kristall_frost",
        "FIRE": "kristall_glut", "EARTH": "kristall_glut",
        "WIND": "kristall_sturm", "THUNDER": "kristall_sturm",
        "SPIRIT": "kristall_seele", "SHADOW": "kristall_seele",
    }
    for element in ELEMENTS:
        crystal = element_crystal.get(element, "splitter_rune")
        add(f"rune_lesser_{element.lower()}", f"Kleine {ELEMENT_DE[element]}rune weben",
            f"Weave Lesser {ELEMENT_EN[element]} Rune", "RUNE_LOOM",
            {"splitter_rune": 3, crystal: 2} if crystal != "splitter_rune" else {"splitter_rune": 5},
            f"rune_lesser_{element.lower()}", gold=250, tier=1)
        add(f"rune_{element.lower()}", f"{ELEMENT_DE[element]}rune weben",
            f"Weave {ELEMENT_EN[element]} Rune", "RUNE_LOOM",
            {"scherbe_glyphe": 3, "splitter_rune": 6}, f"rune_{element.lower()}",
            gold=900, tier=2)
        add(f"rune_greater_{element.lower()}", f"Große {ELEMENT_DE[element]}rune weben",
            f"Weave Greater {ELEMENT_EN[element]} Rune", "RUNE_LOOM",
            {"scherbe_glyphe": 8, "staub_stern": 3, "faden_norne": 1},
            f"rune_greater_{element.lower()}", gold=2600, chance=0.88, tier=4)
    add("rune_keen", "Rune des scharfen Blicks weben", "Weave Rune of the Keen Eye", "RUNE_LOOM",
        {"scherbe_glyphe": 6, "feder_walkuere": 2}, "rune_keen", gold=1800, tier=3)

    # --- altar (legendary, risky) ----------------------------------------
    add("amulett_weltenbaum", "Weltenbaum-Amulett weihen", "Consecrate World Tree Amulet", "ALTAR",
        {"holz_weltenast": 2, "faden_norne": 2, "traene_vanen": 3},
        "amulett_weltenbaum", gold=6000, chance=0.60, tier=5,
        byproduct="asche_welt", byproduct_chance=0.5)
    add("relikt_nornenspindel", "Nornenspindel weihen", "Consecrate Norn's Spindle", "ALTAR",
        {"faden_norne": 3, "staub_stern": 4}, "relikt_nornenspindel", gold=4800, chance=0.65, tier=5)
    add("orb_legend", "Legendäre Runenkugel weihen", "Consecrate Legendary Rune Orb", "ALTAR",
        {"essenz_chaos": 2, "gold_zwerg": 5, "faden_norne": 1}, "orb_legend",
        gold=3000, amount=3, chance=0.70, tier=5)
    add("orb_divine", "Göttliche Runenkugel weihen", "Consecrate Divine Rune Orb", "ALTAR",
        {"staub_stern": 4, "gold_zwerg": 3}, "orb_divine", gold=1800, amount=5, chance=0.85, tier=4)

    return recipes


# ---------------------------------------------------------------------------
# Talent trees
# ---------------------------------------------------------------------------

TALENT_LAYOUT = {
    "attacker": [("Wilder Hieb", "Savage Blow", {"attack": 6}), ("Zähigkeit", "Toughness", {"hp": 14}),
                 ("Reißzahn", "Rending Fang", {"attack": 10}), ("Instinkt", "Instinct", {"speed": 8}),
                 ("Blutrausch", "Bloodlust", {"attack": 14}), ("Wolfsherz", "Wolf Heart", {"hp": 22}),
                 ("Meisterhieb", "Master Strike", {"attack": 20, "luck": 6})],
    "mage": [("Seidr-Studien", "Seidr Studies", {"magic": 6}), ("Klarer Geist", "Clear Mind", {"resistance": 8}),
             ("Runenfluss", "Rune Flow", {"magic": 10}), ("Zirkel", "Circle", {"hp": 14}),
             ("Arkane Tiefe", "Arcane Depth", {"magic": 14}), ("Bannkreis", "Ward Circle", {"resistance": 14}),
             ("Erzmagier", "Archmage", {"magic": 20, "speed": 6})],
    "bruiser": [("Kraftschub", "Power Surge", {"attack": 6}), ("Standfest", "Steadfast", {"defense": 8}),
                ("Rammbock", "Battering Ram", {"attack": 10}), ("Ausdauer", "Endurance", {"hp": 18}),
                ("Eisenfaust", "Iron Fist", {"attack": 14}), ("Kampfnarben", "Battle Scars", {"defense": 14}),
                ("Berserkerblut", "Berserker Blood", {"attack": 18, "hp": 18})],
    "tank": [("Dickhaut", "Thick Hide", {"defense": 8}), ("Vitalität", "Vitality", {"hp": 18}),
             ("Schildwall", "Shield Wall", {"defense": 12}), ("Trotz", "Defiance", {"resistance": 10}),
             ("Bergfest", "Mountainfast", {"defense": 16}), ("Lebenskraft", "Life Force", {"hp": 26}),
             ("Unbeugsam", "Unyielding", {"defense": 22, "resistance": 12})],
    "wall": [("Bannhaut", "Warded Skin", {"resistance": 8}), ("Ruhe", "Calm", {"hp": 16}),
             ("Geisterwehr", "Spirit Guard", {"resistance": 12}), ("Gelassenheit", "Serenity", {"defense": 10}),
             ("Seelenschild", "Soul Shield", {"resistance": 16}), ("Beharrlichkeit", "Persistence", {"hp": 24}),
             ("Unantastbar", "Untouchable", {"resistance": 22, "defense": 12})],
    "speedster": [("Leichtfuß", "Light Foot", {"speed": 8}), ("Wachsamkeit", "Alertness", {"luck": 8}),
                  ("Windschritt", "Wind Step", {"speed": 12}), ("Reflexe", "Reflexes", {"defense": 8}),
                  ("Sturmlauf", "Storm Run", {"speed": 16}), ("Glückssträhne", "Lucky Streak", {"luck": 14}),
                  ("Blitzgeschwind", "Lightning Swift", {"speed": 22, "attack": 10})],
    "support": [("Fürsorge", "Care", {"hp": 14}), ("Bündnis", "Alliance", {"resistance": 8}),
                ("Heilhand", "Healing Hand", {"magic": 8}), ("Wachtruf", "Watch Call", {"luck": 10}),
                ("Segensspruch", "Blessing", {"resistance": 14}), ("Treue", "Loyalty", {"hp": 22}),
                ("Schutzherr", "Protector", {"resistance": 18, "magic": 12})],
    "glass": [("Scharfsinn", "Acuity", {"magic": 8}), ("Wagemut", "Daring", {"attack": 8}),
              ("Präzision", "Precision", {"luck": 10}), ("Riskanter Stand", "Risky Stance", {"speed": 10}),
              ("Todesstoß", "Death Blow", {"attack": 16}), ("Urgewalt", "Primal Force", {"magic": 16}),
              ("Alles oder nichts", "All or Nothing", {"attack": 20, "magic": 20})],
    "balanced": [("Grundlage", "Foundation", {"hp": 12}), ("Ausgleich", "Balance", {"defense": 8}),
                 ("Vielseitig", "Versatile", {"attack": 8}), ("Anpassung", "Adaptation", {"magic": 8}),
                 ("Erfahrung", "Experience", {"speed": 10}), ("Meisterschaft", "Mastery", {"resistance": 12}),
                 ("Vollendung", "Perfection", {"attack": 12, "magic": 12, "hp": 16})],
}


def build_talents(strings: Strings) -> dict[str, list[dict]]:
    trees: dict[str, list[dict]] = {}
    for role, nodes in TALENT_LAYOUT.items():
        tree_id = f"talent_{role}"
        entries = []
        for index, (name_de, name_en, bonus) in enumerate(nodes):
            tier = index // 2 + 1
            node_id = f"{tree_id}_{index + 1}"
            strings.add(f"{node_id}_name", name_de, name_en)
            strings.add(
                f"{node_id}_desc",
                ", ".join(f"+{value} {key}" for key, value in bonus.items()),
                ", ".join(f"+{value} {key}" for key, value in bonus.items()),
            )
            entries.append({
                "id": node_id,
                "nameKey": f"{node_id}_name",
                "descriptionKey": f"{node_id}_desc",
                "tier": tier,
                "cost": tier,
                "requires": [f"{tree_id}_{index}"] if index > 0 else [],
                "statBonus": bonus,
            })
        trees[tree_id] = entries
    return trees


# ---------------------------------------------------------------------------
# Titles and achievements
# ---------------------------------------------------------------------------

TITLES = [
    ("titel_wanderer", "Wanderer", "Wanderer", "Beginne deine Reise.", "Begin your journey.", {}),
    ("titel_runenwaechter", "Runenwächter", "Rune Warden", "Erhalte das Siegel der Runenwacht.",
     "Receive the Seal of the Rune Watch.", {"experienceBonusPercent": 5}),
    ("titel_faenger", "Fänger", "Catcher", "Fange 50 Monster.", "Catch 50 monsters.",
     {"captureBonusPercent": 5}),
    ("titel_sammler", "Sammler der Neun", "Collector of the Nine", "Fange 150 Monster.",
     "Catch 150 monsters.", {"captureBonusPercent": 10}),
    ("titel_chronist", "Chronist", "Chronicler", "Vervollständige das Bestiarium.",
     "Complete the bestiary.", {"captureBonusPercent": 20, "experienceBonusPercent": 10}),
    ("titel_schimmerjaeger", "Schimmerjäger", "Shimmer Hunter", "Finde zehn schillernde Monster.",
     "Find ten shiny monsters.", {"captureBonusPercent": 15}),
    ("titel_haendler", "Händler", "Merchant", "Verkaufe Waren im Wert von 100 000 Gold.",
     "Sell goods worth 100,000 gold.", {"goldBonusPercent": 15}),
    ("titel_schmied", "Meisterschmied", "Master Smith", "Fertige 100 Gegenstände.",
     "Craft 100 items.", {"goldBonusPercent": 10}),
    ("titel_zuechter", "Züchter", "Breeder", "Lasse 50 Eier schlüpfen.", "Hatch 50 eggs.", {}),
    ("titel_bezwinger", "Bezwinger", "Vanquisher", "Besiege alle neun Weltenbosse.",
     "Defeat all nine world bosses.", {"experienceBonusPercent": 15}),
    ("titel_walkuerenfreund", "Walkürenfreund", "Friend of the Valkyries",
     "Erreiche höchstes Ansehen bei den Walküren.", "Reach the highest Valkyrie standing.", {}),
    ("titel_zwergenfreund", "Zwergenfreund", "Friend of the Dwarves",
     "Erreiche höchstes Ansehen bei den Zwergen.", "Reach the highest dwarven standing.",
     {"goldBonusPercent": 10}),
    ("titel_chaosbrecher", "Chaosbrecher", "Chaosbreaker", "Zerschlage den Chaoskult.",
     "Shatter the Cult of Chaos.", {"experienceBonusPercent": 10}),
    ("titel_endlos", "Ewiger Wanderer", "Eternal Wanderer", "Erreiche Stufe 50 im Endlosen Verlies.",
     "Reach floor 50 of the Endless Vault.", {"experienceBonusPercent": 20}),
    ("titel_turniersieger", "Turniersieger", "Tournament Champion", "Gewinne das Große Thing.",
     "Win the Great Thing.", {"goldBonusPercent": 20}),
    ("titel_ragnarok", "Der das Ende wandte", "Who Turned the End", "Verhindere Ragnarök.",
     "Prevent Ragnarok.", {"experienceBonusPercent": 25, "captureBonusPercent": 10}),
]

ACHIEVEMENTS = [
    ("ach_first_step", "Erster Schritt", "First Step", "STORY", 1, "Beginne dein Abenteuer.",
     "Begin your adventure."),
    ("ach_first_catch", "Erster Fang", "First Catch", "COLLECTION", 1, "Fange dein erstes Monster.",
     "Catch your first monster."),
    ("ach_catch_10", "Kleine Meute", "Small Pack", "COLLECTION", 10, "Fange 10 Monster.",
     "Catch 10 monsters."),
    ("ach_catch_50", "Wachsende Schar", "Growing Host", "COLLECTION", 50, "Fange 50 Monster.",
     "Catch 50 monsters."),
    ("ach_catch_150", "Große Schar", "Great Host", "COLLECTION", 150, "Fange 150 Monster.",
     "Catch 150 monsters."),
    ("ach_dex_complete", "Vollständiges Bestiarium", "Complete Bestiary", "COLLECTION", 250,
     "Trage jedes Wesen der Neun Welten ein.", "Record every creature of the Nine Worlds."),
    ("ach_shiny_first", "Ein Schimmern", "A Shimmer", "COLLECTION", 1,
     "Finde ein schillerndes Monster.", "Find a shiny monster."),
    ("ach_shiny_10", "Schimmerjagd", "Shimmer Hunt", "COLLECTION", 10,
     "Finde zehn schillernde Monster.", "Find ten shiny monsters."),
    ("ach_battles_100", "Hundert Kämpfe", "Hundred Battles", "BATTLE", 100, "Gewinne 100 Kämpfe.",
     "Win 100 battles."),
    ("ach_battles_1000", "Tausend Kämpfe", "Thousand Battles", "BATTLE", 1000, "Gewinne 1000 Kämpfe.",
     "Win 1000 battles."),
    ("ach_crit_500", "Schwachstelle", "Weak Point", "BATTLE", 500, "Lande 500 kritische Treffer.",
     "Land 500 critical hits."),
    ("ach_boss_all", "Bezwinger der Neun", "Vanquisher of the Nine", "BATTLE", 9,
     "Besiege alle neun Weltenbosse.", "Defeat all nine world bosses."),
    ("ach_eggs_10", "Brutpflege", "Nurture", "BREEDING", 10, "Lasse 10 Eier schlüpfen.",
     "Hatch 10 eggs."),
    ("ach_eggs_50", "Ahnenreihe", "Lineage", "BREEDING", 50, "Lasse 50 Eier schlüpfen.",
     "Hatch 50 eggs."),
    ("ach_hybrid", "Zwitterwesen", "Hybrid", "BREEDING", 1, "Erzeuge einen Hybriden.",
     "Produce a hybrid."),
    ("ach_perfect_genes", "Vollkommenes Blut", "Perfect Blood", "BREEDING", 1,
     "Züchte ein Monster mit sieben perfekten Genen.",
     "Breed a monster with seven perfect genes."),
    ("ach_worlds_all", "Neun Welten", "Nine Worlds", "EXPLORATION", 9, "Betrete alle neun Welten.",
     "Set foot in all nine worlds."),
    ("ach_secrets_9", "Geheimniskrämer", "Keeper of Secrets", "EXPLORATION", 9,
     "Entdecke neun Geheimnisse.", "Discover nine secrets."),
    ("ach_steps_100k", "Weiter Weg", "Long Road", "EXPLORATION", 100000, "Gehe 100 000 Schritte.",
     "Walk 100,000 steps."),
    ("ach_craft_100", "Handwerk", "Craftsmanship", "CRAFTING", 100, "Fertige 100 Gegenstände.",
     "Craft 100 items."),
    ("ach_craft_legendary", "Legendäres Werk", "Legendary Work", "CRAFTING", 1,
     "Fertige einen legendären Gegenstand.", "Craft a legendary item."),
    ("ach_level_100", "Vollendung", "Perfection", "MASTERY", 1,
     "Bringe ein Monster auf Stufe 100.", "Raise a monster to level 100."),
    ("ach_endless_50", "Tiefer und tiefer", "Deeper and Deeper", "MASTERY", 50,
     "Erreiche Stufe 50 im Endlosen Verlies.", "Reach floor 50 of the Endless Vault."),
    ("ach_tournament", "Sieger des Things", "Victor of the Thing", "MASTERY", 1,
     "Gewinne das Große Thing.", "Win the Great Thing."),
    ("ach_new_game_plus", "Ein zweites Mal", "A Second Time", "MASTERY", 1,
     "Beginne ein Neues Spiel Plus.", "Start a New Game Plus."),
    ("ach_all_endings", "Alle Fäden", "All Threads", "STORY", 4, "Sieh jedes Ende.",
     "See every ending."),
    ("ach_quests_50", "Helfer der Neun", "Helper of the Nine", "STORY", 50,
     "Schließe 50 Aufträge ab.", "Complete 50 quests."),
]


def build_titles(strings: Strings) -> list[dict]:
    result = []
    for title_id, name_de, name_en, cond_de, cond_en, bonuses in TITLES:
        strings.add(f"{title_id}_name", name_de, name_en)
        strings.add(f"{title_id}_desc", cond_de, cond_en)
        strings.add(f"{title_id}_cond", cond_de, cond_en)
        result.append({
            "id": title_id,
            "nameKey": f"{title_id}_name",
            "descriptionKey": f"{title_id}_desc",
            "unlockConditionKey": f"{title_id}_cond",
            "captureBonusPercent": bonuses.get("captureBonusPercent", 0),
            "goldBonusPercent": bonuses.get("goldBonusPercent", 0),
            "experienceBonusPercent": bonuses.get("experienceBonusPercent", 0),
            "rarityTier": 1 + len(bonuses),
        })
    return result


def build_achievements(strings: Strings) -> list[dict]:
    result = []
    for ach_id, name_de, name_en, category, target, desc_de, desc_en in ACHIEVEMENTS:
        strings.add(f"{ach_id}_name", name_de, name_en)
        strings.add(f"{ach_id}_desc", desc_de, desc_en)
        result.append({
            "id": ach_id,
            "nameKey": f"{ach_id}_name",
            "descriptionKey": f"{ach_id}_desc",
            "iconKey": f"ic_{ach_id}",
            "category": category,
            "progressTarget": target,
            "hidden": False,
            "points": 10 if target <= 10 else 25 if target <= 100 else 50,
        })
    return result
