"""Generates ``moves.json`` — the complete attack catalogue.

Every element receives a full ladder of moves (basic → ultimate, physical and
magical), plus its own status, debuff, drain, multi-hit, priority, buff and
shield variants.  On top of that come universal support moves, capture moves
and the signature moves of the legendary guardians.

Names are German compounds assembled from an authored per-element prefix pool
and a per-archetype suffix pool, which produces varied, on-theme names
(``Glutklaue``, ``Firnschleier``, ``Galdrbollwerk``) without hand-writing 300
strings.
"""

from __future__ import annotations

from common import ELEMENT_DE, ELEMENT_EN, ELEMENTS, Strings, rng, slug

# Combining forms, ready to be concatenated with a suffix.
PREFIXES = {
    "FIRE": ["Glut", "Flammen", "Brand", "Aschen", "Funken", "Lohen", "Muspel"],
    "WATER": ["Flut", "Wogen", "Strom", "Gischt", "Quell", "Tiefen", "Brandungs"],
    "ICE": ["Frost", "Firn", "Eis", "Reif", "Schnee", "Gletscher", "Hrim"],
    "WIND": ["Sturm", "Böen", "Wirbel", "Orkan", "Luft", "Wolken", "Zugwind"],
    "EARTH": ["Fels", "Stein", "Erd", "Geröll", "Gebirgs", "Grund", "Schollen"],
    "NATURE": ["Ranken", "Wurzel", "Blatt", "Hain", "Dorn", "Moos", "Samen"],
    "THUNDER": ["Blitz", "Donner", "Gewitter", "Ladungs", "Zorn", "Stromschlag", "Wetter"],
    "LIGHT": ["Glanz", "Strahlen", "Licht", "Schimmer", "Aurora", "Klar", "Morgen"],
    "SHADOW": ["Schatten", "Dunkel", "Finsternis", "Nacht", "Zwielicht", "Umbra", "Raben"],
    "SPIRIT": ["Geister", "Seelen", "Wispern", "Schemen", "Nebel", "Toten", "Ahnen"],
    "RUNE": ["Runen", "Glyphen", "Zeichen", "Siegel", "Stab", "Galdr", "Ritz"],
    "CHAOS": ["Chaos", "Zerr", "Riss", "Wahn", "Unheil", "Verderb", "Bruch"],
    "METAL": ["Eisen", "Stahl", "Erz", "Klingen", "Schmiede", "Panzer", "Nagel"],
    "DIVINE": ["Asen", "Segens", "Himmels", "Weih", "Odem", "Gnaden", "Wanen"],
}

PREFIXES_EN = {
    "FIRE": ["Ember", "Flame", "Brand", "Ash", "Spark", "Blaze", "Muspel"],
    "WATER": ["Flood", "Wave", "Current", "Spray", "Spring", "Deep", "Surf"],
    "ICE": ["Frost", "Firn", "Ice", "Rime", "Snow", "Glacier", "Hrim"],
    "WIND": ["Storm", "Gust", "Whirl", "Gale", "Air", "Cloud", "Draft"],
    "EARTH": ["Crag", "Stone", "Earth", "Scree", "Mountain", "Ground", "Clod"],
    "NATURE": ["Vine", "Root", "Leaf", "Grove", "Thorn", "Moss", "Seed"],
    "THUNDER": ["Bolt", "Thunder", "Tempest", "Charge", "Wrath", "Shock", "Weather"],
    "LIGHT": ["Gleam", "Ray", "Light", "Shimmer", "Aurora", "Clear", "Dawn"],
    "SHADOW": ["Shadow", "Dark", "Gloom", "Night", "Dusk", "Umbra", "Raven"],
    "SPIRIT": ["Ghost", "Soul", "Whisper", "Wraith", "Mist", "Dead", "Ancestor"],
    "RUNE": ["Rune", "Glyph", "Sign", "Seal", "Stave", "Galdr", "Carve"],
    "CHAOS": ["Chaos", "Warp", "Rift", "Madness", "Bane", "Ruin", "Break"],
    "METAL": ["Iron", "Steel", "Ore", "Blade", "Forge", "Plate", "Nail"],
    "DIVINE": ["Aesir", "Blessing", "Heaven", "Hallow", "Breath", "Grace", "Vanir"],
}

# Which status each element naturally inflicts.
ELEMENT_STATUS = {
    "FIRE": "BURN", "WATER": "CONFUSION", "ICE": "FREEZE", "WIND": "CONFUSION",
    "EARTH": "PARALYSIS", "NATURE": "POISON", "THUNDER": "PARALYSIS",
    "LIGHT": "SLEEP", "SHADOW": "SLEEP", "SPIRIT": "CURSE", "RUNE": "PARALYSIS",
    "CHAOS": "CONFUSION", "METAL": "BLEED", "DIVINE": "CURSE",
}

ELEMENT_WEATHER = {
    "FIRE": "HEATWAVE", "WATER": "RAIN", "ICE": "SNOW", "THUNDER": "THUNDERSTORM",
    "EARTH": "SANDSTORM", "SHADOW": "FOG", "SPIRIT": "FOG", "LIGHT": "AURORA",
    "DIVINE": "AURORA", "RUNE": "AURORA",
}

STATUS_DE = {
    "BURN": "Verbrennung", "POISON": "Vergiftung", "SLEEP": "Schlaf",
    "FREEZE": "Einfrieren", "CONFUSION": "Verwirrung", "BLEED": "Blutung",
    "CURSE": "Fluch", "PARALYSIS": "Lähmung",
}
STATUS_EN = {
    "BURN": "burn", "POISON": "poison", "SLEEP": "sleep", "FREEZE": "freeze",
    "CONFUSION": "confusion", "BLEED": "bleeding", "CURSE": "curse",
    "PARALYSIS": "paralysis",
}

STAT_DE = {
    "ATTACK": "Angriff", "DEFENSE": "Verteidigung", "MAGIC": "Magie",
    "RESISTANCE": "Magieabwehr", "SPEED": "Initiative", "LUCK": "Glück",
}
STAT_EN = {
    "ATTACK": "Attack", "DEFENSE": "Defense", "MAGIC": "Magic",
    "RESISTANCE": "Resistance", "SPEED": "Speed", "LUCK": "Luck",
}

# (suffix_de, suffix_en) pools per archetype.
SUFFIXES = {
    "phys": [("klaue", "Claw"), ("hieb", "Slash"), ("stoß", "Thrust"), ("biss", "Bite"),
             ("schlag", "Strike"), ("pranke", "Paw")],
    "mag": [("sturm", "Storm"), ("welle", "Wave"), ("ruf", "Call"), ("schleier", "Veil"),
            ("strahl", "Beam"), ("flut", "Surge")],
    "ult": [("zorn", "Wrath"), ("verderben", "Doom"), ("sturz", "Fall"), ("gericht", "Judgement")],
    "status": [("fessel", "Bind"), ("hauch", "Breath"), ("kuss", "Kiss"), ("mal", "Mark")],
    "debuff": [("brecher", "Breaker"), ("schwund", "Wane"), ("zehrer", "Sapper")],
    "multi": [("salve", "Volley"), ("hagel", "Hail"), ("regen", "Rain")],
    "drain": [("sauger", "Siphon"), ("zehr", "Leech"), ("durst", "Thirst")],
    "prio": [("vorstoß", "Lunge"), ("satz", "Dash"), ("stich", "Jab")],
    "buff": [("segen", "Boon"), ("rausch", "Frenzy"), ("weihe", "Rite")],
    "shield": [("wall", "Wall"), ("bollwerk", "Bulwark"), ("schild", "Shield")],
    "combo": [("bund", "Bond"), ("kette", "Chain"), ("gleichklang", "Unison")],
    "weather": [("himmel", "Sky"), ("zeichen", "Sign"), ("wende", "Turn")],
}

COMBO_TAGS = ["runic_chain", "elemental_bond", "twin_fang", "storm_call", "sacred_unison"]


def _name(element: str, archetype: str, index: int, roll) -> tuple[str, str]:
    prefix_de = PREFIXES[element][index % len(PREFIXES[element])]
    prefix_en = PREFIXES_EN[element][index % len(PREFIXES_EN[element])]
    suffix_de, suffix_en = SUFFIXES[archetype][index % len(SUFFIXES[archetype])]
    return f"{prefix_de}{suffix_de}", f"{prefix_en} {suffix_en}"


def _move(
    move_id: str,
    element: str,
    category: str,
    power: int,
    accuracy: int,
    pp: int,
    tier: int,
    effects: list[dict] | None = None,
    priority: int = 0,
    target: str = "SINGLE_OPPONENT",
    always_hits: bool = False,
    crit_bonus: int = 0,
    combo_tag: str | None = None,
    contact: bool | None = None,
) -> dict:
    payload = {
        "id": move_id,
        "nameKey": f"{move_id}_name",
        "descriptionKey": f"{move_id}_desc",
        "element": element,
        "category": category,
        "power": power,
        "accuracy": accuracy,
        "maxPp": pp,
        "priority": priority,
        "target": target,
        "effects": effects or [],
        "critStageBonus": crit_bonus,
        "contact": contact if contact is not None else category == "PHYSICAL",
        "alwaysHits": always_hits,
        "ignoresProtection": False,
        "animationKey": f"anim_{element.lower()}_{category.lower()}_{tier}",
        "soundKey": f"sfx_{element.lower()}_{tier}",
        "tier": tier,
    }
    if combo_tag:
        payload["comboTag"] = combo_tag
    return payload


def build_moves(strings: Strings) -> list[dict]:
    roll = rng("moves")
    moves: list[dict] = []

    for element_index, element in enumerate(ELEMENTS):
        de_word = ELEMENT_DE[element]
        en_word = ELEMENT_EN[element]
        status = ELEMENT_STATUS[element]

        # --- damage ladder, physical and magical -------------------------
        ladder = [
            ("phys", "PHYSICAL", 45, 100, 35, 1),
            ("phys", "PHYSICAL", 70, 95, 25, 2),
            ("phys", "PHYSICAL", 92, 90, 15, 3),
            ("ult", "PHYSICAL", 125, 85, 8, 4),
            ("mag", "MAGICAL", 50, 100, 30, 1),
            ("mag", "MAGICAL", 75, 95, 20, 2),
            ("mag", "MAGICAL", 98, 90, 12, 3),
            ("ult", "MAGICAL", 135, 85, 6, 4),
        ]
        for slot, (archetype, category, power, accuracy, pp, tier) in enumerate(ladder):
            name_de, name_en = _name(element, archetype, element_index + slot * 3, roll)
            move_id = f"mv_{slug(name_de)}"
            effects = []
            if tier == 4 and category == "PHYSICAL":
                effects.append({"type": "Recoil", "fraction": 0.30, "chance": 1.0})
            if tier == 4 and category == "MAGICAL":
                effects.append({"type": "Charge", "messageKey": "msg_charging_generic", "chance": 1.0})
            if tier == 3:
                effects.append({"type": "InflictStatus", "condition": status, "chance": 0.15})
            moves.append(
                _move(move_id, element, category, power, accuracy, pp, tier, effects,
                      crit_bonus=1 if tier == 3 else 0)
            )
            strings.add(f"{move_id}_name", name_de, name_en)
            strings.add(
                f"{move_id}_desc",
                f"Ein{'e' if name_de.endswith(('e', 'welle', 'flut', 'klaue', 'salve'))  else ''} "
                f"{de_word}-Angriff der Stufe {tier}. Fügt "
                f"{'physischen' if category == 'PHYSICAL' else 'magischen'} Schaden zu"
                + (f" und kann {STATUS_DE[status]} verursachen." if tier == 3 else "."),
                f"A tier-{tier} {en_word.lower()} attack dealing "
                f"{'physical' if category == 'PHYSICAL' else 'magical'} damage"
                + (f", with a chance to inflict {STATUS_EN[status]}." if tier == 3 else "."),
            )

        # --- status ------------------------------------------------------
        name_de, name_en = _name(element, "status", element_index, roll)
        move_id = f"mv_{slug(name_de)}"
        moves.append(
            _move(move_id, element, "SUPPORT", 0, 88, 18, 2,
                  [{"type": "InflictStatus", "condition": status, "chance": 1.0}])
        )
        strings.add(f"{move_id}_name", name_de, name_en)
        strings.add(
            f"{move_id}_desc",
            f"Belegt das Ziel mit {STATUS_DE[status]}.",
            f"Afflicts the target with {STATUS_EN[status]}.",
        )

        # --- debuff ------------------------------------------------------
        debuff_stat = ["ATTACK", "DEFENSE", "MAGIC", "RESISTANCE", "SPEED"][element_index % 5]
        name_de, name_en = _name(element, "debuff", element_index + 1, roll)
        move_id = f"mv_{slug(name_de)}"
        moves.append(
            _move(move_id, element, "SUPPORT", 0, 95, 20, 2,
                  [{"type": "ModifyStat", "stat": debuff_stat, "stages": -2,
                    "onSelf": False, "chance": 1.0}])
        )
        strings.add(f"{move_id}_name", name_de, name_en)
        strings.add(
            f"{move_id}_desc",
            f"Senkt {STAT_DE[debuff_stat]} des Ziels deutlich.",
            f"Sharply lowers the target's {STAT_EN[debuff_stat]}.",
        )

        # --- multi hit ---------------------------------------------------
        name_de, name_en = _name(element, "multi", element_index + 2, roll)
        move_id = f"mv_{slug(name_de)}"
        moves.append(
            _move(move_id, element, "PHYSICAL", 22, 90, 15, 2,
                  [{"type": "MultiHit", "min": 2, "max": 5, "chance": 1.0}])
        )
        strings.add(f"{move_id}_name", name_de, name_en)
        strings.add(
            f"{move_id}_desc",
            "Trifft zwei- bis fünfmal hintereinander.",
            "Strikes two to five times in a row.",
        )

        # --- drain -------------------------------------------------------
        name_de, name_en = _name(element, "drain", element_index + 3, roll)
        move_id = f"mv_{slug(name_de)}"
        moves.append(
            _move(move_id, element, "MAGICAL", 62, 100, 12, 3,
                  [{"type": "Drain", "fraction": 0.5, "chance": 1.0}])
        )
        strings.add(f"{move_id}_name", name_de, name_en)
        strings.add(
            f"{move_id}_desc",
            "Entzieht dem Ziel Lebenskraft und heilt den Anwender um die Hälfte des Schadens.",
            "Drains the target and heals the user for half the damage dealt.",
        )

        # --- priority ----------------------------------------------------
        name_de, name_en = _name(element, "prio", element_index + 4, roll)
        move_id = f"mv_{slug(name_de)}"
        moves.append(_move(move_id, element, "PHYSICAL", 42, 100, 25, 2, priority=1))
        strings.add(f"{move_id}_name", name_de, name_en)
        strings.add(
            f"{move_id}_desc",
            "Ein blitzschneller Angriff, der fast immer zuerst trifft.",
            "A lightning-quick attack that almost always strikes first.",
        )

        # --- self buff ---------------------------------------------------
        buff_stat = ["ATTACK", "MAGIC", "SPEED", "DEFENSE", "RESISTANCE"][element_index % 5]
        name_de, name_en = _name(element, "buff", element_index + 5, roll)
        move_id = f"mv_{slug(name_de)}"
        moves.append(
            _move(move_id, element, "SUPPORT", 0, 100, 15, 2,
                  [{"type": "ModifyStat", "stat": buff_stat, "stages": 2,
                    "onSelf": True, "chance": 1.0}],
                  target="SELF", always_hits=True)
        )
        strings.add(f"{move_id}_name", name_de, name_en)
        strings.add(
            f"{move_id}_desc",
            f"Erhöht den eigenen {STAT_DE[buff_stat]} deutlich.",
            f"Sharply raises the user's {STAT_EN[buff_stat]}.",
        )

        # --- shield ------------------------------------------------------
        name_de, name_en = _name(element, "shield", element_index + 6, roll)
        move_id = f"mv_{slug(name_de)}"
        moves.append(
            _move(move_id, element, "SUPPORT", 0, 100, 10, 3,
                  [{"type": "RaiseShield", "fraction": 0.30, "turns": 4, "chance": 1.0}],
                  target="SELF", always_hits=True)
        )
        strings.add(f"{move_id}_name", name_de, name_en)
        strings.add(
            f"{move_id}_desc",
            f"Errichtet einen {de_word}-Schild, der 30 % der eigenen maximalen KP absorbiert.",
            f"Raises a {en_word.lower()} shield absorbing 30 % of the user's max HP.",
        )

        # --- combo -------------------------------------------------------
        tag = COMBO_TAGS[element_index % len(COMBO_TAGS)]
        name_de, name_en = _name(element, "combo", element_index + 7, roll)
        move_id = f"mv_{slug(name_de)}"
        moves.append(
            _move(move_id, element, "MAGICAL", 68, 95, 12, 3, combo_tag=tag)
        )
        strings.add(f"{move_id}_name", name_de, name_en)
        strings.add(
            f"{move_id}_desc",
            "Verbindet sich mit gleichartigen Attacken von Verbündeten zu einem Kombinationsschlag.",
            "Links with allied moves of the same kind into a combination strike.",
        )

        # --- spread ------------------------------------------------------
        name_de, name_en = _name(element, "mag", element_index + 11, roll)
        move_id = f"mv_{slug(name_de)}_weit"
        moves.append(
            _move(move_id, element, "MAGICAL", 58, 92, 10, 3, target="ALL_OPPONENTS")
        )
        strings.add(f"{move_id}_name", f"Weit{name_de.lower()}", f"Wide {name_en}")
        strings.add(
            f"{move_id}_desc",
            "Trifft alle Gegner auf dem Feld.",
            "Hits every opponent on the field.",
        )

        # --- team buff ---------------------------------------------------
        team_stat = ["DEFENSE", "RESISTANCE", "ATTACK", "MAGIC", "SPEED"][element_index % 5]
        name_de, name_en = _name(element, "buff", element_index + 9, roll)
        move_id = f"mv_{slug(name_de)}_bund"
        moves.append(
            _move(move_id, element, "SUPPORT", 0, 100, 10, 3,
                  [{"type": "ModifyStat", "stat": team_stat, "stages": 1,
                    "onSelf": False, "chance": 1.0}],
                  target="ALL_ALLIES", always_hits=True)
        )
        strings.add(f"{move_id}_name", f"{name_de} des Bundes", f"{name_en} of the Bond")
        strings.add(
            f"{move_id}_desc",
            f"Erhöht {STAT_DE[team_stat]} des gesamten Teams.",
            f"Raises the whole team's {STAT_EN[team_stat]}.",
        )

        # --- high-crit finisher ------------------------------------------
        name_de, name_en = _name(element, "phys", element_index + 10, roll)
        move_id = f"mv_{slug(name_de)}_jagd"
        moves.append(
            _move(move_id, element, "PHYSICAL", 78, 95, 12, 3, crit_bonus=2)
        )
        strings.add(f"{move_id}_name", f"Jagd{name_de.lower()}", f"Hunting {name_en}")
        strings.add(
            f"{move_id}_desc",
            "Ein gezielter Hieb mit stark erhöhter Chance auf einen kritischen Treffer.",
            "A precise blow with a greatly increased critical-hit rate.",
        )

        # --- weather (only for elements that own one) --------------------
        if element in ELEMENT_WEATHER:
            weather = ELEMENT_WEATHER[element]
            name_de, name_en = _name(element, "weather", element_index + 8, roll)
            move_id = f"mv_{slug(name_de)}"
            moves.append(
                _move(move_id, element, "SUPPORT", 0, 100, 8, 3,
                      [{"type": "SetWeather", "weather": weather, "turns": 5, "chance": 1.0}],
                      target="FIELD", always_hits=True)
            )
            strings.add(f"{move_id}_name", name_de, name_en)
            strings.add(
                f"{move_id}_desc",
                "Ruft für fünf Runden ein passendes Wetter herbei.",
                "Summons matching weather for five turns.",
            )

    moves.extend(_universal_moves(strings))
    moves.extend(_signature_moves(strings))

    # Guard against accidental id collisions.
    seen = set()
    for move in moves:
        if move["id"] in seen:
            raise ValueError(f"duplicate move id {move['id']}")
        seen.add(move["id"])
    return moves


def _universal_moves(strings: Strings) -> list[dict]:
    """Element-neutral utility moves available to nearly every species."""
    entries = [
        # (id, name_de, name_en, element, category, power, acc, pp, tier, effects, extra, desc_de, desc_en)
        ("mv_heilende_hand", "Heilende Hand", "Healing Hand", "NATURE", "SUPPORT", 0, 100, 10, 2,
         [{"type": "Heal", "fraction": 0.5, "onSelf": True, "chance": 1.0}],
         {"target": "SELF", "always_hits": True},
         "Stellt die Hälfte der eigenen maximalen KP wieder her.",
         "Restores half of the user's maximum HP."),
        ("mv_ahnenruf", "Ahnenruf", "Ancestor's Call", "SPIRIT", "SUPPORT", 0, 100, 5, 3,
         [{"type": "Heal", "fraction": 0.6, "onSelf": False, "chance": 1.0}],
         {"target": "SINGLE_ALLY", "always_hits": True},
         "Heilt einen Verbündeten um 60 % seiner maximalen KP.",
         "Heals an ally for 60 % of their maximum HP."),
        ("mv_runenschirm", "Runenschirm", "Rune Screen", "RUNE", "SUPPORT", 0, 100, 10, 2,
         [{"type": "Protect", "turns": 1, "chance": 1.0}],
         {"target": "SELF", "always_hits": True, "priority": 4},
         "Wehrt den nächsten Angriff vollständig ab. Bei wiederholtem Einsatz unzuverlässig.",
         "Blocks the next incoming attack. Unreliable when used repeatedly."),
        ("mv_reinigende_flamme", "Reinigende Flamme", "Cleansing Flame", "LIGHT", "SUPPORT", 0, 100, 10, 2,
         [{"type": "CureStatus", "onSelf": True, "chance": 1.0}],
         {"target": "SELF", "always_hits": True},
         "Heilt die eigene Statusveränderung.",
         "Cures the user's status condition."),
        ("mv_runenfessel", "Runenfessel", "Rune Fetter", "RUNE", "SUPPORT", 0, 92, 10, 3,
         [{"type": "WeakenForCapture", "multiplier": 1.8, "turns": 4, "chance": 1.0}],
         {},
         "Bindet ein wildes Monster mit Runen und erhöht die Fangchance deutlich.",
         "Binds a wild monster in runes, greatly raising the capture chance."),
        ("mv_seherblick", "Seherblick", "Seer's Gaze", "LIGHT", "SUPPORT", 0, 100, 15, 2,
         [{"type": "ModifyRatio", "kind": "ACCURACY", "stages": 2, "onSelf": True, "chance": 1.0}],
         {"target": "SELF", "always_hits": True},
         "Schärft den Blick und erhöht die eigene Genauigkeit.",
         "Sharpens the user's aim, raising accuracy."),
        ("mv_nebelschritt", "Nebelschritt", "Mist Step", "WIND", "SUPPORT", 0, 100, 15, 2,
         [{"type": "ModifyRatio", "kind": "EVASION", "stages": 2, "onSelf": True, "chance": 1.0}],
         {"target": "SELF", "always_hits": True},
         "Erhöht die eigene Ausweichrate deutlich.",
         "Sharply raises the user's evasion."),
        ("mv_bannspruch", "Bannspruch", "Dispel Verse", "RUNE", "SUPPORT", 0, 100, 10, 3,
         [{"type": "ClearStatChanges", "positiveOnly": True, "chance": 1.0}],
         {"always_hits": True},
         "Löscht sämtliche Verstärkungen des Ziels.",
         "Removes every stat boost from the target."),
        ("mv_urteil_der_norne", "Urteil der Norne", "Norn's Verdict", "DIVINE", "MAGICAL", 0, 100, 5, 4,
         [{"type": "PercentDamage", "fraction": 0.5, "chance": 1.0}],
         {"always_hits": True},
         "Nimmt dem Ziel die Hälfte seiner aktuellen KP — unabhängig von Werten.",
         "Removes half of the target's current HP, ignoring all stats."),
        ("mv_schicksalsschlag", "Schicksalsschlag", "Fated Blow", "CHAOS", "PHYSICAL", 0, 85, 5, 4,
         [{"type": "FixedDamage", "amount": 60, "chance": 1.0}],
         {},
         "Fügt stets genau 60 Schadenspunkte zu.",
         "Always deals exactly 60 damage."),
        ("mv_letztes_aufbaeumen", "Letztes Aufbäumen", "Last Stand", "CHAOS", "PHYSICAL", 70, 100, 5, 4,
         [{"type": "ScaleWithMissingHp", "maxMultiplier": 2.5, "chance": 1.0}],
         {},
         "Wird umso stärker, je weniger KP der Anwender noch besitzt.",
         "Grows stronger the less HP the user has left."),
        ("mv_kriegsrausch", "Kriegsrausch", "War Frenzy", "METAL", "PHYSICAL", 60, 100, 10, 3,
         [{"type": "ScaleWithBuffs", "perStage": 0.2, "chance": 1.0}],
         {},
         "Gewinnt Stärke aus jeder eigenen Wertsteigerung.",
         "Gains power from each of the user's stat boosts."),
        ("mv_jaegerinstinkt", "Jägerinstinkt", "Hunter's Instinct", "NATURE", "PHYSICAL", 75, 100, 10, 3,
         [{"type": "BonusVersusStatus", "condition": "SLEEP", "multiplier": 2.0, "chance": 1.0}],
         {},
         "Verdoppelt den Schaden gegen schlafende Ziele.",
         "Deals double damage to sleeping targets."),
        ("mv_diebesgriff", "Diebesgriff", "Thief's Grasp", "SHADOW", "PHYSICAL", 45, 100, 15, 2,
         [{"type": "StealItem", "chance": 1.0}],
         {},
         "Entwendet dem Ziel sein getragenes Objekt.",
         "Steals the target's held item."),
        ("mv_spiegelrune", "Spiegelrune", "Mirror Rune", "RUNE", "SUPPORT", 0, 100, 10, 3,
         [{"type": "MirrorMove", "chance": 1.0}],
         {"always_hits": True},
         "Kopiert die zuletzt vom Ziel eingesetzte Attacke.",
         "Copies the move the target used last."),
        ("mv_wirbelwechsel", "Wirbelwechsel", "Whirl Switch", "WIND", "PHYSICAL", 55, 100, 15, 2,
         [{"type": "SwitchOut", "chance": 1.0}],
         {},
         "Greift an und tauscht anschließend das Monster aus.",
         "Attacks, then swaps the user out."),
        ("mv_durchbruch", "Durchbruch", "Breakthrough", "EARTH", "PHYSICAL", 80, 95, 10, 3,
         [{"type": "IgnoreDefenceStages", "chance": 1.0}],
         {},
         "Ignoriert sämtliche Verteidigungsverstärkungen des Ziels.",
         "Ignores every defensive boost of the target."),
        ("mv_koenigsklinge", "Königsklinge", "King's Edge", "METAL", "PHYSICAL", 85, 95, 8, 4,
         [{"type": "AlwaysCritical", "chance": 1.0}],
         {},
         "Trifft immer kritisch.",
         "Always lands a critical hit."),
        ("mv_odems_gnade", "Odems Gnade", "Grace of Breath", "DIVINE", "SUPPORT", 0, 100, 5, 4,
         [{"type": "Heal", "fraction": 0.35, "onSelf": False, "chance": 1.0},
          {"type": "CureStatus", "onSelf": False, "chance": 1.0}],
         {"target": "ALL_ALLIES", "always_hits": True},
         "Heilt das gesamte Team und befreit es von Statusveränderungen.",
         "Heals the whole team and clears their status conditions."),
        ("mv_wildschlag", "Wildschlag", "Wild Strike", "NATURE", "PHYSICAL", 100, 80, 10, 3,
         [{"type": "InflictStatus", "condition": "CONFUSION", "chance": 0.30}],
         {},
         "Ein ungestümer Hieb, der das Ziel verwirren kann.",
         "A reckless blow that may confuse the target."),
    ]

    moves = []
    for (move_id, name_de, name_en, element, category, power, accuracy, pp, tier,
         effects, extra, desc_de, desc_en) in entries:
        moves.append(
            _move(move_id, element, category, power, accuracy, pp, tier, effects,
                  priority=extra.get("priority", 0),
                  target=extra.get("target", "SINGLE_OPPONENT"),
                  always_hits=extra.get("always_hits", False))
        )
        strings.add(f"{move_id}_name", name_de, name_en)
        strings.add(f"{move_id}_desc", desc_de, desc_en)
    return moves


# Signature moves of the legendary guardians. Each is tied to one species by
# `monsters.py`, which is why they carry hand-written flavour.
SIGNATURES = [
    ("mv_weltenbrand", "Weltenbrand", "World Conflagration", "FIRE", "MAGICAL", 140, 90,
     "Der Brand, der am Ende aller Tage die Neun Welten verzehrt.",
     "The blaze that devours the Nine Worlds at the end of days."),
    ("mv_urflut", "Urflut", "Primordial Tide", "WATER", "MAGICAL", 135, 90,
     "Die Flut, aus der die Welt einst stieg — und in die sie zurücksinkt.",
     "The tide the world rose from, and will sink back into."),
    ("mv_ewiger_winter", "Ewiger Winter", "Endless Winter", "ICE", "MAGICAL", 130, 90,
     "Der Fimbulwinter, der drei Sommer verschlingt.",
     "The Fimbulwinter that swallows three summers."),
    ("mv_sturmgesang", "Sturmgesang", "Storm Song", "WIND", "MAGICAL", 128, 95,
     "Der Gesang der Winde über den neun Himmeln.",
     "The song of the winds across nine heavens."),
    ("mv_weltengrund", "Weltengrund", "World's Foundation", "EARTH", "PHYSICAL", 138, 90,
     "Ein Schlag, der die Wurzeln des Weltenbaums erzittern lässt.",
     "A blow that shakes the roots of the World Tree."),
    ("mv_lebenssaat", "Lebenssaat", "Seed of Life", "NATURE", "MAGICAL", 125, 95,
     "Der Same, aus dem nach Ragnarök alles neu wächst.",
     "The seed from which all things regrow after Ragnarok."),
    ("mv_himmelszorn", "Himmelszorn", "Wrath of the Sky", "THUNDER", "MAGICAL", 142, 88,
     "Der Hammerschlag, der Riesen fällt.",
     "The hammer-fall that levels giants."),
    ("mv_erster_morgen", "Erster Morgen", "First Morning", "LIGHT", "MAGICAL", 132, 92,
     "Das Licht des allerersten Morgens über Gimlé.",
     "The light of the very first morning over Gimlé."),
    ("mv_langnacht", "Langnacht", "Long Night", "SHADOW", "MAGICAL", 134, 90,
     "Die Nacht, die kein Morgen kennt.",
     "The night that knows no morning."),
    ("mv_seelenernte", "Seelenernte", "Soul Harvest", "SPIRIT", "MAGICAL", 130, 90,
     "Die Ernte der Ungezählten in Helheims Hallen.",
     "The harvest of the uncounted in Hel's halls."),
    ("mv_erstes_zeichen", "Erstes Zeichen", "The First Sign", "RUNE", "MAGICAL", 136, 92,
     "Die Rune, die Odin am Weltenbaum hängend errang.",
     "The rune Odin won while hanging from the World Tree."),
    ("mv_ragnaroek", "Ragnarök", "Ragnarok", "CHAOS", "MAGICAL", 150, 85,
     "Das Ende — und nur vielleicht ein Anfang.",
     "The end — and, only perhaps, a beginning."),
    ("mv_zwergengericht", "Zwergengericht", "Dwarven Judgement", "METAL", "PHYSICAL", 140, 88,
     "Geschmiedet in einer Esse, die niemals erlosch.",
     "Forged in a hearth that never went cold."),
    ("mv_asenurteil", "Asenurteil", "Judgement of the Aesir", "DIVINE", "MAGICAL", 145, 88,
     "Das letzte Wort des Thing der Götter.",
     "The final word of the gods' assembly."),
]


def _signature_moves(strings: Strings) -> list[dict]:
    moves = []
    for move_id, name_de, name_en, element, category, power, accuracy, flavour_de, flavour_en in SIGNATURES:
        moves.append(
            _move(move_id, element, category, power, accuracy, 5, 5,
                  [{"type": "InflictStatus",
                    "condition": ELEMENT_STATUS[element], "chance": 0.30}],
                  crit_bonus=1)
        )
        strings.add(f"{move_id}_name", name_de, name_en)
        strings.add(
            f"{move_id}_desc",
            f"{flavour_de} Extrem starker Angriff mit Zusatzwirkung.",
            f"{flavour_en} An extremely powerful attack with a secondary effect.",
        )
    return moves
