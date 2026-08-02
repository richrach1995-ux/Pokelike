"""Generates the narrative content: factions, NPCs, trainer teams, dialogue
trees, quests, story chapters, cutscenes and endings.

The main story is authored explicitly (nine chapters plus prologue and
epilogue); side, faction, daily, legendary and secret quests are expanded from
per-region templates so every region carries the same amount of optional
content without 130 hand-written duplicates.
"""

from __future__ import annotations

from common import ELEMENT_DE, Strings, rng, slug
from world import REGIONS

# ---------------------------------------------------------------------------
# Factions
# ---------------------------------------------------------------------------

FACTIONS = [
    ("fac_runewardens", "Runenhüter", "Rune Wardens", 0xFFC79A4B, "midgard_heartlands",
     "Ein alter Orden, der die Runen bewahrt und die Grenzen der Neun Welten überwacht.",
     "An ancient order that keeps the runes and watches the borders of the Nine Worlds.",
     ["fac_chaoscult"],
     [("Fremder", "Stranger", -1000), ("Geduldet", "Tolerated", 0), ("Schildträger", "Shieldbearer", 200),
      ("Runenwächter", "Rune Warden", 500), ("Hüter des Ersten Zeichens", "Keeper of the First Sign", 850)]),
    ("fac_valkyries", "Walküren", "Valkyries", 0xFFE8D27A, "asgard_goldenheights",
     "Die Wählerinnen der Gefallenen. Sie messen dich an jedem Kampf, den du führst.",
     "The choosers of the slain. They measure you by every battle you fight.",
     ["fac_chaoscult"],
     [("Unbeachtet", "Unnoticed", -1000), ("Gesehen", "Seen", 0), ("Erprobt", "Proven", 200),
      ("Erwählt", "Chosen", 500), ("Schwesterngleich", "Sister-Equal", 850)]),
    ("fac_chaoscult", "Chaoskult", "Cult of Chaos", 0xFF8E2F4F, "helheim_deathmarches",
     "Sie glauben, das Ende sei überfällig — und wollen dabei helfen.",
     "They believe the end is overdue — and intend to help it along.",
     ["fac_runewardens", "fac_valkyries", "fac_aesir"],
     [("Feind", "Enemy", -1000), ("Unbekannt", "Unknown", 0), ("Geduldet", "Tolerated", 200),
      ("Eingeweihter", "Initiate", 500), ("Stimme des Bruchs", "Voice of the Breach", 850)]),
    ("fac_dwarves", "Zwerge von Svartalfheim", "Dwarves of Svartalfheim", 0xFF9AA3AD,
     "svartalfheim_deeps",
     "Handwerker ohne Gleichen. Sie zahlen in Gold und messen in Jahrhunderten.",
     "Craftsmen without equal. They pay in gold and measure in centuries.",
     [],
     [("Kunde", "Customer", -1000), ("Bekannt", "Known", 0), ("Geselle", "Journeyman", 200),
      ("Meistergast", "Master's Guest", 500), ("Blutsbruder", "Blood Brother", 850)]),
    ("fac_vanir", "Vanen", "Vanir", 0xFF4CA64C, "vanaheim_marches",
     "Die alten Fruchtbarkeitsmächte. Sie erinnern sich noch an den Krieg mit den Asen.",
     "The old powers of growth. They still remember the war with the Aesir.",
     ["fac_aesir"],
     [("Fremd", "Foreign", -1000), ("Gast", "Guest", 0), ("Freund der Felder", "Friend of Fields", 200),
      ("Saatbewahrer", "Seedkeeper", 500), ("Kind Vanaheims", "Child of Vanaheim", 850)]),
    ("fac_aesir", "Asen", "Aesir", 0xFF2F7FD6, "asgard_goldenheights",
     "Die herrschenden Götter. Sie sind weniger allmächtig, als sie wirken — und wissen das.",
     "The ruling gods. They are less all-powerful than they appear — and they know it.",
     ["fac_vanir", "fac_chaoscult"],
     [("Sterblich", "Mortal", -1000), ("Beachtet", "Noticed", 0), ("Gastfreund", "Guest-Friend", 200),
      ("Verbündeter", "Ally", 500), ("Thingberechtigt", "Thing-Entitled", 850)]),
]


def build_factions(strings: Strings) -> list[dict]:
    result = []
    for (faction_id, name_de, name_en, color, home, desc_de, desc_en, foes, ranks) in FACTIONS:
        strings.add(f"{faction_id}_name", name_de, name_en)
        strings.add(f"{faction_id}_desc", desc_de, desc_en)
        rank_entries = []
        for index, (rank_de, rank_en, minimum) in enumerate(ranks):
            rank_id = f"{faction_id}_rank{index}"
            strings.add(f"{rank_id}_name", rank_de, rank_en)
            rank_entries.append({
                "id": rank_id,
                "nameKey": f"{rank_id}_name",
                "minReputation": minimum,
                "discountPercent": index * 4,
                "unlocksQuestIds": [],
            })
        result.append({
            "id": faction_id,
            "nameKey": f"{faction_id}_name",
            "descriptionKey": f"{faction_id}_desc",
            "crestKey": f"crest_{faction_id}",
            "colorHex": color,
            "opposingFactionIds": foes,
            "ranks": rank_entries,
            "homeRegionId": home,
        })
    return result


# ---------------------------------------------------------------------------
# NPCs
# ---------------------------------------------------------------------------

GIVEN_NAMES = [
    "Astrid", "Bjorn", "Dagny", "Eirik", "Frida", "Gunnar", "Halla", "Ingvar", "Jorunn",
    "Kettil", "Liv", "Magnhild", "Njal", "Orm", "Ragna", "Sigrun", "Torsten", "Unn",
    "Vigdis", "Ylva", "Alfr", "Brynja", "Dagr", "Egil", "Frode", "Gudrun", "Hakon",
    "Ingrid", "Jarl", "Kari", "Leif", "Nanna", "Oddr", "Ragnvald", "Solveig", "Thora",
    "Ulf", "Vali", "Yrsa", "Aslaug", "Birk", "Dis", "Eldgrim", "Fenja", "Grimr",
    "Hedin", "Iva", "Kolbein", "Lodin", "Mardoll", "Njord", "Ottar", "Rannveig",
    "Skuli", "Tove", "Vebjorn", "Ylfa", "Arnbjorg", "Bersi", "Dagfinn",
]

ROLE_TEMPLATES = [
    ("MERCHANT", "Händler", "Merchant", "Handelt mit allem, was sich tragen lässt.",
     "Trades in anything that can be carried."),
    ("TRAINER", "Kämpfer", "Fighter", "Sucht ständig nach einem würdigen Gegner.",
     "Is forever looking for a worthy opponent."),
    ("QUEST_GIVER", "Auftraggeber", "Task-Giver", "Hat immer eine Bitte, die niemand sonst erfüllt.",
     "Always has a request nobody else will grant."),
    ("LORE_KEEPER", "Chronist", "Chronicler", "Kennt jede Geschichte der Region — auch die falschen.",
     "Knows every tale of the region, including the false ones."),
    ("HEALER", "Heilkundige", "Healer", "Pflegt Monster ebenso sorgfältig wie Menschen.",
     "Tends to monsters as carefully as to people."),
    ("SMITH", "Schmied", "Smith", "Arbeitet lieber mit Erz als mit Worten.",
     "Prefers working with ore over working with words."),
    ("BREEDER", "Züchter", "Breeder", "Betreut die Brutstätte und kennt jede Blutlinie.",
     "Runs the roost and knows every bloodline."),
    ("TUTOR", "Lehrmeister", "Tutor", "Lehrt Attacken, die kein Monster von allein lernt.",
     "Teaches moves no monster learns on its own."),
    ("GUARD", "Wache", "Guard", "Steht am Tor und lässt nur durch, wer einen Grund hat.",
     "Stands at the gate and only lets through those with a reason."),
    ("WANDERER", "Wanderer", "Wanderer", "Ist nie zweimal am selben Ort — außer hier.",
     "Is never twice in the same place — except here."),
]

# Story-critical characters, authored individually.
STORY_NPCS = [
    ("npc_seeress_vala", "Vala", "Vala", "Die Seherin von Wanderrast", "The Seeress of Wanderer's Rest",
     ["QUEST_GIVER", "LORE_KEEPER"], "loc_wanderrast", "midgard_heartlands", "fac_runewardens",
     "Sie hat dein Kommen gesehen, bevor du geboren wurdest — und trotzdem wirkt sie überrascht.",
     "She saw you coming before you were born — and still seems surprised."),
    ("npc_warden_hrafn", "Hrafn", "Hrafn", "Erster Runenwächter", "First Rune Warden",
     ["QUEST_GIVER", "TRAINER"], "loc_eschenfurt", "midgard_heartlands", "fac_runewardens",
     "Er trägt das Siegel seit vierzig Jahren und ist es müde. Deshalb sucht er dich.",
     "He has borne the seal for forty years and is tired of it. That is why he is looking for you."),
    ("npc_valkyrie_sigrdrifa", "Sigrdrifa", "Sigrdrifa", "Erwählerin der Gefallenen",
     "Chooser of the Slain", ["TRAINER", "QUEST_GIVER"], "loc_walstatt", "asgard_goldenheights",
     "fac_valkyries",
     "Sie entscheidet, wer in die Halle darf. Bei dir ist sie sich noch nicht sicher.",
     "She decides who enters the hall. About you, she has not decided yet."),
    ("npc_smith_dvalin", "Dvalin", "Dvalin", "Meister der Alten Esse", "Master of the Old Hearth",
     ["SMITH", "MERCHANT"], "loc_ambosshall", "svartalfheim_deeps", "fac_dwarves",
     "Er redet nicht viel, aber was er schmiedet, hält länger als der, der es trägt.",
     "He says little, but what he forges outlasts whoever carries it."),
    ("npc_vanir_gerdis", "Gerdis", "Gerdis", "Saatbewahrerin", "Seedkeeper",
     ["QUEST_GIVER", "HEALER"], "loc_saatheim", "vanaheim_marches", "fac_vanir",
     "Sie hat den Krieg zwischen Asen und Wanen noch erlebt und spricht ungern darüber.",
     "She lived through the war between Aesir and Vanir and does not like to speak of it."),
    ("npc_cult_grimnir", "Grimnir Bruchstimme", "Grimnir Breachvoice", "Erster Sprecher des Bruchs",
     "First Speaker of the Breach", ["ANTAGONIST", "TRAINER"], "loc_hallentor",
     "helheim_deathmarches", "fac_chaoscult",
     "Er will nicht die Welt zerstören. Er will sie nur endlich abschließen.",
     "He does not want to destroy the world. He only wants to finish it at last."),
    ("npc_cult_saga", "Saga", "Saga", "Zweite Stimme", "Second Voice",
     ["ANTAGONIST", "COMPANION"], "loc_grabrast", "helheim_deathmarches", "fac_chaoscult",
     "Sie war einmal Runenhüterin. Sie erklärt dir gern, warum sie nicht mehr eine ist.",
     "She was a Rune Warden once. She will gladly explain why she is not one any more."),
    ("npc_alf_lioss", "Lioss", "Lioss", "Sprecher der Lichtalfen", "Speaker of the Light Elves",
     ["QUEST_GIVER", "LORE_KEEPER"], "loc_glanzhall", "alfheim_prismwoods", None,
     "Er ist höflich bis zur Grausamkeit.", "He is polite to the point of cruelty."),
    ("npc_jotun_beira", "Beira", "Beira", "Alte des Grats", "Elder of the Ridge",
     ["QUEST_GIVER", "TRAINER"], "loc_trollmark", "jotunheim_ridges", None,
     "Sie ist dreimal so groß wie du und hält dich für ungewöhnlich mutig oder ungewöhnlich dumm.",
     "She is three times your size and thinks you unusually brave or unusually stupid."),
    ("npc_hel_modgud", "Modgud", "Modgud", "Wächterin der Brücke der Namen",
     "Warden of the Bridge of Names", ["GUARD", "LORE_KEEPER"], "loc_hallentor",
     "helheim_deathmarches", None,
     "Sie fragt jeden nach seinem Namen und schreibt ihn auf. Sie hat nie einen vergessen.",
     "She asks everyone their name and writes it down. She has never forgotten one."),
    ("npc_aesir_bragr", "Bragr", "Bragr", "Stimme des Things", "Voice of the Thing",
     ["DEITY", "QUEST_GIVER"], "loc_gladsheim_vorhof", "asgard_goldenheights", "fac_aesir",
     "Er spricht in Versen, wenn er nervös ist. Er ist meistens nervös.",
     "He speaks in verse when nervous. He is usually nervous."),
    ("npc_muspel_eldra", "Eldra", "Eldra", "Hüterin des Ewigen Brands", "Keeper of the Eternal Burning",
     ["LORE_KEEPER", "TRAINER"], "loc_aschenhort", "muspelheim_emberfields", None,
     "Sie hat noch nie gefroren und kann sich nicht vorstellen, wie das wäre.",
     "She has never once been cold and cannot imagine what that would be like."),
]


def build_npcs(strings: Strings, regions: list[dict]) -> tuple[list[dict], list[dict], list[dict]]:
    """Returns (npcs, trainer teams, dialogue trees)."""
    roll = rng("npcs")
    npcs: list[dict] = []
    teams: list[dict] = []
    trees: list[dict] = []
    name_index = 0

    for (npc_id, name_de, name_en, title_de, title_en, roles, location_id, region_id,
         faction, desc_de, desc_en) in STORY_NPCS:
        strings.add(f"{npc_id}_name", name_de, name_en)
        strings.add(f"{npc_id}_title", title_de, title_en)
        strings.add(f"{npc_id}_desc", desc_de, desc_en)
        tree = _dialogue_tree(npc_id, roles, strings, story=True)
        trees.append(tree)
        npcs.append({
            "id": npc_id,
            "nameKey": f"{npc_id}_name",
            "titleKey": f"{npc_id}_title",
            "descriptionKey": f"{npc_id}_desc",
            "roles": roles,
            "locationId": location_id,
            "regionId": region_id,
            "portraitKey": f"portrait_{npc_id}",
            "spriteKey": f"spr_{npc_id}",
            "factionId": faction,
            "dialogueTreeIds": [tree["id"]],
            "questIds": [],
            "isStoryCritical": True,
            "rematchable": "TRAINER" in roles,
            "loreKeys": [f"{npc_id}_desc"],
            "unlockRequirement": {"type": "None"},
        })
        if "TRAINER" in roles or "ANTAGONIST" in roles:
            teams.append(_trainer_team(npc_id, region_id, regions, roll, boss=True, strings=strings))
            npcs[-1]["trainerTeamId"] = teams[-1]["id"]

    # Ordinary population: eleven per region, spread over its locations.
    for region in regions:
        settlements = [loc for loc in region["locations"] if loc["type"] in ("TOWN", "SANCTUARY")]
        outdoors = [loc for loc in region["locations"] if loc["type"] in ("ROUTE", "LANDMARK")]
        placements = (settlements * 4 + outdoors)[:11]
        # Fixed slot order so every region ships three trainers, one healer,
        # one breeder and one tutor — the services the player relies on.
        slot_order = [
            "MERCHANT", "TRAINER", "QUEST_GIVER", "LORE_KEEPER", "HEALER", "TRAINER",
            "SMITH", "BREEDER", "TUTOR", "TRAINER", "WANDERER",
        ]
        templates_by_key = {entry[0]: entry for entry in ROLE_TEMPLATES}
        for slot, location in enumerate(placements):
            role_key, role_de, role_en, role_desc_de, role_desc_en = templates_by_key[
                slot_order[slot % len(slot_order)]
            ]
            given = GIVEN_NAMES[name_index % len(GIVEN_NAMES)]
            name_index += 1
            npc_id = f"npc_{region['id'].split('_')[0]}_{slug(given)}_{slot}"
            name_de = f"{given}"
            title_de = f"{role_de} von {_short_name(region, strings)}"
            strings.add(f"{npc_id}_name", name_de, name_de)
            strings.add(f"{npc_id}_title", title_de, f"{role_en}")
            strings.add(f"{npc_id}_desc", role_desc_de, role_desc_en)

            roles = [role_key]
            tree = _dialogue_tree(npc_id, roles, strings, story=False)
            trees.append(tree)
            entry = {
                "id": npc_id,
                "nameKey": f"{npc_id}_name",
                "titleKey": f"{npc_id}_title",
                "descriptionKey": f"{npc_id}_desc",
                "roles": roles,
                "locationId": location["id"],
                "regionId": region["id"],
                "portraitKey": f"portrait_generic_{role_key.lower()}",
                "spriteKey": f"spr_generic_{role_key.lower()}",
                "factionId": None,
                "dialogueTreeIds": [tree["id"]],
                "questIds": [],
                "appearsDuringPhases": ["NIGHT", "DUSK"] if role_key == "WANDERER" else [],
                "isStoryCritical": False,
                "rematchable": role_key == "TRAINER",
                "loreKeys": [],
                "unlockRequirement": {"type": "None"},
            }
            if role_key == "MERCHANT" and location.get("shopId"):
                entry["shopId"] = location["shopId"]
            if role_key == "TRAINER":
                team = _trainer_team(npc_id, region["id"], regions, roll, boss=False, strings=strings)
                teams.append(team)
                entry["trainerTeamId"] = team["id"]
            npcs.append(entry)

    return npcs, teams, trees


def _short_name(region: dict, strings: Strings) -> str:
    return strings.de.get(region["nameKey"], region["id"])


def _trainer_team(npc_id, region_id, regions, roll, boss: bool, strings: Strings) -> dict:
    from monsters import FAMILIES  # local import keeps the module graph acyclic

    region = next(r for r in regions if r["id"] == region_id)
    world = region["world"]
    candidates = [f for f in FAMILIES if f[4] == world]
    if not candidates:
        candidates = FAMILIES
    level = region["minLevel"] + (6 if boss else 2)
    size = 4 if boss else roll.randint(1, 3)
    members = []
    for index in range(size):
        family = roll.choice(candidates)
        stage = min(len(family[1]) - 1, 1 if not boss else len(family[1]) - 1)
        members.append({
            "speciesId": slug(family[1][stage]),
            "level": level + index,
            "geneQuality": 25 if boss else 12,
            "isShiny": False,
            "moveIds": [],
        })
    team_id = f"team_{npc_id}"
    strings.add(f"{team_id}_intro", "Zeig mir, was du gelernt hast!", "Show me what you have learned!")
    strings.add(f"{team_id}_defeat", "Ich habe dich unterschätzt.", "I underestimated you.")
    strings.add(f"{team_id}_victory", "Komm wieder, wenn du stärker bist.",
                "Come back when you are stronger.")
    return {
        "id": team_id,
        "npcId": npc_id,
        "aiProfile": "MASTERFUL" if boss else roll.choice(["INSTINCTIVE", "TACTICAL"]),
        "members": members,
        "rewardGold": (300 if boss else 90) * (region["minLevel"] // 5 + 1),
        "rewardItemIds": [],
        "rematchLevelBonus": 5,
        "introDialogueNodeId": f"{team_id}_intro",
        "defeatDialogueNodeId": f"{team_id}_defeat",
        "victoryDialogueNodeId": f"{team_id}_victory",
    }


def _dialogue_tree(npc_id: str, roles: list[str], strings: Strings, story: bool) -> dict:
    """Builds a small branching tree with day/night and quest-state variants."""
    tree_id = f"dlg_{npc_id}"
    nodes = []

    def node(node_id, text_de, text_en, choices=None, next_id=None, actions=None, conditions=None):
        strings.add(f"{node_id}_text", text_de, text_en)
        nodes.append({
            "id": node_id,
            "speakerNameKey": f"{npc_id}_name",
            "textKey": f"{node_id}_text",
            "portraitKey": f"portrait_{npc_id}" if story else None,
            "emotion": "neutral",
            "choices": choices or [],
            "nextNodeId": next_id,
            "actions": actions or [],
            "conditions": conditions or [],
        })

    def choice(choice_id, text_de, text_en, next_id, actions=None, conditions=None):
        strings.add(f"{choice_id}_text", text_de, text_en)
        return {
            "id": choice_id,
            "textKey": f"{choice_id}_text",
            "nextNodeId": next_id,
            "conditions": conditions or [],
            "actions": actions or [],
            "moralWeight": 0,
        }

    greeting_choices = [
        choice(f"{tree_id}_c_lore", "Erzähl mir von dieser Gegend.", "Tell me about this place.",
               f"{tree_id}_lore"),
        choice(f"{tree_id}_c_bye", "Ein andermal.", "Another time.", None,
               actions=[{"type": "EndDialogue"}]),
    ]
    if "MERCHANT" in roles:
        greeting_choices.insert(0, choice(
            f"{tree_id}_c_shop", "Zeig mir deine Waren.", "Show me your wares.", None,
            actions=[{"type": "OpenShop", "shopId": f"shop_{npc_id}"}],
        ))
    if "TRAINER" in roles or "ANTAGONIST" in roles:
        greeting_choices.insert(0, choice(
            f"{tree_id}_c_fight", "Lass uns kämpfen.", "Let us fight.", f"{tree_id}_fight",
        ))
    if "HEALER" in roles:
        greeting_choices.insert(0, choice(
            f"{tree_id}_c_heal", "Bitte heile mein Team.", "Please heal my team.", f"{tree_id}_healed",
            actions=[{"type": "HealParty", "full": True}],
        ))

    node(f"{tree_id}_greet_day",
         "Ein guter Tag zum Wandern. Was führt dich her?",
         "A good day for travelling. What brings you here?",
         choices=greeting_choices,
         conditions=[{"type": "TimeOfDay", "phase": "DAY"}])
    node(f"{tree_id}_greet_night",
         "Spät unterwegs? Die Grenzen sind nachts dünner, das solltest du wissen.",
         "Out late? The borders are thinner at night — you should know that.",
         choices=greeting_choices,
         conditions=[{"type": "TimeOfDay", "phase": "NIGHT"}])
    node(f"{tree_id}_greet",
         "Sei gegrüßt, Wanderer.", "Well met, traveller.",
         choices=greeting_choices)
    node(f"{tree_id}_lore",
         "Seit der Weltenbaum krankt, stimmt hier nichts mehr. Frag die Alten, wenn du es genau wissen willst.",
         "Since the World Tree sickened, nothing here is right. Ask the elders if you want the details.",
         next_id=None)
    if "TRAINER" in roles or "ANTAGONIST" in roles:
        node(f"{tree_id}_fight", "Dann zeig mir, was dein Bund taugt!",
             "Then show me what your bond is worth!",
             actions=[{"type": "StartBattle", "encounterId": f"team_{npc_id}"}])
    if "HEALER" in roles:
        node(f"{tree_id}_healed", "Deine Gefährten sind wieder bei Kräften.",
             "Your companions are restored.")

    return {
        "id": tree_id,
        "npcId": npc_id,
        "entryNodeId": f"{tree_id}_greet",
        "nodes": nodes,
        "priority": 10 if story else 0,
        "conditions": [],
    }


# ---------------------------------------------------------------------------
# Story chapters and main quests
# ---------------------------------------------------------------------------

CHAPTERS = [
    (0, "Der brennende Ast", "The Burning Bough", "midgard_heartlands",
     "Ein Ast des Weltenbaums fällt brennend vom Himmel und schlägt in den Hain bei Wanderrast. "
     "Vala, die Seherin, weiß sofort, was es bedeutet — und wen sie dafür braucht.",
     "A branch of the World Tree falls burning from the sky into the grove near Wanderer's Rest. "
     "Vala the Seeress knows at once what it means — and whom she needs for it."),
    (1, "Das Siegel der Runenwacht", "The Seal of the Rune Watch", "midgard_heartlands",
     "Hrafn übergibt dir ein Siegel, das er seit vierzig Jahren trägt. Er sagt, es sei eine Ehre. "
     "Sein Gesicht sagt etwas anderes.",
     "Hrafn hands you a seal he has borne for forty years. He says it is an honour. "
     "His face says otherwise."),
    (2, "Risse im Licht", "Cracks in the Light", "alfheim_prismwoods",
     "In Alfheim fällt das Licht falsch. Die Lichtalfen bemerken es als Letzte — oder geben es "
     "als Letzte zu.",
     "In Alfheim the light falls wrongly. The light elves notice last — or admit it last."),
    (3, "Was unter den Feldern wächst", "What Grows Beneath the Fields", "vanaheim_marches",
     "Vanaheims Ernte ist zu üppig, zu schnell, zu früh. Gerdis weiß, womit man Wachstum "
     "erkauft: mit dem Jahr danach.",
     "Vanaheim's harvest is too rich, too fast, too early. Gerdis knows what growth is bought "
     "with: the year after."),
    (4, "Der Grat der Riesen", "The Ridge of Giants", "jotunheim_ridges",
     "Die Riesen fliehen aus ihren eigenen Bergen. Etwas ist dort eingezogen, das größer ist "
     "als sie.",
     "The giants are fleeing their own mountains. Something has moved in that is larger than "
     "they are."),
    (5, "Die Alte Esse", "The Old Hearth", "svartalfheim_deeps",
     "Die Zwerge schmieden seit Wochen Ketten. Niemand hat sie beauftragt. Sie können nicht "
     "aufhören.",
     "The dwarves have been forging chains for weeks. Nobody commissioned them. They cannot stop."),
    (6, "Der Ewige Brand", "The Eternal Burning", "muspelheim_emberfields",
     "In Muspelheim brennt ein Feuer, das älter ist als die Zeit — und es breitet sich zum "
     "ersten Mal aus.",
     "In Muspelheim burns a fire older than time — and for the first time it is spreading."),
    (7, "Elf Quellen, ein Brunnen", "Eleven Springs, One Well", "niflheim_mistwastes",
     "Im Nebel findest du die zweite Stimme des Chaoskults. Sie war einmal wie du. Sie erklärt "
     "dir genau, warum sie es nicht mehr ist.",
     "In the mist you find the cult's second voice. She was once like you. She explains exactly "
     "why she is not any more."),
    (8, "Die Halle der Ungezählten", "The Hall of the Uncounted", "helheim_deathmarches",
     "Helheim ist voll. Zum ersten Mal seit dem Anbeginn ist kein Platz mehr. Und jemand hat "
     "das Tor geöffnet.",
     "Helheim is full. For the first time since the beginning there is no more room. And someone "
     "has opened the gate."),
    (9, "Thing der Neun", "Thing of the Nine", "asgard_goldenheights",
     "Die Götter tagen ein letztes Mal. Sie werden dich fragen, ob die Welt es wert ist. "
     "Und sie werden auf deine Antwort hören.",
     "The gods convene one final time. They will ask you whether the world is worth it. "
     "And they will listen to your answer."),
]


def build_story(strings: Strings, regions: list[dict], npcs: list[dict], species: list[dict]):
    """Returns (chapters, quests, cutscenes, endings)."""
    roll = rng("story")
    chapters = []
    quests: list[dict] = []
    cutscenes: list[dict] = []

    npcs_by_region: dict[str, list[dict]] = {}
    for npc in npcs:
        npcs_by_region.setdefault(npc["regionId"], []).append(npc)

    for (number, title_de, title_en, region_id, synopsis_de, synopsis_en) in CHAPTERS:
        strings.add(f"chapter_{number}_title", title_de, title_en)
        strings.add(f"chapter_{number}_synopsis", synopsis_de, synopsis_en)
        opening = _cutscene(f"cs_chapter_{number}_open", title_de, title_en,
                            synopsis_de, synopsis_en, strings)
        closing = _cutscene(f"cs_chapter_{number}_close", title_de, title_en,
                            "Ein Abschnitt schließt sich — und der nächste Riss öffnet sich.",
                            "One chapter closes — and the next tear opens.", strings)
        cutscenes.extend([opening, closing])

        region = next(r for r in regions if r["id"] == region_id)
        chapter_quests = _main_quests(number, region, npcs_by_region.get(region_id, []),
                                      species, strings, roll)
        quests.extend(chapter_quests)

        chapters.append({
            "number": number,
            "titleKey": f"chapter_{number}_title",
            "synopsisKey": f"chapter_{number}_synopsis",
            "regionId": region_id,
            "mainQuestIds": [quest["id"] for quest in chapter_quests],
            "openingCutsceneId": opening["id"],
            "closingCutsceneId": closing["id"],
            "unlocksRegionIds": [regions[min(number + 1, len(regions) - 1)]["id"]],
            "bossId": f"legend_{region['legendaryMonsterIds'][0]}",
            "musicKey": f"bgm_chapter_{number}",
        })

    quests.extend(_side_quests(regions, npcs_by_region, species, strings, roll))
    quests.extend(_faction_quests(regions, strings, roll))
    quests.extend(_daily_quests(strings))
    quests.extend(_legendary_quests(regions, strings))
    quests.extend(_secret_quests(regions, strings))

    endings = _endings(strings, cutscenes)
    return chapters, quests, cutscenes, endings


def _cutscene(cutscene_id, title_de, title_en, text_de, text_en, strings: Strings) -> dict:
    strings.add(f"{cutscene_id}_title", title_de, title_en)
    strings.add(f"{cutscene_id}_line1", text_de, text_en)
    strings.add(
        f"{cutscene_id}_line2",
        "Yggdrasil ächzt. Irgendwo reißt eine weitere Grenze.",
        "Yggdrasil groans. Somewhere another border tears.",
    )
    return {
        "id": cutscene_id,
        "titleKey": f"{cutscene_id}_title",
        "musicKey": "bgm_story",
        "skippable": True,
        "beats": [
            {"type": "ChangeMusic", "musicKey": "bgm_story", "fadeMs": 900},
            {"type": "Narration", "textKey": f"{cutscene_id}_line1", "durationMs": 4200},
            {"type": "ScreenEffect", "effect": "rune_flash", "durationMs": 700},
            {"type": "Narration", "textKey": f"{cutscene_id}_line2", "durationMs": 3400},
        ],
    }


def _quest(quest_id, name_de, name_en, summary_de, summary_en, category, objectives,
           rewards, strings: Strings, **extra) -> dict:
    strings.add(f"{quest_id}_name", name_de, name_en)
    strings.add(f"{quest_id}_summary", summary_de, summary_en)
    strings.add(f"{quest_id}_desc", summary_de, summary_en)
    payload = {
        "id": quest_id,
        "nameKey": f"{quest_id}_name",
        "summaryKey": f"{quest_id}_summary",
        "descriptionKey": f"{quest_id}_desc",
        "category": category,
        "objectives": objectives,
        "rewards": rewards,
        "prerequisite": {"type": "None"},
        "prerequisiteQuestIds": [],
        "repeatable": False,
        "isHidden": False,
        "grantsStoryFlags": [],
    }
    payload.update(extra)
    return payload


def _objective(objective_id, desc_de, desc_en, kind, target=None, count=1, strings=None) -> dict:
    strings.add(f"{objective_id}_desc", desc_de, desc_en)
    return {
        "id": objective_id,
        "descriptionKey": f"{objective_id}_desc",
        "type": kind,
        "targetId": target,
        "requiredCount": count,
        "optional": False,
    }


def _main_quests(chapter, region, region_npcs, species, strings, roll) -> list[dict]:
    quests = []
    region_id = region["id"]
    towns = [loc for loc in region["locations"] if loc["type"] == "TOWN"]
    dungeons = [loc for loc in region["locations"] if loc["type"] == "DUNGEON"]
    sanctuary = next(loc for loc in region["locations"] if loc["type"] == "SANCTUARY")
    quest_giver = region_npcs[0]["id"] if region_npcs else None
    local_species = [s for s in species if s["nativeWorld"] == region["world"]]

    steps = [
        ("ankunft", f"Ankunft in {strings.de[region['nameKey']]}",
         f"Arrival in {strings.en[region['nameKey']]}",
         "Erreiche die Region und sprich mit den Ansässigen.",
         "Reach the region and speak with the locals.",
         [_objective(f"q_main_{chapter}_a_o1", "Erreiche die Region.", "Reach the region.",
                     "REACH_LOCATION", towns[0]["id"], 1, strings),
          _objective(f"q_main_{chapter}_a_o2", "Sprich mit einem Bewohner.",
                     "Speak with a local.", "TALK_TO_NPC", quest_giver, 1, strings)]),
        ("spur", "Die Spur des Risses", "The Trail of the Tear",
         "Untersuche, was die Grenze der Welt hier aufgerissen hat.",
         "Investigate what tore the world's border here.",
         [_objective(f"q_main_{chapter}_b_o1", "Besiege fünf wilde Wesen der Region.",
                     "Defeat five wild creatures of the region.", "WIN_BATTLES", None, 5, strings),
          _objective(f"q_main_{chapter}_b_o2", "Fange ein Wesen dieser Region.",
                     "Capture a creature of this region.", "CAPTURE_SPECIES",
                     local_species[0]["id"] if local_species else None, 1, strings)]),
        ("verlies", f"Hinab in {strings.de[dungeons[0]['nameKey']]}",
         f"Down into {strings.en[dungeons[0]['nameKey']]}",
         "Steige in das Verlies hinab und finde die Quelle der Störung.",
         "Descend into the dungeon and find the source of the disturbance.",
         [_objective(f"q_main_{chapter}_c_o1", "Durchquere das Verlies.",
                     "Clear the dungeon.", "COMPLETE_DUNGEON", dungeons[0]["id"], 1, strings),
          _objective(f"q_main_{chapter}_c_o2", "Besiege den Wächter.",
                     "Defeat the guardian.", "DEFEAT_BOSS", dungeons[0]["bossId"], 1, strings)]),
        ("rune", "Die Rune der Region", "The Rune of the Region",
         "Finde die Rune, die diese Welt zusammenhält, und binde sie.",
         "Find the rune that holds this world together and bind it.",
         [_objective(f"q_main_{chapter}_d_o1", "Erreiche das Heiligtum.",
                     "Reach the sanctuary.", "REACH_LOCATION", sanctuary["id"], 1, strings),
          _objective(f"q_main_{chapter}_d_o2", "Binde die Rune.",
                     "Bind the rune.", "BIND_RUNE", None, 1, strings)]),
    ]

    previous = None
    for index, (slug_key, name_de, name_en, summary_de, summary_en, objectives) in enumerate(steps):
        quest_id = f"q_main_{chapter}_{slug_key}"
        rewards = [
            {"type": "Gold", "amount": 500 * (chapter + 1)},
            {"type": "Experience", "amount": 800 * (chapter + 1)},
        ]
        if index == len(steps) - 1:
            rewards.append({"type": "StoryFlag", "flag": f"chapter_{chapter}_complete"})
            rewards.append({"type": "Reputation", "factionId": "fac_runewardens", "amount": 60})
        quests.append(_quest(
            quest_id, name_de, name_en, summary_de, summary_en, "MAIN", objectives, rewards,
            strings,
            chapter=chapter,
            regionId=region_id,
            giverNpcId=quest_giver,
            recommendedLevel=region["minLevel"],
            prerequisite={"type": "Chapter", "chapter": chapter},
            prerequisiteQuestIds=[previous] if previous else [],
            grantsStoryFlags=[f"{quest_id}_done"],
            nextQuestId=None,
        ))
        if previous:
            quests[-2]["nextQuestId"] = quest_id
        previous = quest_id
    return quests


SIDE_TEMPLATES = [
    ("jagd", "Ungebetene Gäste", "Unwanted Guests",
     "Etwas streift zu nah an den Höfen. Vertreibe es.",
     "Something is prowling too near the farms. Drive it off.",
     "DEFEAT_SPECIES", 6),
    ("sammeln", "Vorräte für den Winter", "Stores for Winter",
     "Sammle Material, bevor der Frost kommt.",
     "Gather material before the frost arrives.",
     "COLLECT_ITEM", 8),
    ("botengang", "Ein Wort zwischen Nachbarn", "A Word Between Neighbours",
     "Überbringe eine Nachricht — und höre dir beide Seiten an.",
     "Deliver a message — and listen to both sides.",
     "TALK_TO_NPC", 1),
    ("fang", "Ein Wesen für die Chronik", "A Creature for the Chronicle",
     "Der Chronist braucht ein lebendes Belegstück.",
     "The chronicler needs a living specimen.",
     "CAPTURE_SPECIES", 1),
    ("pruefung", "Prüfung des Bundes", "Trial of the Bond",
     "Beweise, dass dein Team zusammenhält.",
     "Prove that your team holds together.",
     "WIN_BATTLES", 10),
]


def _side_quests(regions, npcs_by_region, species, strings, roll) -> list[dict]:
    quests = []
    for region in regions:
        region_npcs = npcs_by_region.get(region["id"], [])
        local = [s for s in species if s["nativeWorld"] == region["world"]]
        for index, (key, name_de, name_en, summary_de, summary_en, kind, count) in enumerate(
            SIDE_TEMPLATES
        ):
            quest_id = f"q_side_{region['id']}_{key}"
            target = None
            if kind in ("DEFEAT_SPECIES", "CAPTURE_SPECIES") and local:
                target = roll.choice(local)["id"]
            elif kind == "COLLECT_ITEM":
                target = "erz_eisen"
            elif kind == "TALK_TO_NPC" and region_npcs:
                target = roll.choice(region_npcs)["id"]
            objectives = [_objective(f"{quest_id}_o1", summary_de, summary_en, kind, target,
                                     count, strings)]
            quests.append(_quest(
                quest_id, name_de, name_en, summary_de, summary_en, "SIDE", objectives,
                [{"type": "Gold", "amount": 220 * (region["minLevel"] // 6 + 1)},
                 {"type": "Experience", "amount": 300 * (region["minLevel"] // 6 + 1)}],
                strings,
                regionId=region["id"],
                giverNpcId=region_npcs[index % len(region_npcs)]["id"] if region_npcs else None,
                recommendedLevel=region["minLevel"] + 2,
                prerequisite={"type": "None"},
            ))
    return quests


def _faction_quests(regions, strings, roll) -> list[dict]:
    quests = []
    templates = [
        ("aufnahme", "Aufnahmeprüfung", "Trial of Admission", "WIN_BATTLES", 8, 40),
        ("dienst", "Ein Dienst für den Bund", "A Service for the Order", "COLLECT_ITEM", 5, 70),
        ("treue", "Beweis der Treue", "Proof of Loyalty", "DEFEAT_BOSS", 1, 140),
    ]
    for (faction_id, name_de, *_rest) in FACTIONS:
        for index, (key, quest_de, quest_en, kind, count, reputation) in enumerate(templates):
            quest_id = f"q_faction_{faction_id}_{key}"
            summary_de = f"{name_de} erwarten von dir einen Beweis."
            summary_en = "The order expects proof from you."
            objectives = [_objective(f"{quest_id}_o1", summary_de, summary_en, kind,
                                     None if kind != "COLLECT_ITEM" else "splitter_rune",
                                     count, strings)]
            quests.append(_quest(
                quest_id, f"{quest_de}: {name_de}", quest_en, summary_de, summary_en,
                "FACTION", objectives,
                [{"type": "Reputation", "factionId": faction_id, "amount": reputation},
                 {"type": "Gold", "amount": 400 * (index + 1)}],
                strings,
                factionId=faction_id,
                factionReputationDelta=reputation,
                prerequisite={"type": "Chapter", "chapter": index + 1},
            ))
    return quests


def _daily_quests(strings) -> list[dict]:
    templates = [
        ("jagd", "Tagesjagd", "Daily Hunt", "WIN_BATTLES", 5),
        ("fang", "Tagesfang", "Daily Catch", "CAPTURE_SPECIES", 2),
        ("ernte", "Tagesernte", "Daily Harvest", "COLLECT_ITEM", 6),
        ("schmiede", "Tageswerk", "Daily Work", "CRAFT_ITEM", 2),
        ("brut", "Tagesbrut", "Daily Brood", "HATCH_EGG", 1),
        ("verlies", "Tagesabstieg", "Daily Descent", "COMPLETE_DUNGEON", 1),
        ("botengang", "Tagesbotengang", "Daily Errand", "TALK_TO_NPC", 3),
        ("wanderung", "Tageswanderung", "Daily Walk", "REACH_LOCATION", 3),
        ("entwicklung", "Tagesentwicklung", "Daily Evolution", "EVOLVE_MONSTER", 1),
        ("rune", "Tagesrune", "Daily Rune", "BIND_RUNE", 1),
    ]
    quests = []
    for key, name_de, name_en, kind, count in templates:
        quest_id = f"q_daily_{key}"
        summary_de = "Ein Auftrag, der sich jeden Tag erneuert."
        summary_en = "A task that renews itself every day."
        quests.append(_quest(
            quest_id, name_de, name_en, summary_de, summary_en, "DAILY",
            [_objective(f"{quest_id}_o1", summary_de, summary_en, kind, None, count, strings)],
            [{"type": "Gold", "amount": 900}, {"type": "Experience", "amount": 1200}],
            strings, repeatable=True,
        ))
    return quests


def _legendary_quests(regions, strings) -> list[dict]:
    quests = []
    for region in regions:
        legend = region["legendaryMonsterIds"][0]
        quest_id = f"q_legend_{legend}"
        sanctuary = next(loc for loc in region["locations"] if loc["type"] == "SANCTUARY")
        summary_de = "Ein legendäres Wesen erwacht. Es wird dich prüfen, bevor es dich anhört."
        summary_en = "A legendary being stirs. It will test you before it listens."
        quests.append(_quest(
            quest_id, "Die Prüfung des Wächters", "The Guardian's Trial",
            summary_de, summary_en, "LEGENDARY",
            [_objective(f"{quest_id}_o1", "Erreiche das Heiligtum.", "Reach the sanctuary.",
                        "REACH_LOCATION", sanctuary["id"], 1, strings),
             _objective(f"{quest_id}_o2", "Überstehe zehn Runden.", "Survive ten turns.",
                        "SURVIVE_TURNS", "10", 1, strings),
             _objective(f"{quest_id}_o3", "Besiege oder fange den Wächter.",
                        "Defeat or capture the guardian.", "DEFEAT_BOSS", f"legend_{legend}", 1,
                        strings)],
            [{"type": "Experience", "amount": 12000},
             {"type": "UnlockTitle", "titleId": "titel_bezwinger"}],
            strings,
            regionId=region["id"],
            recommendedLevel=region["maxLevel"],
            prerequisite={"type": "Chapter", "chapter": max(1, region["minLevel"] // 8)},
        ))
    return quests


def _secret_quests(regions, strings) -> list[dict]:
    quests = []
    for region in regions:
        secret_location = next(loc for loc in region["locations"] if loc["type"] == "SECRET")
        quest_id = f"q_secret_{region['id']}"
        summary_de = "Der Nornenkompass zeigt auf einen Ort, der auf keiner Karte steht."
        summary_en = "The Norn's Compass points at a place no map shows."
        quests.append(_quest(
            quest_id, "Was auf keiner Karte steht", "What No Map Shows",
            summary_de, summary_en, "SECRET",
            [_objective(f"{quest_id}_o1", "Finde den verborgenen Ort.", "Find the hidden place.",
                        "DISCOVER_SECRET", f"secret_{region['id']}", 1, strings)],
            [{"type": "Gold", "amount": 5000},
             {"type": "Items", "itemIds": {"staub_stern": 3}}],
            strings,
            regionId=region["id"],
            isHidden=True,
            prerequisite={"type": "KeyItem", "itemId": "schluessel_nornenkompass"},
        ))
    return quests


ENDINGS = [
    ("ending_renewal", "Der neue Ast", "The New Bough",
     "Du pflanzt den Samen, den Ardvin dir gab. Yggdrasil stirbt trotzdem — aber nicht "
     "allein, und nicht ohne Nachkommen. Die Neun Welten werden kleiner, ärmer und "
     "unendlich viel jünger.",
     "You plant the seed Ardvin gave you. Yggdrasil dies anyway — but not alone, and not "
     "without issue. The Nine Worlds become smaller, poorer and infinitely younger.",
     ["chapter_9_complete"], [], 20, None, True, 100),
    ("ending_binding", "Die Neunte Fessel", "The Ninth Binding",
     "Du bindest den Bruch mit allen neun Runen. Die Welten bleiben, wie sie waren — "
     "einschließlich allem, was an ihnen falsch war. Grimnir lacht, als man ihn abführt.",
     "You bind the breach with all nine runes. The worlds remain as they were — including "
     "everything that was wrong with them. Grimnir laughs as they lead him away.",
     ["chapter_9_complete"], [], None, None, False, 80),
    ("ending_breach", "Die Stimme des Bruchs", "The Voice of the Breach",
     "Du gibst Grimnir recht. Das Ende kommt schnell, ordentlich und ohne Groll. "
     "Was danach kommt, sieht dir niemand mehr zu.",
     "You agree with Grimnir. The end comes swiftly, tidily and without rancour. "
     "What comes after, nobody watches you do.",
     ["chapter_9_complete"], [], None, -30, False, 90),
    ("ending_warden", "Der letzte Runenwächter", "The Last Rune Warden",
     "Du nimmst Hrafns Platz ein und hältst die Grenzen, so lange du kannst. Es ist keine "
     "Lösung. Es ist ein Versprechen — und du hältst es vierzig Jahre.",
     "You take Hrafn's place and hold the borders as long as you can. It is no solution. "
     "It is a promise — and you keep it for forty years.",
     ["chapter_9_complete"], [], None, None, False, 10),
]


def _endings(strings: Strings, cutscenes: list[dict]) -> list[dict]:
    result = []
    for (ending_id, title_de, title_en, text_de, text_en, required, forbidden,
         min_moral, max_moral, needs_runes, priority) in ENDINGS:
        cutscene = _cutscene(f"cs_{ending_id}", title_de, title_en, text_de, text_en, strings)
        cutscenes.append(cutscene)
        strings.add(f"{ending_id}_title", title_de, title_en)
        strings.add(f"{ending_id}_desc", text_de, text_en)
        payload = {
            "id": ending_id,
            "titleKey": f"{ending_id}_title",
            "descriptionKey": f"{ending_id}_desc",
            "cutsceneId": cutscene["id"],
            "requiredFlags": required,
            "forbiddenFlags": forbidden,
            "requiresAllRunes": needs_runes,
            "priority": priority,
            "minFactionStanding": {},
        }
        if min_moral is not None:
            payload["minMoralScore"] = min_moral
        if max_moral is not None:
            payload["maxMoralScore"] = max_moral
        result.append(payload)
    return result
