# Sprites und Animationen

Runeveil zeigt 255 Monsterarten, ohne eine einzige Monster-Bilddatei
auszuliefern. Jede Art wird zur Laufzeit aus einem **Bauplan** gezeichnet, der
deterministisch aus der Art selbst abgeleitet ist.

## Warum nicht gezeichnete Bilder?

Ein Bild je Art hieße 255 Dateien mal Schimmer-Variante mal Auflösungsstufe. Ohne
Illustratorin gäbe es dafür nur zwei ehrliche Möglichkeiten: 255 Platzhalter, für
die der Spieler Downloadvolumen bezahlt, oder gar nichts. Der Bauplan ist die
dritte: jede Art bekommt ein eigenes, wiedererkennbares Aussehen, das aus ihren
Spielwerten folgt — und ein Nachzeichnen von Hand bleibt jederzeit möglich, weil
der Renderer hinter einer Schnittstelle liegt.

## Aufbau

```
domain/…/model/sprite/SpriteBlueprint.kt    was gezeichnet wird (reines Kotlin)
domain/…/model/sprite/SpriteBlueprints.kt   wie es aus der Art abgeleitet wird
app/…/ui/sprite/MonsterSprite.kt            wie es auf die Leinwand kommt
app/…/ui/sprite/SpriteAnimation.kt          wie es sich bewegt
```

Die Entscheidungen über Form und Farbe fallen vollständig in `:domain` und sind
dort mit 12 Tests festgenagelt. Der Renderer im App-Modul weiß nur noch, wie man
das beschriebene Wesen zeichnet — er trifft keine Entscheidungen.

## Determinismus

Zwei Startwerte, beide aus stabilen Bezeichnern:

| Startwert | steuert | Wirkung |
|---|---|---|
| `familyId` | Körperbau, Schweif, Kopfschmuck, Flügel, Zeichnung, Augenzahl | Eine Entwicklungslinie sieht sich ähnlich |
| `species.id` | Proportionen | Stufe 3 ist sichtbar wuchtiger als Stufe 1 |
| `species.id + "#palette"` | Farben | Getrennter Strom, damit ein Schimmer-Exemplar dieselbe Gestalt behält |

Als Zufallsquelle dient **SplitMix64**, nicht `kotlin.random.Random`: dessen
Algorithmus ist ein Implementierungsdetail, das sich zwischen Kotlin-Versionen
ändern darf. Ein Monster darf nicht anders aussehen, weil eine Abhängigkeit
aktualisiert wurde. `String.hashCode` ist von der Java-Sprachspezifikation
festgelegt, damit ist die ganze Kette vom Artennamen bis zur Silhouette gepinnt.

## Was die Werte bestimmen

| Spielwert | Wirkung im Bild |
|---|---|
| Primärelement | Körperbau (zwei Kandidaten je Element) und Grundfarbe |
| Sekundärelement | Akzentfarbe an Hörnern, Flossen, Zeichnung |
| Stufe | Masse, Größe des Kopfschmucks, Arme ab Stufe 2–3 |
| Größenklasse | Gesamtmaßstab von 0,66 (winzig) bis 1,38 (kolossal) |
| Seltenheit | Aura ab *Legendär*; Runenzeichnung ab *Episch* |
| Schimmer | Farbkreis-Drehung um rund 150° — auffällig, nicht nur wärmer |

Acht Körperbaupläne: `BEAST`, `DRAKE`, `SERPENT`, `AVIAN`, `AQUATIC`,
`INSECTOID`, `GOLEM`, `WISP`.

## Animation

`SpriteMotion` beschreibt, was über der Silhouette liegt: Versatz, Maßstab,
Stauchung, Drehung, Deckkraft, Farbblitz. Alle Werte sind Bruchteile der eigenen
Zeichenfläche, nie Pixel — dieselbe Animation liest sich auf Telefon und Tablet
gleich.

| Pose | Bewegung | Ausgelöst durch |
|---|---|---|
| `IDLE` | Atmen, leichte Stauchung | Ruhezustand |
| `ATTACK` | Ausfallschritt nach vorn und zurück | `MoveDeclared` |
| `HURT` | Rückstoß, Zittern, weißer Blitz | `DamageDealt` |
| `FAINT` | Kippen, Absacken, Ausblenden | `Fainted` |
| `ENTER` | Aufskalieren | `SwitchedIn` |
| `CAPTURE` | Schrumpfen in die Kugel, goldener Blitz | `CaptureSucceeded` |

Die Posen werden **aus den Ereignissen der Kampf-Engine** gesetzt, nicht aus der
Oberfläche heraus erfunden. Was auf dem Bildschirm passiert, ist damit exakt das,
was in der Runde tatsächlich passiert ist.

`FAINT` und `CAPTURE` sind Zustände, keine Gesten: sie kehren nicht ins Atmen
zurück.

Dazu: aufsteigende Schadenszahlen (eingefärbt nach Effektivität), ein
Bildschirmruckeln bei Volltreffern, Bodenlichter unter beiden Wesen.

## Barrierefreiheit und Leistung

* **Weniger Bewegung** (Einstellungen) hält alles still; Posen lösen sofort aus,
  damit der Kampfablauf nicht auf abgeschaltete Animationen wartet.
* **Bildschirmruckeln** lässt sich getrennt abschalten.
* **Kampftempo** skaliert jede Dauer.
* Die Animation wird *innerhalb* des Zeichenbereichs gelesen (`motion` ist eine
  Lambda, kein Wert). Sechzig Bilder pro Sekunde zeichnen damit neu, ohne den
  Kampfbildschirm neu zu komponieren.
* Ein Sprite kostet einige Dutzend Zeichenaufrufe und keinerlei Dekodierung —
  deshalb kann das Bestiarium ein ganzes Raster davon zeigen.

## Weiterentwicklung

Der Renderer liegt hinter `MonsterSprite`/`MonsterSpriteStatic`. Sobald echte
Illustrationen vorliegen, lassen sie sich dort einhängen, ohne dass Kampf-,
Team- oder Bestiarium-Bildschirm etwas davon merken.
