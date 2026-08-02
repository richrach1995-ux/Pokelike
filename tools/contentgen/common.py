"""Shared helpers for the Runeveil content pipeline.

The pipeline turns compact, hand-authored seed tables into the JSON content
files shipped in ``app/src/main/assets``.  Two things matter here:

* **Determinism** — every generator seeds its RNG from a fixed constant, so
  regenerating content produces a byte-identical result.  Content diffs are
  therefore reviewable.
* **Separation of data and text** — content files carry *keys*
  (``species_glutwelp_name``); the human-readable German and English strings go
  into ``assets/i18n/<lang>.json``.  That keeps the data files language neutral
  and avoids a 40 000-line ``strings.xml``.
"""

from __future__ import annotations

import json
import os
import random
import unicodedata
from dataclasses import dataclass, field

SEED = 20250801

REPO_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
ASSET_ROOT = os.path.join(REPO_ROOT, "app", "src", "main", "assets")
CONTENT_DIR = os.path.join(ASSET_ROOT, "content")
I18N_DIR = os.path.join(ASSET_ROOT, "i18n")

ELEMENTS = [
    "FIRE", "WATER", "ICE", "WIND", "EARTH", "NATURE", "THUNDER",
    "LIGHT", "SHADOW", "SPIRIT", "RUNE", "CHAOS", "METAL", "DIVINE",
]

WORLDS = [
    "MIDGARD", "ASGARD", "VANAHEIM", "ALFHEIM", "JOTUNHEIM",
    "MUSPELHEIM", "NIFLHEIM", "HELHEIM", "SVARTALFHEIM",
]

RARITIES = ["COMMON", "UNCOMMON", "RARE", "EPIC", "LEGENDARY", "MYTHIC", "DIVINE"]

STATUSES = ["BURN", "POISON", "SLEEP", "FREEZE", "CONFUSION", "BLEED", "CURSE", "PARALYSIS"]

WEATHERS = [
    "CLEAR", "RAIN", "THUNDERSTORM", "SNOW", "FOG", "HEATWAVE", "AURORA", "SANDSTORM",
]

# German element nouns used when composing names and descriptions.
ELEMENT_DE = {
    "FIRE": "Feuer", "WATER": "Wasser", "ICE": "Eis", "WIND": "Wind",
    "EARTH": "Erde", "NATURE": "Natur", "THUNDER": "Donner", "LIGHT": "Licht",
    "SHADOW": "Schatten", "SPIRIT": "Geist", "RUNE": "Runen", "CHAOS": "Chaos",
    "METAL": "Metall", "DIVINE": "Göttlich",
}

ELEMENT_EN = {
    "FIRE": "Fire", "WATER": "Water", "ICE": "Ice", "WIND": "Wind",
    "EARTH": "Earth", "NATURE": "Nature", "THUNDER": "Thunder", "LIGHT": "Light",
    "SHADOW": "Shadow", "SPIRIT": "Spirit", "RUNE": "Rune", "CHAOS": "Chaos",
    "METAL": "Metal", "DIVINE": "Divine",
}

WORLD_DE = {
    "MIDGARD": "Midgard", "ASGARD": "Asgard", "VANAHEIM": "Vanaheim",
    "ALFHEIM": "Alfheim", "JOTUNHEIM": "Jötunheim", "MUSPELHEIM": "Muspelheim",
    "NIFLHEIM": "Niflheim", "HELHEIM": "Helheim", "SVARTALFHEIM": "Svartalfheim",
}


def slug(text: str) -> str:
    """ASCII slug used for ids: ``Glutwelp`` -> ``glutwelp``."""
    normalised = (
        text.replace("ä", "ae").replace("ö", "oe").replace("ü", "ue")
        .replace("Ä", "Ae").replace("Ö", "Oe").replace("Ü", "Ue")
        .replace("ß", "ss")
    )
    normalised = unicodedata.normalize("NFKD", normalised)
    normalised = "".join(c for c in normalised if not unicodedata.combining(c))
    out = []
    for char in normalised.lower():
        if char.isalnum():
            out.append(char)
        elif out and out[-1] != "_":
            out.append("_")
    return "".join(out).strip("_")


@dataclass
class Strings:
    """Collects the localisation entries produced while generating content."""

    de: dict[str, str] = field(default_factory=dict)
    en: dict[str, str] = field(default_factory=dict)

    def add(self, key: str, german: str, english: str) -> str:
        if key in self.de and self.de[key] != german:
            raise ValueError(f"duplicate string key with different text: {key}")
        self.de[key] = german
        self.en[key] = english
        return key

    def merge(self, other: "Strings") -> None:
        for key, value in other.de.items():
            self.de.setdefault(key, value)
        for key, value in other.en.items():
            self.en.setdefault(key, value)

    def __len__(self) -> int:  # pragma: no cover - diagnostics only
        return len(self.de)


def rng(salt: str) -> random.Random:
    """A deterministic RNG namespaced by [salt]."""
    return random.Random(f"{SEED}:{salt}")


def write_json(path: str, payload) -> int:
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as handle:
        json.dump(payload, handle, ensure_ascii=False, indent=1, sort_keys=False)
        handle.write("\n")
    return os.path.getsize(path)


# ---------------------------------------------------------------------------
# Stat budgets
# ---------------------------------------------------------------------------

# Role -> relative weights for (hp, atk, def, mag, res, spd, lck).
ROLE_WEIGHTS = {
    "attacker":  (0.95, 1.45, 0.85, 0.70, 0.80, 1.15, 0.85),
    "mage":      (0.90, 0.65, 0.80, 1.50, 0.95, 1.05, 0.90),
    "bruiser":   (1.15, 1.30, 1.05, 0.80, 0.90, 0.85, 0.80),
    "tank":      (1.35, 0.90, 1.45, 0.75, 1.10, 0.65, 0.80),
    "wall":      (1.30, 0.70, 1.20, 0.85, 1.50, 0.65, 0.85),
    "speedster": (0.85, 1.15, 0.75, 1.00, 0.75, 1.60, 1.00),
    "support":   (1.05, 0.80, 1.05, 1.05, 1.20, 0.95, 1.15),
    "glass":     (0.70, 1.55, 0.60, 1.45, 0.60, 1.30, 0.90),
    "balanced":  (1.05, 1.05, 1.05, 1.05, 1.05, 1.00, 1.00),
}

# Total stat budget per evolution stage and rarity tier.
STAGE_BUDGET = {1: 320, 2: 425, 3: 530}
RARITY_BONUS = {
    "COMMON": 0, "UNCOMMON": 15, "RARE": 35, "EPIC": 60,
    "LEGENDARY": 95, "MYTHIC": 120, "DIVINE": 145,
}


def stat_block(role: str, stage: int, rarity: str, jitter: random.Random) -> dict[str, int]:
    """Distributes a stat budget across the seven stats for one species.

    The budget depends on the evolution stage and rarity; the role decides how
    it is split.  A small deterministic jitter keeps species inside a family
    from feeling mechanically identical.
    """
    weights = ROLE_WEIGHTS[role]
    budget = STAGE_BUDGET[stage] + RARITY_BONUS[rarity]
    total_weight = sum(weights)
    keys = ["hp", "attack", "defense", "magic", "resistance", "speed", "luck"]
    values = {}
    for key, weight in zip(keys, weights):
        share = budget * weight / total_weight
        share *= jitter.uniform(0.94, 1.06)
        values[key] = max(5, min(190, int(round(share))))
    # Luck is a soft stat and stays in a narrower band.
    values["luck"] = max(20, min(110, int(values["luck"] * 0.75)))
    return values


def catch_rate(rarity: str, stage: int) -> int:
    base = {
        "COMMON": 200, "UNCOMMON": 150, "RARE": 95,
        "EPIC": 55, "LEGENDARY": 18, "MYTHIC": 8, "DIVINE": 3,
    }[rarity]
    return max(3, int(base * (1.0 - 0.22 * (stage - 1))))


def base_experience(stage: int, rarity: str) -> int:
    return int((48 + stage * 26) * (1.0 + RARITY_BONUS[rarity] / 200.0))
