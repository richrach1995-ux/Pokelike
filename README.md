# Pokelike

Ein Idle-/Clicker-Spiel für Android, gebaut mit Kotlin, Jetpack Compose und
Material Design 3.

Dieses Repository befindet sich im Aufbau. Der aktuelle Stand entspricht
**Schritt 1: Projekt-Grundgerüst**.

---

## Technischer Stack

| Bereich | Technologie |
|---|---|
| Sprache | Kotlin 2.0.21 |
| UI | Jetpack Compose, Material 3 |
| Architektur | MVVM + Clean Architecture |
| DI | Hilt |
| Persistenz | Room, DataStore |
| Nebenläufigkeit | Coroutines, StateFlow |
| Navigation | Navigation Compose |
| Build | AGP 8.7.3, Gradle 8.14.3, KSP |

Alle Versionen werden zentral in [`gradle/libs.versions.toml`](gradle/libs.versions.toml)
gepflegt. Kein Modul notiert Versionen direkt in seiner `build.gradle.kts`.

---

## Bauen

```bash
./gradlew assembleDebug     # Debug-APK
./gradlew testDebugUnitTest # Unit-Tests
./gradlew lintDebug         # Lint
```

**Voraussetzung:** Ein installiertes Android SDK mit Plattform 35. Android
Studio legt `local.properties` mit dem SDK-Pfad automatisch an; auf einem
CI-Runner setzt man stattdessen `ANDROID_HOME`.

Die Datei `local.properties` ist bewusst nicht eingecheckt — sie enthält einen
maschinenspezifischen Pfad.

---

## Paketstruktur

```
com.pokelike.idle
├── config/     Balancing- und Laufzeitkonstanten (GameConfig)
├── data/
│   ├── database/    Room-Entities, DAO, Mapper
│   ├── preferences/ DataStore für Einstellungen
│   ├── repository/  Umsetzung der Domänen-Schnittstellen
│   └── security/    Signatur des Spielstands
├── di/         Hilt-Module und Qualifier
├── domain/
│   ├── model/      Zahlentyp, Ressourcen, Spielstand
│   ├── repository/ Schnittstellen
│   └── usecases/   Offline-Fortschritt
├── manager/    Prozessweite Dienste (GameClock, Autosave, Sitzung)
├── ui/
│   ├── navigation/  Zielregistry, NavHost, untere Leiste
│   ├── screens/     Bildschirme, je Feature ein Unterpaket
│   └── theme/       Farben, Typografie, Abstände
└── util/       Querschnittswerkzeuge (Zeit, Dispatcher, Formatierung)
```

Weitere Pakete (`data/`, `domain/`, `billing/`, `ads/`, `analytics/`,
`events/`, `quests/`, `achievements/`, `prestige/`) kommen in den folgenden
Schritten hinzu, sobald das jeweilige System implementiert wird — jeweils mit
echtem Inhalt statt als leeres Gerüst.

---

## Architekturentscheidungen

**Eine Activity.** Navigation Compose verwaltet den Backstack innerhalb einer
Activity. Mehrere Activities würden einen zweiten, unabhängigen Stack einführen,
was spätestens beim Zurückkehren aus Kauf- oder Werbe-Flows zu schwer
nachvollziehbarem Verhalten führt.

**Eine Taktquelle.** `GameClock` ist die einzige Zeitbasis der Engine.
Idle-Einkommen, Booster-Laufzeiten, Event-Spawns, Quests und Autosave beziehen
sich alle darauf. Getrennte Timer würden auseinanderlaufen, sobald das System
einen davon drosselt.

**Monotone Zeit für Spielmechanik.** `TimeSource` trennt `elapsedRealtime()`
(monoton, nicht verstellbar) von `wallClock()` (Systemzeit). Jede Mechanik nutzt
die erste — sonst genügte ein Vorstellen der Geräteuhr, um beliebig viel
Offline-Ertrag zu erzeugen. Die Systemzeit wird nur dort gebraucht, wo echte
Kalendertage zählen (Login-Bonus, Tagesquests), und dort später gegen einen
Serverzeitstempel geprüft.

**Prozess-Lifecycle statt Activity-Lifecycle.** Die Uhr hängt an
`ProcessLifecycleOwner`. Am Activity-Lifecycle würde sie bei jeder
Bildschirmdrehung anhalten und neu starten.

**Eigener Zahlentyp.** `BigNumber` stellt Werte als `mantisse * 10^exponent`
dar. `Double` bricht bei ~1e308 — mit Prestige-Multiplikatoren wird das
regelmäßig erreicht, und `1.15^n` (Gebäudepreis) liefert ab etwa 5.300
Gebäuden nur noch `Infinity`. `BigDecimal` wäre exakt, aber für zehn
Berechnungen pro Sekunde über dutzende Gebäude zu langsam.

Die Mantisse wird bei jeder Operation auf 14 signifikante Stellen gerundet.
Ohne diesen Schritt ergibt `100 - 30` in Fließkommaarithmetik `6.999999999999999e1`
statt `7e1` — im Spiel bliebe der Kauf-Button dann genau dann grau, wenn der
Spieler gerade genug gespart hat.

**Ressourcen ohne Sonderfälle.** Jede Währung ist ein Eintrag in
`ResourceType`. Ob sie den Prestige-Reset überlebt und ob sie serverseitig
geprüft werden muss, steht als Eigenschaft am Eintrag — nicht als `if` in der
Reset-Logik. Eine neue Ressource ist damit eine Zeile, und sie verhält sich
beim Prestige automatisch richtig.

**Beträge können nicht negativ werden.** `ResourceBundle` und `ResourcePool`
weisen negative Werte ab, und Abbuchungen laufen ausschließlich über
`spend()`, das Deckungsprüfung und Abbuchung in einer Operation vereint. Ein
Preis mit negativem Betrag würde beim Bezahlen sonst Guthaben gutschreiben.

**Spielstand im Arbeitsspeicher, Sicherung in Abständen.** `GameRepository`
führt den Zustand als `StateFlow` und schreibt alle 30 Sekunden sowie beim
Wechsel in den Hintergrund. Bei zehn Änderungen pro Sekunde wäre ein
Schreibvorgang je Änderung sinnlos.

**Ressourcen als Zeilen, nicht als Spalten.** Eine neue Währung ist dadurch
kein Datenbankschema-Wechsel. `DATABASE_VERSION` beschreibt den Tabellenaufbau,
`GameState.CURRENT_SCHEMA_VERSION` den Inhalt — der häufigere Fall kommt ohne
Room-Migration aus.

**Kein `fallbackToDestructiveMigration`.** Das würde bei einer vergessenen
Migration alle Spielstände löschen — erst nach dem Rollout und ohne Weg zurück.
Ein Absturz beim Start fällt in der Entwicklung sofort auf, stiller
Datenverlust erst in den Rezensionen.

**Signatur über beide Tabellen.** `SaveSignature` bildet ein HMAC-SHA256 über
skalare Felder *und* Ressourcenzeilen. Deckte sie nur die skalaren Felder ab,
ließe sich der Münzstand direkt in der Ressourcentabelle hochsetzen. Das stoppt
das Bearbeiten der Datenbankdatei, nicht das Manipulieren des laufenden
Prozesses — der Schlüssel steckt im Programm. Belastbaren Schutz gibt es nur
serverseitig; die Architektur ist darauf vorbereitet.

**Offline-Zeit nur über die Systemuhr, mit Prüfung.** Die monotone Uhr steht
bei beendetem Prozess nicht zur Verfügung. Liegt der gespeicherte Zeitpunkt
mehr als fünf Minuten in der Zukunft, gilt die Uhr als manipuliert und es wird
nichts gutgeschrieben. Zusätzlich ein Deckel von acht Stunden.

**Kein Dynamic Color.** Bei einem Spiel trägt Farbe Information: Gold bedeutet
Münzen, Violett bedeutet Event-Token. Eine vom Systemhintergrund abgeleitete
Palette würde diese Zuordnung zerstören.

**Spielstand nicht im Auto-Backup.** Ein Geräte-Backup lässt sich vom Nutzer
wiederherstellen und wäre ein trivialer Weg, Fortschritt und Premiumwährung zu
duplizieren. Gesichert werden nur Geräteeinstellungen; für Spielstände ist der
Cloud-Save-Pfad mit serverseitiger Autorität vorgesehen.

---

## Tests

Unit-Tests liegen unter `app/src/test/`. Der Engine-Kern ist vollständig ohne
Android-Framework testbar: `TimeSource` ist ein Interface ohne Android-Typen,
und `DispatcherProvider` erlaubt es, sämtliche Nebenläufigkeit auf einen
Test-Dispatcher zu legen.

`VirtualTimeSource` koppelt die Zeitquelle an die virtuelle Uhr des
Test-Schedulers. Dadurch lassen sich Stunden Spielzeit in Millisekunden
Testlaufzeit prüfen — nötig für Offline-Progress und Booster-Ablauf.
