# Runeveil — Saga der Neun

Ein vollständiges Monster-Sammel-RPG für Android, angesiedelt in einer eigenständigen
Welt auf Basis der nordischen Mythologie. Kotlin, Jetpack Compose, Clean Architecture.

> Der Weltenbaum stirbt. Die Grenzen zwischen den Neun Welten reißen auf, Chaoswesen
> strömen hindurch, und eine uralte Organisation arbeitet daran, Ragnarök nicht zu
> verhindern, sondern zu beschleunigen. Du wirst zum Runenwächter berufen.

---

## Inhalt in Zahlen

| Kategorie | Umfang |
|---|---|
| Monsterarten | **255** in 106 Entwicklungsfamilien |
| Attacken | **310** |
| Passive Fähigkeiten | 42 (alle im Kampfsystem implementiert) |
| Elemente | 14 mit vollständiger, balancierter Effektivitätsmatrix |
| Statusveränderungen | 8 |
| Wetterarten | 8 (kampfrelevant) |
| Regionen | 9 Welten mit 96 Orten |
| Begegnungstabellen | 50 |
| NPCs | 111 mit eigenen Dialogbäumen |
| Trainerteams | 33 |
| Fraktionen | 6 mit Rufsystem |
| Quests | 131 (40 Haupt-, 45 Neben-, 18 Fraktions-, 10 Tages-, 9 legendäre, 9 geheime) |
| Kapitel | 10 · Zwischensequenzen 24 · Enden 4 |
| Gegenstände | 161 · Rezepte 73 · Talentbäume 9 |
| Texte | 4 479 Schlüssel, zweisprachig (DE/EN) |

---

## Aufbau

```
Runeveil/
├── app/        Android-Anwendung: Compose-UI, Navigation, Audio, DI
├── data/       Room, DataStore, Inhalts-Loader, Repository-Implementierungen
├── domain/     Reines Kotlin: Modelle, Regeln, Kampf-Engine, Anwendungsfälle
├── tools/      Inhalts-Pipeline (Python) inkl. Validator
└── docs/       Architektur, Datenbankschema, Spieldesign, Klassenübersicht
```

Abhängigkeitsrichtung: `app → data → domain`. `domain` kennt weder Android noch
eine Datenbank und ist deshalb vollständig auf der JVM testbar.

Ausführliche Beschreibung: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

---

## Bauen

Voraussetzungen: JDK 17, Android Studio Ladybug (oder neuer), Android SDK 35.

```bash
./gradlew :app:assembleDebug     # APK bauen
./gradlew :domain:test           # Regel-Tests (116 Tests)
./gradlew qualityGate            # alle Tests + Debug-Build
```

Das Projekt lässt sich ohne weitere Anpassungen in Android Studio öffnen
(`File → Open` auf das Wurzelverzeichnis).

### Inhalte neu erzeugen

Sämtliche JSON-Assets werden generiert, nicht von Hand gepflegt:

```bash
python3 tools/contentgen/build.py          # erzeugen + prüfen
python3 tools/contentgen/build.py --check  # nur prüfen (CI)
```

Der Generator ist deterministisch — zweimaliges Ausführen liefert byte-identische
Dateien. Der Validator prüft jede Referenz (Attacken, Arten, Gegenstände, Orte,
NPCs, Quests) und gleicht Fähigkeits-Effekte gegen `AbilityEffects.kt` ab; ein
unbekannter Verweis lässt den Build fehlschlagen.

---

## Systeme

**Kampf** — rundenbasiert, bis zu drei Wesen pro Seite. Initiative, Priorität,
Trefferchance, kritische Treffer, Elementareffektivität, Affinitätsbonus,
Tageszeit- und Wettereinfluss, Schilde, Kombinationsschläge, acht
Statusveränderungen, 24 Attackeneffekte, fünf KI-Profile von „wild" bis
„mythisch". Die Engine ist eine reine Funktion aus (Zustand, Aktionen, Zufallsquelle)
und damit vollständig reproduzierbar.

**Sammeln** — acht Runenkugel-Grade mit situativen Boni (nachts, bei Statusleiden,
im Heimatreich …). Die Fangchance wird in vier Erschütterungen aufgelöst, jede
davon echt gewürfelt: ein knapper Fehlschlag *war* knapp.

**Zucht** — Genvererbung (3 bzw. 5 von 7), Temperament, Ei-Attacken, geerbte
Talente, Mutationen, Hybride, verbesserte Schimmer-Chance bei Eltern aus
verschiedenen Welten.

**Entwicklung** — durch Stufe, Gegenstand, Freundschaft, Tageszeit, Ort, Welt,
gebundene Rune, Geschlecht, bekannte Attacke, Wetter oder Story-Ereignis.

**Fortschritt** — Stufen bis 100, sieben Werte, Gene (0–31), Trainingswerte
(max. 510), Temperamente, Talentbäume mit erstattbaren Punkten, bindbare Runen.

**Welt** — Tag/Nacht in vier Phasen, regionales Wetter mit eigenen
Wahrscheinlichkeiten, tages- und wetterabhängige Begegnungstabellen.

**Weiteres** — Handwerk an vier Werkstätten (legendäre Rezepte können
fehlschlagen), Handel mit regionalen Aufschlägen, Fraktionsruf mit fünf Rängen,
Errungenschaften, Titel mit spielmechanischen Boni, mehrere Speicherstände plus
Autosave, New Game Plus.

---

## Qualitätssicherung

- **116 Unit-Tests** decken Typentabelle, Werteformeln, Erfahrungskurve,
  Schadens- und Trefferrechnung, Fangsystem, Kampf-Engine (End-to-End über
  mehrere Runden), Zucht, Entwicklung, Fortschritt, Inventar und Quests ab.
- Die Typentabelle wird gegen **Balance-Invarianten** getestet: jedes Element hat
  2–4 Stärken und 2–4 Schwächen, keines ist gegen sich selbst effektiv, die
  Streuung der Offensivwerte bleibt begrenzt.
- Der Inhalts-Validator prüft referentielle Integrität, Wertebereiche,
  Erreichbarkeit aller Orte und Vollständigkeit beider Sprachen.

---

## Rechtliches

Alle Namen, Beschreibungen, Kreaturen, Orte und Texte sind eigenständige
Schöpfungen. Die nordische Mythologie selbst ist gemeinfrei; es wurden keinerlei
geschützte Namen, Designs oder Inhalte bestehender Marken übernommen.

Audio- und Bilddateien sind nicht Teil dieses Repositorys. Das Spiel adressiert
sie über Schlüssel (`bgm_battle_boss`, `spr_glutwelp`) und läuft auch dann, wenn
eine Datei fehlt — siehe [docs/ASSETS.md](docs/ASSETS.md).
