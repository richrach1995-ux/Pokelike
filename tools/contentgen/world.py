"""Generates ``regions.json``, ``encounters.json``, ``shops.json`` and
``chests.json``.

Each of the Nine Worlds is a graph of locations — towns, routes, dungeons,
landmarks, a sanctuary and one secret — connected by travel links.  Encounter
tables are assembled from the species whose native world matches the region,
filtered by rarity so that the level curve of the main story lines up with the
monsters the player meets.
"""

from __future__ import annotations

from common import Strings, rng, slug

# (world, region_id, name_de, name_en, level_range, elements, towns, routes,
#  dungeons, landmark, sanctuary, secret, weather weights, boss species)
REGIONS = [
    ("MIDGARD", "midgard_heartlands", "Herzland von Midgard", "Heartlands of Midgard",
     (2, 14), ["NATURE", "EARTH", "WATER"],
     [("Wanderrast", "Wanderer's Rest"), ("Eschenfurt", "Ashford"), ("Saltvik", "Saltwick")],
     [("Ährenweg", "Grainway"), ("Nebelmoor", "Mistmoor"), ("Steinpfad", "Stone Path"),
      ("Küstenweg", "Coast Road"), ("Rabenschlucht", "Raven Gorge")],
     [("Grabhügel von Alt-Eik", "Barrows of Old Eik"), ("Verlassene Mühle", "Abandoned Mill")],
     ("Der Gespaltene Stein", "The Cloven Stone"),
     ("Hain der ersten Rune", "Grove of the First Rune"),
     ("Versunkene Kammer", "Sunken Chamber"),
     {"CLEAR": 50, "RAIN": 25, "FOG": 15, "THUNDERSTORM": 10},
     "jorwyrm"),

    ("ALFHEIM", "alfheim_prismwoods", "Prismenwälder", "Prism Woods",
     (12, 24), ["LIGHT", "NATURE", "WIND"],
     [("Glanzhall", "Gleamhall"), ("Tauhof", "Dewcourt")],
     [("Silberpfad", "Silver Path"), ("Dämmerlichtung", "Dusk Clearing"),
      ("Prismensteig", "Prism Trail"), ("Lerchenwiese", "Lark Meadow")],
     [("Spiegelgrotte", "Mirror Grotto"), ("Turm der Sphären", "Tower of Spheres")],
     ("Stein des ersten Morgens", "Stone of the First Morning"),
     ("Lichtkreis", "Circle of Light"),
     ("Hinter dem Regenbogen", "Behind the Rainbow"),
     {"CLEAR": 55, "AURORA": 25, "RAIN": 10, "FOG": 10},
     "dagrun"),

    ("VANAHEIM", "vanaheim_marches", "Blühende Marken", "Blooming Marches",
     (20, 32), ["NATURE", "WATER", "RUNE"],
     [("Saatheim", "Seedholm"), ("Metquell", "Meadspring")],
     [("Ährenmark", "Grain March"), ("Nebelaue", "Mist Meadow"), ("Seidrpfad", "Seidr Path"),
      ("Schilfufer", "Reed Shore")],
     [("Wurzelgrotte", "Root Grotto"), ("Schilfhöhle", "Reed Cave")],
     ("Goldbrunnen", "Golden Well"),
     ("Erntestein", "Harvest Stone"),
     ("Verborgener Weiher", "Hidden Pool"),
     {"CLEAR": 45, "RAIN": 30, "FOG": 15, "THUNDERSTORM": 10},
     "ardvin"),

    ("JOTUNHEIM", "jotunheim_ridges", "Frostkämme", "Frost Ridges",
     (30, 42), ["ICE", "EARTH", "WIND"],
     [("Trollmark", "Trollmark"), ("Riesenfurt", "Giant's Ford")],
     [("Windgrat", "Wind Ridge"), ("Gletscherzunge", "Glacier Tongue"),
      ("Steinbruch", "Quarry"), ("Hochpass", "High Pass")],
     [("Bergfausts Höhle", "Bergfaust's Cave"), ("Eiskathedrale", "Ice Cathedral")],
     ("Der Sitzende Riese", "The Seated Giant"),
     ("Urhornstein", "Aurochs Stone"),
     ("Spalte im Grat", "Cleft in the Ridge"),
     {"SNOW": 40, "CLEAR": 25, "FOG": 20, "SANDSTORM": 15},
     "bergrimm"),

    ("SVARTALFHEIM", "svartalfheim_deeps", "Tiefenstollen", "Deep Delvings",
     (38, 50), ["METAL", "EARTH", "RUNE"],
     [("Ambosshall", "Anvilhall"), ("Goldgrund", "Goldground")],
     [("Erzstollen", "Ore Gallery"), ("Schachtweg", "Shaft Road"), ("Aderngang", "Vein Passage")],
     [("Alte Esse", "Old Hearth"), ("Runenschmiede", "Rune Forge")],
     ("Herz des Berges", "Heart of the Mountain"),
     ("Erster Amboss", "First Anvil"),
     ("Vergessene Kammer", "Forgotten Chamber"),
     {"CLEAR": 60, "SANDSTORM": 25, "HEATWAVE": 15},
     "dvalgrim"),

    ("MUSPELHEIM", "muspelheim_emberfields", "Glutfelder", "Ember Fields",
     (46, 58), ["FIRE", "CHAOS", "METAL"],
     [("Aschenhort", "Ashhold"), ("Schlackenhort", "Slaghold")],
     [("Lavastrom", "Lava Flow"), ("Rauchsteig", "Smoke Trail"), ("Basaltweg", "Basalt Road")],
     [("Surtwacht-Feste", "Surtwatch Keep"), ("Glutschlund", "Ember Maw")],
     ("Der Ewige Brand", "The Eternal Burning"),
     ("Funkenaltar", "Spark Altar"),
     ("Kammer unter der Asche", "Chamber Beneath the Ash"),
     {"HEATWAVE": 55, "CLEAR": 30, "SANDSTORM": 15},
     "surtwacht"),

    ("NIFLHEIM", "niflheim_mistwastes", "Nebelöden", "Mist Wastes",
     (54, 66), ["ICE", "SPIRIT", "WATER"],
     [("Reifhall", "Rimehall"), ("Brunnenrast", "Wellrest")],
     [("Nebelweg", "Mist Road"), ("Firnfeld", "Firn Field"), ("Kältegrat", "Cold Ridge")],
     [("Brunnentiefe", "Well Deep"), ("Nagwyrm-Nest", "Nagwyrm Nest")],
     ("Elf Quellen", "Eleven Springs"),
     ("Stiller Ring", "Silent Ring"),
     ("Unter dem Eis", "Beneath the Ice"),
     {"SNOW": 45, "FOG": 35, "CLEAR": 20},
     "hrimthar"),

    ("HELHEIM", "helheim_deathmarches", "Totenmarken", "Death Marches",
     (62, 74), ["SHADOW", "SPIRIT", "CHAOS"],
     [("Grabrast", "Graverest"), ("Hallentor", "Hallgate")],
     [("Modersteig", "Mould Trail"), ("Schattenmoor", "Shadow Moor"), ("Rabenfeld", "Raven Field")],
     [("Halle der Ungezählten", "Hall of the Uncounted"), ("Garmwacht-Tor", "Garmwatch Gate")],
     ("Brücke der Namen", "Bridge of Names"),
     ("Ruhestein", "Rest Stone"),
     ("Zweite Halle", "Second Hall"),
     {"FOG": 50, "CLEAR": 25, "RAIN": 25},
     "helvarn"),

    ("ASGARD", "asgard_goldenheights", "Goldene Höhen", "Golden Heights",
     (70, 85), ["DIVINE", "LIGHT", "THUNDER"],
     [("Gladsheim-Vorhof", "Gladsheim Forecourt"), ("Walstatt", "Choosing Field")],
     [("Bifröststeig", "Bifrost Stair"), ("Wolkenmark", "Cloud March"), ("Speerfeld", "Spear Field")],
     [("Hallen der Gefallenen", "Halls of the Fallen"), ("Sturmwacht-Turm", "Stormwatch Tower")],
     ("Bifröstbogen", "Bifrost Arch"),
     ("Thing der Asen", "Thing of the Aesir"),
     ("Odrmunds Auge", "Odrmund's Eye"),
     {"AURORA": 40, "CLEAR": 35, "THUNDERSTORM": 25},
     "odrmund"),
]


def build_world(strings: Strings, species: list[dict], items: list[dict]):
    roll = rng("world")
    regions = []
    encounter_tables = []
    shops = []
    chests = []

    by_world: dict[str, list[dict]] = {}
    for entry in species:
        by_world.setdefault(entry["nativeWorld"], []).append(entry)

    shop_pools = _shop_pools(items)

    for order, (world, region_id, name_de, name_en, level_range, elements, towns, routes,
                dungeons, landmark, sanctuary, secret, weather, boss) in enumerate(REGIONS):
        strings.add(f"{region_id}_name", name_de, name_en)
        strings.add(
            f"{region_id}_desc",
            f"Empfohlene Stufe {level_range[0]}–{level_range[1]}.",
            f"Recommended level {level_range[0]}–{level_range[1]}.",
        )
        strings.add(f"{region_id}_lore", *_region_lore(world))

        locations = []
        links = []
        pool = [s for s in by_world.get(world, []) if not s["rarity"] in ("MYTHIC", "DIVINE")]

        # --- towns --------------------------------------------------------
        for index, (town_de, town_en) in enumerate(towns):
            location_id = f"loc_{slug(town_de)}"
            strings.add(f"{location_id}_name", town_de, town_en)
            strings.add(
                f"{location_id}_desc",
                f"Eine Siedlung in {name_de}. Hier findest du Rast, Handel und Neuigkeiten.",
                f"A settlement in {name_en}. Rest, trade and news can be found here.",
            )
            shop_id = f"shop_{slug(town_de)}"
            shops.append(_shop(shop_id, town_de, town_en, shop_pools, order, strings))
            locations.append({
                "id": location_id, "regionId": region_id,
                "nameKey": f"{location_id}_name", "descriptionKey": f"{location_id}_desc",
                "type": "TOWN", "mapX": 0.18 + index * 0.30, "mapY": 0.22 + index * 0.12,
                "backgroundKey": f"bg_{region_id}_town",
                "npcIds": [], "shopId": shop_id,
                "healerAvailable": True,
                "roostAvailable": index == 0,
                "forgeAvailable": index == len(towns) - 1,
                "storageAvailable": True,
                "fastTravel": True,
                "unlockRequirement": {"type": "None"},
            })

        # --- routes -------------------------------------------------------
        for index, (route_de, route_en) in enumerate(routes):
            location_id = f"loc_{slug(route_de)}"
            table_id = f"enc_{slug(route_de)}"
            strings.add(f"{location_id}_name", route_de, route_en)
            strings.add(
                f"{location_id}_desc",
                "Wilde Wesen streifen hier umher. Halte deine Runenkugeln bereit.",
                "Wild creatures roam here. Keep your rune orbs ready.",
            )
            encounter_tables.append(
                _encounter_table(table_id, pool, level_range, roll, dungeon=False)
            )
            locations.append({
                "id": location_id, "regionId": region_id,
                "nameKey": f"{location_id}_name", "descriptionKey": f"{location_id}_desc",
                "type": "ROUTE", "mapX": 0.12 + index * 0.18, "mapY": 0.55 + (index % 2) * 0.16,
                "backgroundKey": f"bg_{region_id}_route",
                "encounterTableId": table_id, "encounterRate": 0.11 + index * 0.006,
                "npcIds": [], "chestIds": [], "unlockRequirement": {"type": "None"},
            })
            chest_id = f"chest_{slug(route_de)}"
            chests.append({
                "id": chest_id, "locationId": location_id,
                "itemIds": [roll.choice(shop_pools["consumable"])],
                "goldAmount": 120 * (order + 1),
                "isHidden": False, "respawnsDaily": False,
            })
            locations[-1]["chestIds"] = [chest_id]

        # --- dungeons -----------------------------------------------------
        for index, (dungeon_de, dungeon_en) in enumerate(dungeons):
            location_id = f"loc_{slug(dungeon_de)}"
            table_id = f"enc_{slug(dungeon_de)}"
            strings.add(f"{location_id}_name", dungeon_de, dungeon_en)
            strings.add(
                f"{location_id}_desc",
                "Ein Verlies mit mehreren Ebenen. Je tiefer, desto gefährlicher.",
                "A multi-floor dungeon. The deeper you go, the more dangerous it gets.",
            )
            encounter_tables.append(
                _encounter_table(table_id, pool, level_range, roll, dungeon=True)
            )
            chest_id = f"chest_{slug(dungeon_de)}"
            chests.append({
                "id": chest_id, "locationId": location_id,
                "itemIds": [roll.choice(shop_pools["treasure"]), roll.choice(shop_pools["material"])],
                "goldAmount": 400 * (order + 1),
                "isHidden": True, "respawnsDaily": False,
            })
            locations.append({
                "id": location_id, "regionId": region_id,
                "nameKey": f"{location_id}_name", "descriptionKey": f"{location_id}_desc",
                "type": "DUNGEON", "mapX": 0.62 + index * 0.20, "mapY": 0.62 - index * 0.14,
                "backgroundKey": f"bg_{region_id}_dungeon",
                "encounterTableId": table_id, "encounterRate": 0.16 + index * 0.02,
                "floors": 4 + index * 2,
                "chestIds": [chest_id],
                "bossId": f"boss_{region_id}_{index + 1}",
                "unlockRequirement": {"type": "Chapter", "chapter": order + 1},
            })

        # --- landmark, sanctuary, secret ----------------------------------
        for kind, (place_de, place_en), location_type in (
            ("landmark", landmark, "LANDMARK"),
            ("sanctuary", sanctuary, "SANCTUARY"),
            ("secret", secret, "SECRET"),
        ):
            location_id = f"loc_{slug(place_de)}"
            strings.add(f"{location_id}_name", place_de, place_en)
            strings.add(f"{location_id}_desc", *_place_desc(kind))
            entry = {
                "id": location_id, "regionId": region_id,
                "nameKey": f"{location_id}_name", "descriptionKey": f"{location_id}_desc",
                "type": location_type,
                "mapX": 0.80 if kind == "landmark" else 0.50 if kind == "sanctuary" else 0.30,
                "mapY": 0.20 if kind == "landmark" else 0.82 if kind == "sanctuary" else 0.92,
                "backgroundKey": f"bg_{region_id}_{kind}",
                "unlockRequirement": (
                    {"type": "None"} if kind == "landmark"
                    else {"type": "Chapter", "chapter": order + 1} if kind == "sanctuary"
                    else {"type": "KeyItem", "itemId": "schluessel_nornenkompass"}
                ),
            }
            if kind == "sanctuary":
                entry["bossId"] = f"legend_{boss}"
                entry["healerAvailable"] = True
            if kind == "secret":
                entry["secretHint"] = f"secret_{region_id}_hint"
                strings.add(
                    f"secret_{region_id}_hint",
                    "Der Nornenkompass zittert hier merkwürdig.",
                    "The Norn's Compass trembles oddly here.",
                )
                chest_id = f"chest_{slug(place_de)}"
                chests.append({
                    "id": chest_id, "locationId": location_id,
                    "itemIds": [roll.choice(shop_pools["treasure"])],
                    "goldAmount": 2000 * (order + 1),
                    "isHidden": True, "respawnsDaily": False,
                })
                entry["chestIds"] = [chest_id]
            locations.append(entry)

        # --- links: town -> routes -> dungeon -> next town ----------------
        ordered_ids = [entry["id"] for entry in locations]
        for previous, following in zip(ordered_ids, ordered_ids[1:]):
            links.append({
                "fromLocationId": previous, "toLocationId": following,
                "travelSteps": 24 + roll.randint(0, 20),
                "requirement": {"type": "None"}, "bidirectional": True,
            })
        # A shortcut from the first town straight to the sanctuary.
        links.append({
            "fromLocationId": ordered_ids[0],
            "toLocationId": f"loc_{slug(sanctuary[0])}",
            "travelSteps": 60,
            "requirement": {"type": "Chapter", "chapter": order + 1},
            "bidirectional": True,
        })

        regions.append({
            "id": region_id,
            "world": world,
            "nameKey": f"{region_id}_name",
            "descriptionKey": f"{region_id}_desc",
            "loreKey": f"{region_id}_lore",
            "minLevel": level_range[0],
            "maxLevel": level_range[1],
            "dominantElements": elements,
            "musicKey": f"bgm_{region_id}",
            "nightMusicKey": f"bgm_{region_id}_night",
            "ambienceKey": f"amb_{region_id}",
            "mapAssetKey": f"map_{region_id}",
            "paletteKey": f"pal_{region_id}",
            "weatherProfile": {
                "weights": weather,
                "changeIntervalMinutes": 12,
                "seasonalOverrides": {},
            },
            "locations": locations,
            "links": links,
            "unlockRequirement": ({"type": "None"} if order == 0
                                  else {"type": "Chapter", "chapter": order + 1}),
            "legendaryMonsterIds": [boss],
            "collectibleIds": [f"collectible_{region_id}_{i}" for i in range(1, 4)],
            "secretIds": [f"secret_{region_id}"],
        })

    return regions, encounter_tables, shops, chests


def _region_lore(world: str) -> tuple[str, str]:
    lore = {
        "MIDGARD": ("Die Welt der Menschen — und der erste Ort, an dem die Grenzen rissen.",
                    "The world of humankind — and the first place where the borders tore."),
        "ASGARD": ("Die goldenen Höhen der Asen, wo jedes Wort Gewicht hat.",
                   "The golden heights of the Aesir, where every word carries weight."),
        "VANAHEIM": ("Land der Wanen, in dem Wachstum eine Form von Magie ist.",
                     "Land of the Vanir, where growth itself is a kind of magic."),
        "ALFHEIM": ("Wälder aus Licht, in denen Schatten als Höflichkeit gelten.",
                    "Forests of light, where shadows count as a courtesy."),
        "JOTUNHEIM": ("Kämme aus Eis und Stein, gemacht für Wesen, die größer sind als du.",
                      "Ridges of ice and stone, made for beings larger than you."),
        "MUSPELHEIM": ("Felder aus Glut, die schon brannten, bevor es Zeit gab.",
                       "Fields of embers that burned before there was time."),
        "NIFLHEIM": ("Nebel über elf Quellen. Man findet hier nur, was man mitbringt.",
                     "Mist over eleven springs. You find only what you brought with you."),
        "HELHEIM": ("Die Marken der Toten, geordnet, still und erstaunlich höflich.",
                    "The marches of the dead: orderly, quiet and surprisingly polite."),
        "SVARTALFHEIM": ("Stollen ohne Ende, in denen seit Anbeginn gehämmert wird.",
                         "Endless galleries where hammering has never once stopped."),
    }
    return lore[world]


def _place_desc(kind: str) -> tuple[str, str]:
    return {
        "landmark": ("Ein Wahrzeichen der Region. Von hier sieht man weit.",
                     "A landmark of the region. You can see far from here."),
        "sanctuary": ("Ein geweihter Ort. Hier ruht ein legendäres Wesen.",
                      "A consecrated place. A legendary being rests here."),
        "secret": ("Ein Ort, den es auf keiner Karte gibt.",
                   "A place that appears on no map."),
    }[kind]


def _encounter_table(table_id: str, pool: list[dict], level_range, roll, dungeon: bool) -> dict:
    """Assembles a weighted encounter table for one location."""
    entries = []
    commons = [s for s in pool if s["rarity"] in ("COMMON", "UNCOMMON") and s["stage"] == 1]
    mids = [s for s in pool if s["stage"] == 2]
    rares = [s for s in pool if s["rarity"] in ("RARE", "EPIC")]
    low, high = level_range

    def add(entry_species, weight, min_level, max_level, **extra):
        payload = {
            "speciesId": entry_species["id"],
            "minLevel": max(2, min_level),
            "maxLevel": max(3, max_level),
            "weight": weight,
            "floorRange": {"first": 1, "last": 99},
        }
        payload.update(extra)
        entries.append(payload)

    for entry_species in roll.sample(commons, min(4, len(commons))):
        add(entry_species, 260, low, low + 3)
    for entry_species in roll.sample(mids, min(3, len(mids))):
        add(entry_species, 110, low + 3, high - 2)
    for entry_species in roll.sample(rares, min(2, len(rares))):
        add(entry_species, 32, high - 3, high, isRareSpawn=True)

    # Night-only and weather-only spawns give the location its own character.
    night_pool = [s for s in pool if s["primaryElement"] in ("SHADOW", "SPIRIT", "CHAOS")]
    if night_pool:
        add(roll.choice(night_pool), 150, low + 1, high - 1, dayPhases=["NIGHT", "DUSK"])
    weather_pool = [s for s in pool if s["primaryElement"] in ("WATER", "ICE", "THUNDER")]
    if weather_pool:
        add(roll.choice(weather_pool), 140, low + 1, high - 1,
            weathers=["RAIN", "THUNDERSTORM", "SNOW"])
    if dungeon:
        deep = [s for s in pool if s["stage"] >= 2]
        if deep:
            entry = roll.choice(deep)
            payload = {
                "speciesId": entry["id"], "minLevel": high - 2, "maxLevel": high + 2,
                "weight": 70, "floorRange": {"first": 3, "last": 99},
            }
            entries.append(payload)

    return {"id": table_id, "entries": entries}


def _shop_pools(items: list[dict]) -> dict[str, list[str]]:
    return {
        "orb": [i["id"] for i in items if i["category"] == "RUNE_ORB" and i["price"] > 0],
        "consumable": [i["id"] for i in items if i["category"] == "HEALING" and i["price"] > 0],
        "battle": [i["id"] for i in items if i["category"] == "BATTLE"],
        "material": [i["id"] for i in items if i["category"] == "MATERIAL"],
        "equipment": [i["id"] for i in items if i["category"] == "EQUIPMENT"],
        "rune": [i["id"] for i in items if i["category"] == "RUNE"],
        "treasure": [i["id"] for i in items if i["category"] in ("EVOLUTION", "RUNE")],
    }


def _shop(shop_id: str, town_de: str, town_en: str, pools, tier: int, strings: Strings) -> dict:
    strings.add(f"{shop_id}_name", f"Handelsposten {town_de}", f"{town_en} Trading Post")
    stock = (
        pools["orb"][: min(len(pools["orb"]), 2 + tier)]
        + pools["consumable"][: 6 + tier]
        + pools["battle"]
        + pools["equipment"][: 3 + tier * 2]
        + pools["rune"][: 4 + tier * 3]
    )
    return {
        "id": shop_id,
        "nameKey": f"{shop_id}_name",
        "ownerNpcId": None,
        "itemIds": list(dict.fromkeys(stock)),
        "markup": round(1.0 + tier * 0.03, 2),
        "unlockRequirement": {"type": "None"},
        "restockDaily": True,
        "factionId": None,
        "factionDiscountPercent": 0,
    }
