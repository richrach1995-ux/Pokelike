#!/usr/bin/env python3
"""Runeveil content build.

Regenerates every JSON asset under ``app/src/main/assets`` from the seed tables
in this directory, then runs the referential-integrity validator.

Usage::

    python3 tools/contentgen/build.py            # build + validate
    python3 tools/contentgen/build.py --check    # validate only, no writes

The build is deterministic: running it twice produces byte-identical files.
"""

from __future__ import annotations

import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from common import CONTENT_DIR, I18N_DIR, Strings, write_json  # noqa: E402
from items import (  # noqa: E402
    build_abilities, build_achievements, build_items, build_recipes, build_talents, build_titles,
)
from monsters import build_monsters  # noqa: E402
from moves import build_moves  # noqa: E402
from story import build_factions, build_npcs, build_story  # noqa: E402
from validate import validate  # noqa: E402
from world import build_world  # noqa: E402


def build() -> tuple[dict, Strings]:
    strings = Strings()

    moves = build_moves(strings)
    abilities = build_abilities(strings)
    species = build_monsters(strings, moves)
    items = build_items(strings)
    recipes = build_recipes(strings, items)
    talents = build_talents(strings)
    titles = build_titles(strings)
    achievements = build_achievements(strings)
    regions, encounters, shops, chests = build_world(strings, species, items)
    factions = build_factions(strings)
    npcs, teams, dialogues = build_npcs(strings, regions)
    chapters, quests, cutscenes, endings = build_story(strings, regions, npcs, species)

    content = {
        "moves": moves,
        "abilities": abilities,
        "monsters": species,
        "items": items,
        "recipes": recipes,
        "talents": talents,
        "titles": titles,
        "achievements": achievements,
        "regions": regions,
        "encounters": encounters,
        "shops": shops,
        "chests": chests,
        "factions": factions,
        "npcs": npcs,
        "trainers": teams,
        "dialogues": dialogues,
        "chapters": chapters,
        "quests": quests,
        "cutscenes": cutscenes,
        "endings": endings,
    }
    return content, strings


# Each content bundle is written as its own asset so the loader can stream
# them in parallel and so diffs stay readable.
FILES = {
    "moves": "moves.json",
    "abilities": "abilities.json",
    "monsters": "monsters.json",
    "items": "items.json",
    "recipes": "recipes.json",
    "talents": "talents.json",
    "titles": "titles.json",
    "achievements": "achievements.json",
    "regions": "regions.json",
    "encounters": "encounters.json",
    "shops": "shops.json",
    "chests": "chests.json",
    "factions": "factions.json",
    "npcs": "npcs.json",
    "trainers": "trainers.json",
    "dialogues": "dialogues.json",
    "chapters": "chapters.json",
    "quests": "quests.json",
    "cutscenes": "cutscenes.json",
    "endings": "endings.json",
}


def main() -> int:
    check_only = "--check" in sys.argv
    content, strings = build()

    errors = validate(content, strings)
    if errors:
        print(f"\n✗ {len(errors)} Fehler in den Inhaltsdaten:\n")
        for error in errors[:60]:
            print(f"  - {error}")
        if len(errors) > 60:
            print(f"  … und {len(errors) - 60} weitere")
        return 1

    if check_only:
        print("✓ Inhalte sind gültig (nur Prüfung, nichts geschrieben).")
        return 0

    total_bytes = 0
    for key, filename in FILES.items():
        payload = content[key]
        # Talents ship as an object keyed by tree id; everything else is a list.
        total_bytes += write_json(os.path.join(CONTENT_DIR, filename), payload)

    total_bytes += write_json(os.path.join(I18N_DIR, "de.json"), strings.de)
    total_bytes += write_json(os.path.join(I18N_DIR, "en.json"), strings.en)

    manifest = {
        "schemaVersion": 1,
        "generator": "tools/contentgen/build.py",
        "counts": {key: len(content[key]) for key in FILES},
        "stringCount": len(strings.de),
        "languages": ["de", "en"],
    }
    total_bytes += write_json(os.path.join(CONTENT_DIR, "manifest.json"), manifest)

    print("✓ Inhalte erzeugt\n")
    for key in FILES:
        print(f"  {key:<14} {len(content[key]):>6}")
    print(f"  {'strings':<14} {len(strings.de):>6}")
    print(f"\n  Gesamtgröße: {total_bytes / 1024:.1f} KiB")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
