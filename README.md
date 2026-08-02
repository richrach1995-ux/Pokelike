# Pokelike – Die Monster von Auronia

Ein komplettes Monster-Sammel-Rollenspiel für Android im Stil der klassischen
Game-Boy-Spiele: Overworld erkunden, wilde Monster fangen, Kämpfe bestreiten,
Arenen besiegen und die Story um den Schattenorden abschließen.

Alles ist in Kotlin geschrieben und rendert auf eine `SurfaceView` – es gibt
**keine externen Bibliotheken und keine Bilddateien**: sämtliche Sprites, Kacheln
und Menüs werden zur Laufzeit aus Pixel-Vorlagen im Code erzeugt.

---

## APK herunterladen

Jeder Push baut die App automatisch über GitHub Actions.

1. Im Repository auf **Actions** → Workflow **„Android APK bauen"** → letzten Lauf öffnen
2. Unten unter **Artifacts** das Paket **`Pokelike-APK`** herunterladen
3. Enthält `Pokelike-debug.apk` und `Pokelike-release.apk`

Die Release-APK ist mit dem Debug-Schlüssel signiert und damit direkt
installierbar (auf dem Handy „Installation aus unbekannten Quellen" erlauben).

**Ohne GitHub-Login herunterladen:** Für Artifacts muss man bei GitHub angemeldet
sein. Wer einen offenen Download-Link möchte, setzt einfach einen Versions-Tag –
der Workflow hängt die APKs dann automatisch an ein GitHub-Release:

```bash
git tag v1.0 && git push origin v1.0
```

Danach liegt die APK unter **Releases** im Repository.

### Selbst bauen

```bash
./gradlew assembleRelease
# Ergebnis: app/build/outputs/apk/release/app-release.apk
```

Voraussetzung: JDK 17 und das Android SDK (compileSdk 35). Minimum ist Android 7.0
(API 24).

---

## Steuerung

| Element | Funktion |
|---|---|
| Steuerkreuz | Laufen, Menüauswahl |
| **A** | Bestätigen, reden, untersuchen, angreifen |
| **B** | Abbrechen, zurück (auch die Zurück-Taste des Handys) |
| **MENÜ** | Hauptmenü (Team, Beutel, Dex, Typen-Info, Box, Trainerkarte, Speichern) |

---

## Spielsysteme

### Typen
Zwölf Typen – **Normal, Feuer, Wasser, Pflanze, Elektro, Eis, Kampf, Gift,
Gestein, Käfer, Geist, Drache** – mit vollständiger Effektivitätstabelle
inklusive Immunitäten (z. B. Elektro wirkt nicht gegen Gestein, Normal nicht
gegen Geist). Monster können ein oder zwei Typen haben, die Effektivitäten
multiplizieren sich (bis ×4 bzw. ×0,25).

Attacken besitzen ebenfalls einen Typ. Setzt ein Monster eine Attacke seines
eigenen Typs ein, gibt es **+50 % Schaden (Typ-Bonus/STAB)**.

Im Spiel gibt es unter **MENÜ → TYPEN-INFO** eine vollständige Übersicht aller
Stärken und Schwächen.

### Kampfsystem
* Schadensformel nach Vorbild der Klassiker: Level, Angriff/Verteidigung
  (physisch bzw. speziell), Stärke, Typ-Bonus, Typ-Effektivität, Volltreffer
  (×1,5; manche Attacken mit erhöhter Quote) und Zufallsstreuung
* Sechs Werte: KP, Angriff, Verteidigung, Sp-Angriff, Sp-Verteidigung, Initiative
* Statusveränderungen: **Gift, Verbrennung, Paralyse, Schlaf, Frost, Verwirrung**
  (mit Typ-Immunitäten – Feuer-Monster verbrennen z. B. nicht)
* Statuswert-Stufen von −6 bis +6 durch Attacken und Items
* Attacken-Prioritäten, Mehrfachtreffer, Rückstoß, Energieabsorption, AP-Verbrauch
* Wechseln, Fliehen (mit Initiative-Formel), Items im Kampf
* Gegner-KI: wilde Monster kämpfen sprunghaft, Trainer wählen die beste Attacke,
  **Bosse** spielen deutlich klüger, setzen Statusattacken gezielt ein und
  benutzen Heilitems

### Level & Entwicklung
* Erfahrung nach besiegtem Gegner (Trainer geben mehr), das aktive Monster
  bekommt den vollen Anteil, der Rest des Teams einen Teil
* Level 1–100, neue Attacken beim Levelaufstieg (mit Abfrage, welche Attacke
  ersetzt wird, wenn schon vier gelernt sind)
* **Entwicklungen** per Level (z. B. Flamki → Flammor → Infernox) und per
  **Entwicklungsstein** (Feuer-, Wasser-, Blatt-, Donnerstein)

### Fangen
Fangformel mit Rest-KP, Fangrate der Art, Ballstärke und Statusbonus
(Schlaf/Frost ×2,5, Gift/Paralyse/Verbrennung ×1,5) – inklusive Wackel-Animation
des Balls. Volle Teams schicken neue Monster in die **Box**.

### Heilstationen
In jeder Stadt gibt es eine **Heilstation** (Gerät mit blinkendem Licht). Sie
heilt das gesamte Team kostenlos und wird zum Rückkehrpunkt: Wer alle Monster
verliert, wacht dort wieder auf. Zuhause heilt zusätzlich Mama.

### Items
* **Bälle:** Fangball, Superball, Hyperball, Meisterball
* **Heilung:** Trank/Supertrank/Hypertrank/Top-Trank, Beleber, Top-Beleber,
  Gegengift, Brandsalbe, Eisspray, Aufwecker, Para-Heiler, Allheiler, Elixier (AP)
* **Kampf:** X-Angriff, X-Abwehr, X-Spezial, X-Tempo
* **Sonstiges:** Sonderbonbon (Level +1), Vitamine (KP-/Kraft-/Panzer-/Geist-/
  Nerven-/Tempo-Plus erhöhen Werte dauerhaft), Entwicklungssteine, Schutz und
  Superschutz (halten wilde Monster fern), Fluchtseil
* **Basis-Items:** Grubenlampe (Höhle), Angel (Wasser-Monster angeln), Gipfelpass

### Welt & Story
24 Karten: Dorf Ahornfels, fünf Routen, die dunkle Kristallhöhle (nur mit Lampe),
Nebelwald, Schattental, Drachengipfel, sechs Städte mit Heilstation und
Marktstand, fünf Arenen, der Hort des Schattenordens und die Liga-Halle.

Story: Prof. Eibe schickt dich mit einem Starter los. Fünf Arenameister
(**Käfer, Wasser, Feuer, Eis, Elektro**) geben je ein Siegel. Der **Schattenorden**
will das legendäre **Titanox** erwecken – nach dem Sieg über Ordensmeisterin
**Morgana** öffnet sich der Weg zur Liga und zum **Champion Drakon**. Danach
wartet Titanox auf dem Drachengipfel.

Dazu: Trainer mit Sichtlinie, versteckte Items, Eisflächen zum Rutschen,
Angelstellen, 45 Monsterarten mit Monsterdex, Speicherfunktion und Trainerkarte.

---

## Tests

Bei jedem Build laufen automatisch neun Tests mit (`./gradlew testDebugUnitTest`):

* Monster- und Attackendaten (Lernsets, Entwicklungen, Sprites, Fangraten)
* Typentabelle inklusive Doppeltyp-Multiplikation und Typ-Bonus
* alle Karten: Zeilenlängen, Warps, NPC-Positionen, Begegnungstabellen, Läden
* **600 zufällig durchgespielte Kämpfe** – prüft auf Abstürze, ungültige Zustände
  und Kämpfe, die nie enden
* Status-Immunitäten, Erfahrung und Entwicklung
* **Durchspielbarkeit**: Eine Flutfüllung über alle Karten, Warps, blockierende
  NPCs und Story-Flags weist nach, dass man vom Startdorf über alle fünf Siegel
  und den Schattenorden bis zum Champion und zu Titanox kommt – und dass jedes
  Bodenitem erreichbar ist.

## Projektaufbau

```
app/src/main/java/com/pokelike/game/
├── Types.kt        Typen und Effektivitätstabelle
├── Moves.kt        ~79 Attacken mit Effekten
├── Species.kt      45 Monsterarten, Basiswerte, Lernsets, Entwicklungen
├── Monster.kt      Monster-Instanz: Werte, Erfahrung, Level, Speichern
├── Items.kt        Items und Beutel
├── Battle.kt       Kampf-Engine (Ereignisse, Schaden, Status, KI, Fangen)
├── World.kt        Kacheln, Karten, NPCs, Trainer-Definitionen
├── MapData.kt      Alle Karten der Region Auronia
├── GameState.kt    Spielstand inkl. JSON-Speicherung
├── Sprites.kt      Pixel-Vorlagen und Bitmap-Erzeugung
├── Render.kt       Zeichenhilfen für Kacheln, Figuren, Oberfläche
├── Scenes.kt       Szenen-System, Textfenster, Titel, Oberwelt
├── BattleScene.kt  Kampfbildschirm
├── MenuScenes.kt   Team, Beutel, Laden, Dex, Typen-Info, Box, Trainerkarte
├── GameView.kt     Render-Schleife und Touch-Steuerung
└── MainActivity.kt Einstiegspunkt
```
