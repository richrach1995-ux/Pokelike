# Medien-Assets

**Monsterbilder werden nicht geladen, sondern erzeugt.** Jede Art wird aus ihrem
eigenen Bauplan gezeichnet — siehe [SPRITES.md](SPRITES.md). Es gibt daher keine
255 Bilddateien, keine Platzhalter und keinen Download dafür.

Ton- und Hintergrunddateien sind weiterhin nicht Teil dieses Repositorys. Das
Spiel adressiert sie über Schlüssel; fehlt eine Datei, bleibt es spielbar
(stiller No-Op beim Ton). So kann Spiellogik unabhängig von der Medienproduktion
entstehen.

## Verzeichnisse

```
app/src/main/assets/
├── content/    generiert — Spieldaten (JSON)
├── i18n/       generiert — Texte (DE/EN)
├── audio/      Musik und Effekte (.ogg)
└── sprites/    Porträts und Hintergründe (.webp) — Monster nicht, die werden gezeichnet
```

## Namensschema

| Art | Schlüssel | Datei |
|---|---|---|
| Regionsmusik | `bgm_<regionId>` | `audio/bgm_midgard_heartlands.ogg` |
| Nachtvariante | `bgm_<regionId>_night` | `audio/bgm_midgard_heartlands_night.ogg` |
| Kampfmusik | `bgm_battle_<typ>` | `audio/bgm_battle_boss.ogg` |
| Attackengeräusch | `sfx_<element>_<stufe>` | `audio/sfx_fire_3.ogg` |
| Monsterruf | `cry_<familyId>` | `audio/cry_glutwelp.ogg` |
| Porträt | `portrait_<npcId>` | `sprites/portrait_npc_seeress_vala.webp` |
| Hintergrund | `bg_<regionId>_<typ>` | `sprites/bg_muspelheim_emberfields_dungeon.webp` |

## Umfang

Aus den erzeugten Inhalten ergeben sich:

- 106 Monsterrufe (einer je Familie)
- 9 Regionsthemen plus 9 Nachtvarianten
- 7 Kampfthemen, 1 Titelthema, 1 Endthema
- ~40 Ortshintergründe
- 12 handgezeichnete Porträts für Story-Figuren, 10 generische je Rolle

## Animationen

Animationen sind datengetrieben (`AnimationSpec` in `MonsterSpecies`):
Bildanzahl, Bilddauer, Wiederholung, Beschleunigungskurve. Die Standardwerte sind
8 Bilder à 96 ms (Ruhe) und 6 Bilder à 64 ms (Angriff). Ein Sprite-Atlas heißt
`<spriteKey>_atlas.json`.

Bei aktivierter Einstellung „Weniger Bewegung" wird jede Dauer auf 0 skaliert;
`MotionSettings.scale(durationMs)` ist die einzige Stelle, an der das geschieht.

## Grafikstil

2D-HD, keine Pixelgrafik. Referenz sind moderne handgezeichnete JRPGs mit
kräftigen Silhouetten und beleuchteten Kanten. Farbleitlinie: Nachtblau als
Grund, gealtertes Gold als Akzent, Bifröst-Frost für Interaktion. Die
Elementfarben stehen in `Element.colorHex` und sind für Abzeichen und Effekte
verbindlich.
