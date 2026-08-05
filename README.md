# Pokelike

Ein Idle-/Clicker-Spiel für Android, gebaut mit Kotlin, Jetpack Compose und
Material Design 3.

Dieses Repository befindet sich im Aufbau. Fertiggestellt sind:

1. Projekt-Grundgerüst (Build, DI, Theme, Navigation, Spiel-Uhr)
2. Zahlentyp, Ressourcensystem, Spielstand
3. Persistenz, Autosave, Offline-Fortschritt
4. Klick-System mit Combo und kritischen Treffern
5. Gebäude, Idle-Einkommen, Offline-Fortschritt
6. Upgrades mit Freischaltbedingungen
7. Prestige mit Wurzelformel und dauerhaftem Bonus
8. Achievements, Quests und eine zentrale Belohnungsmeldung
9. Täglicher Bonus mit Serie und kaufbarem Serienschutz
10. Booster mit Echtzeit-Laufzeit und Booster-Angebot
11. Belohnungsvideos: Schnittstelle, Nachbildung, Wartezeit

Als Nächstes: die AdMob-Anbindung hinter der bestehenden Schnittstelle —
sobald das Projekt einmal in Android Studio gebaut wurde.

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
├── ads/        Grenze zum Werbe-SDK: Schnittstelle und Nachbildung
├── config/     Balancing- und Laufzeitkonstanten (GameConfig)
├── data/
│   ├── database/    Room-Entities, DAO, Mapper
│   ├── preferences/ DataStore für Einstellungen
│   ├── repository/  Umsetzung der Domänen-Schnittstellen
│   └── security/    Signatur des Spielstands
├── di/         Hilt-Module und Qualifier
├── domain/
│   ├── model/      Zahlentyp, Ressourcen, Gebäude, Upgrades, Ziele,
│   │                Tagesbonus, Booster, Werbestellen, Spielstand
│   ├── repository/ Schnittstellen
│   └── usecases/   Klick, Einkommen, Käufe, Modifikatoren, Offline, Ziele,
│                    Tagesbonus, Booster, Werbebelohnung
├── manager/    Prozessweite Dienste (Uhr, Autosave, Klicks, Einkommen,
│            Modifikatoren, Achievements, Belohnungen, Tagesbonus, Booster,
│            Belohnungsvideos, Sitzung)
├── ui/
│   ├── components/  Klick-Button, schwebender Text, Combo-Anzeige,
│   │                Belohnungs- und Tagesbonusdialog
│   ├── navigation/  Zielregistry, NavHost, untere Leiste
│   ├── screens/     Bildschirme, je Feature ein Unterpaket
│   └── theme/       Farben, Typografie, Abstände
└── util/       Querschnittswerkzeuge (Zeit, Dispatcher, Formatierung)
```

Weitere Pakete (`billing/`, `analytics/`, `events/`) kommen in den folgenden
Schritten hinzu, sobald das jeweilige System implementiert wird — jeweils mit
echtem Inhalt statt als leeres Gerüst.

Prestige, Quests und Achievements haben bewusst **kein** eigenes Top-Level-Paket
bekommen. Sie bestehen aus einem deklarativen Katalog (`domain/model/`), Regeln
(`domain/usecases/`) und Anzeige (`ui/screens/`) — dieselbe Dreiteilung wie
Gebäude und Upgrades. Ein Paket je Spielsystem würde diese Schichtung
durchschneiden und wäre der erste Schritt zu Feature-Silos, die jeweils ihre
eigene Persistenz und ihre eigenen Regeln mitbringen.

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

**Zufall als injizierte Schnittstelle.** `RandomProvider` statt direkter
`Random`-Aufrufe. Ein Test gibt damit gezielt den Wert vor, der die
Kritschwelle trifft, statt über tausende Durchläufe zu schätzen und
gelegentlich ohne Fehler im Code fehlzuschlagen. `rollChance` vergleicht mit
`<`; bei Wahrscheinlichkeit 0 oder 1 wird gar nicht erst gewürfelt, damit
Sonderfälle die Zufallsfolge nicht verschieben.

**Reihenfolge der Klickberechnung ist Balancing.**
`(Grundwert + flacher Zuschlag) × Faktor × Combo × Kritfaktor` — erst
addieren, dann multiplizieren. Andersherum wären flache Zuschläge im späten
Spiel wertlos, und ein Upgrade ohne Wirkung ist schlimmer als gar keines.

**Combo im prozessweiten Dienst, nicht im ViewModel.** Ein ViewModel wird beim
Bildschirmwechsel verworfen — die Combo würde beim Blick in den Shop verfallen.
`ClickManager` hält sie; sie endet nur, wenn das Zeitfenster tatsächlich
abläuft oder die App in den Hintergrund geht.

**Klick-Rückmeldung ohne Ripple.** Die Standardanimation von Material ist auf
einzelne Betätigungen ausgelegt und überlagert sich bei mehreren Klicks pro
Sekunde zu einem Flimmern. Stattdessen staucht sich der Button per Feder —
jederzeit abfangbar, kein Klick wirkt verschluckt. Skalierung und schwebende
Texte laufen über `graphicsLayer`, also ohne erneutes Layout oder Recompose.

**Vibration nur bei kritischen Treffern.** Bei jedem Klick zu vibrieren würde
bei schnellem Tippen zu einem Dauerbrummen verschmelzen, das den Akku belastet
und den besonderen Moment entwertet.

**Gebäude als Zeilen, mit echter Migration.** Eine neue Tabelle *ist* eine
Schemaänderung — `DATABASE_VERSION` steht auf 2, `Migration(1,2)` legt die
Tabelle an. Bestehende Spielstände bleiben unverändert und starten mit leerem
Bestand.

**Signatur-Format ist anhängend.** Gebäude werden an die kanonische Zeichenkette
*angehängt*, nie eingefügt. Ein Spielstand ohne Gebäude ergibt dadurch exakt
dieselbe Prüfsumme wie vor Einführung des Abschnitts — alte Stände behalten
ihre Gültigkeit. Diese Regel gilt für jede künftige Erweiterung.

**Geschlossene Preisformel statt Schleife.** Ein Sammelkauf von zehntausend
Exemplaren wäre sonst zehntausend `BigNumber`-Multiplikationen während eines
Frames. Die größtmögliche Kaufmenge wird über den Logarithmus bestimmt und
anschließend gegengeprüft — der Logarithmus rechnet mit `Double`-Genauigkeit
und kann um ein Exemplar danebenliegen.

**Eine Einkommensformel für drei Verwendungen.** Laufende Gutschrift, Anzeige
und Offline-Berechnung nutzen dasselbe `CalculateIncomeUseCase`. Wäre die
Formel mehrfach vorhanden, liefen die Werte auseinander — und genau das bemerkt
ein Spieler sofort.

**Idle-Einkommen am Uhrentakt.** Ein eigener Zeitgeber würde im Hintergrund
weiterlaufen und dort Ertrag erzeugen, den die Offline-Berechnung beim nächsten
Start ein zweites Mal gutschreibt.

**Gebäude erscheinen ab dem halben Preis.** Der Spieler sieht sein nächstes
Ziel, bevor er es sich leisten kann. Maßgeblich ist die Lebenszeitsumme, nicht
der Kontostand — sonst verschwände ein Gebäude wieder, sobald er sein Geld
ausgibt.

**Freischaltbedingungen als `sealed interface`.** Jede Bedingung trägt genau
die Angaben, die sie braucht, und prüft sich selbst gegen den Spielstand. Ein
Enum mit Zahlenfeldern müsste alle denkbaren Felder führen, von denen bei jeder
Bedingung die meisten bedeutungslos wären. Eine neue Bedingungsart ist eine neue
Klasse — bestehende bleiben unberührt.

**`GameModifiers` als einzige Zwischenschicht.** Upgrades, Booster — und später
Events, Prestige-Boni, Skins — verändern ausschließlich dieses Objekt. Klick,
Einkommen, Preise und Offline lesen daraus und kennen keine einzige Quelle.
Ohne die Schicht wäre eine neue Quelle eine Änderung an vier Stellen.

**Verrechnungsart ist Balancing.** Faktoren werden multipliziert (zweimal
„doppelt" ergibt vierfach — additiv wären späte Upgrades wirkungslos),
Zuschläge addiert (5 % + 5 % Kritchance = 10 %, so liest es der Spieler),
Nachlässe addiert und gedeckelt (multiplikativ ergäben 5 % + 10 % nur 14,5 %
und wirkten wie ein Rechenfehler).

**Nur Wirkungsarten mit Auswertungsstelle.** `UpgradeEffect` enthält keine
Wirkung, die nirgends ankommt — ein Upgrade, das der Spieler kauft und das
nichts tut, ist schlimmer als gar keines. Ein Test prüft, dass jede der acht
Arten mindestens einmal vorkommt.

**Upgrades überleben den Prestige-Reset.** Gebäude fallen weg, Upgrades
bleiben. Sie sind der Grund, warum ein Neuanfang schneller läuft als der vorige
Durchlauf — ohne sie wäre Prestige eine reine Bestrafung.

**Prestige-Punkte über die Wurzelformel.** `punkte = ⌊√(lebenssumme / basis)⌋`.
Der zehnfache Ertrag bringt nur etwa dreifach so viele Punkte. Ohne diese
Dämpfung wäre ein einziger sehr langer Durchlauf jedem regelmäßigen Spielen
überlegen — genau das Gegenteil dessen, was Prestige belohnen soll.

**Prestige-Bonus additiv, nicht multiplikativ.** Hundert Punkte ergeben den
dreifachen Ertrag, nicht das Zweihoch-Hundertfache. Multiplikativ wären die
Zahlen nach wenigen Durchläufen so groß, dass Gebäudepreise, Upgrade-Kosten
und Angebotsgestaltung keinen Einfluss auf den Spielverlauf mehr hätten.

**Offene Punkte über die Lebenssumme, nicht den Kontostand.** Abgezogen wird
`lifetimeEarned[PRESTIGE_POINTS]`. Sobald sich Punkte ausgeben lassen, würde
der Kontostand sinken — und der Spieler bekäme dieselben Punkte ein zweites
Mal. Ein Test sichert ab, dass ein zweiter Reset ohne neuen Fortschritt nichts
einbringt.

**Fortschrittsbalken logarithmisch.** Zwischen zwei Punktschwellen liegen im
späten Spiel Größenordnungen; ein linearer Balken stünde dort über Stunden bei
nahezu null und trüge keine Information mehr.

**Reset nur über Bestätigungsdialog, der den Verlust benennt.** Ein Dialog, der
nur den Gewinn nennt, wäre im Ergebnis eine Falle.

**Quest-Fortschritt als Differenz zu einem Ausgangswert.** Eine Tagesquest
speichert keinen eigenen Zähler, sondern nur den Stand der Messgröße zu Beginn
des Zeitraums; der Fortschritt ist die Differenz zum aktuellen Stand.
Zurücksetzen heißt damit: Ausgangswert neu setzen. Bei eigenen Zählern je Quest
müsste jede neue Quest eine neue Spalte oder Zeile bekommen und beim
Zurücksetzen einzeln angefasst werden — und jeder vergessene Zähler wäre eine
Quest, die sich nie wieder erledigen lässt.

**Zeiträume nach Kalendertag, nicht nach 24-Stunden-Fenster.** Der Wechsel wird
über `java.time` in der Zeitzone des Geräts bestimmt. Ein rollendes Fenster
würde den Zeitpunkt, zu dem neue Quests erscheinen, mit jedem Tag nach hinten
schieben. Eine zurückgestellte Uhr löst kein Zurücksetzen aus: Der Zeitraum
beginnt nur neu, wenn der aktuelle Kalendertag *nach* dem gespeicherten liegt.

**Belohnungen werden sofort gutgeschrieben, gemeldet wird getrennt.** Wären
Gutschrift und Meldung ein gemeinsamer Schritt, ginge die Belohnung verloren,
sobald die App zwischen Freischaltung und Dialog beendet wird. `RewardManager`
ist deshalb eine reine Benachrichtigungsschlange; Achievements, Quests und
später Events und Battle Pass melden dort an und teilen sich einen Dialog.

Der tägliche Bonus ist bewusst die Ausnahme: Er wird nicht gutgeschrieben,
sondern abgeholt, und braucht dafür seinen eigenen Dialog mit der Zyklusleiste.
Ihn zusätzlich als Meldung anzumelden hieße, dem Spieler unmittelbar nach dem
Abholen ein zweites Popup mit derselben Zahl zu zeigen.

**Ein Bildschirm für Quests und Achievements.** Beides sind Ziele mit
Belohnung, und zwei Einträge in der unteren Leiste hätten sie auf sechs
gebracht — Material Design empfiehlt drei bis fünf.

**Alle Zieltabellen liegen unter der Prüfsumme.** Achievements zahlen Diamanten
aus, Abholvermerke verhindern doppelte Quest-Belohnungen, und ein kleinerer
Ausgangswert bedeutet mehr Fortschritt. Jede dieser Tabellen wäre ohne
Signatur ein direkter Weg, sich Premiumwährung einzutragen. Die Abschnitte
werden dabei *angehängt*, nie eingefügt: Ein Spielstand aus Version 3 ergibt
weiterhin dieselbe Prüfsumme.

**Der Münzanteil des Tagesbonus wird in Minuten angegeben, nicht als Betrag.**
Ein fester Münzbetrag wäre nach wenigen Stunden weniger als eine Sekunde
Einkommen. Angegeben ist deshalb eine Zeitspanne, und der Betrag ergibt sich aus
dem Einkommen des Spielers — der Bonus behält damit über die gesamte Spieldauer
denselben gefühlten Wert. Untergrenze ist der erwartete Ertrag eines Klicks pro
Sekunde, damit ein Spieler ohne Gebäude nicht null Münzen bekommt.
Diamantenzuschläge bleiben dagegen fest: Ihr Wert hängt an den Preisen im
Angebot, nicht am Fortschritt.

**Die Serie zählt Kalendertage, der Zyklus läuft endlos.** Nach dem siebten Tag
beginnt der Zyklus wieder bei eins, die Serie zählt weiter. Ein Zyklusende, das
die Serie beendet, würde den Spieler für sein Durchhalten bestrafen. Achievements
hängen deshalb am Bestwert und nicht an der laufenden Serie — eine später
gerissene Serie darf ein erreichtes Achievement nicht wieder entziehen.

**Serienschutz ist ein Verbrauchsgut, kein Dauerschutz.** Ein einmal gekaufter
Dauerschutz wäre ein einziger Kauf, danach nie wieder — und die Serie hätte ihre
Bedeutung verloren, weil sie nicht mehr reißen kann. Der Preis entspricht genau
der Diamantensumme eines vollständigen Zyklus: Eine lückenlose Woche trägt genau
eine Ladung. Ein Test sichert dieses Gleichgewicht ab, damit eine
Balancing-Änderung an einer der beiden Stellen auffällt.

**Der Tagesbonus wird nicht automatisch gutgeschrieben.** Anders als
Achievements: Das Abholen ist ein bewusst gesetzter Moment, und der Zyklus wirkt
nur, wenn der Spieler sieht, wie weit er gekommen ist. Ein still gutgeschriebener
Bonus wäre für ihn nicht von einem Fehler zu unterscheiden. Wegtippen lässt den
Anspruch bestehen — er verfällt nicht.

**Der Stand des Tagesbonus liegt in einer eigenen Tabelle, nicht in Spalten von
`game_state`.** Zusätzliche Spalten wären bei einem alten Spielstand mit ihrem
Standardwert vorhanden und würden die signierte Zeichenkette verändern; jeder
Spielstand aus Version 4 würde beim nächsten Start als manipuliert gelten. Eine
eigene Tabelle ist dort schlicht leer, und ein leerer Abschnitt trägt nichts bei.

**Ein Booster ist ein Upgrade auf Zeit — und benutzt dieselbe Wirkungsart.**
`BoosterType` trägt einen `UpgradeEffect`, und `CalculateModifiersUseCase`
wirft Upgrades und laufende Booster in denselben Topf. Zwei getrennte
Auswertungen wären zwei Kopien derselben Balancing-Regeln, die früher oder
später auseinanderlaufen; so wirkt eine neue Wirkungsart ohne weiteres Zutun in
beiden Systemen.

**Booster laufen in Echtzeit, ihr Ablaufen ist eine Änderung des Spielstands.**
Gespeichert werden absolute Zeitpunkte der Systemuhr — die monotone Uhr steht
bei beendetem Prozess nicht zur Verfügung. `BoosterManager` entfernt
abgelaufene Booster im Sekundentakt aus dem Spielstand, statt jede Leseseite
die Uhrzeit prüfen zu lassen. Damit bekommen Modifikatoren, Anzeige und
Autosave das Ablaufen gleichermaßen mit, und `CalculateModifiersUseCase` bleibt
eine reine Funktion des Spielstands ohne Zeitparameter.

Ein Zurückstellen der Geräteuhr verlängert einen Booster nicht: Die
Restlaufzeit ist auf die gewährte Laufzeit gedeckelt. Mehr als „Booster neu
starten" ist damit nicht zu holen — und genau das kann der Spieler ohnehin
kaufen.

**Booster wirken nicht auf den Offline-Ertrag.** Sie belohnen aktives Spielen.
Würden sie auch bei geschlossener App zahlen, wäre das Schließen der App die
beste Art, einen Booster zu nutzen. Der Offline-Pfad ruft deshalb
`calculateModifiers(state, includeBoosters = false)` — Upgrades bleiben davon
unberührt, denn ein Offline-Upgrade wurde genau dafür gekauft.

**Ein zweiter Start verlängert, statt zu verfallen.** Alles andere wäre eine
bezahlte Belohnung, die verpufft. Gedeckelt auf das Vierfache der
Grundlaufzeit, damit der Booster ein Ereignis bleibt und nicht zum Normalzustand
wird; ein Kauf, der wegen des Deckels wirkungslos bliebe, wird vorher
abgewiesen statt abgebucht.

**Das Werbe-SDK taucht an genau einer Stelle auf.** `RewardedAdSource` ist die
Grenze; alles darüber — Wartezeit, Belohnung, Anzeige — kennt nur diese
Schnittstelle. Werbevermittler werden gewechselt, und ein SDK, das sich durch
ViewModels und Use Cases zieht, macht daraus ein Umbauprojekt statt eines
Austauschs. Die Schnittstelle kennt bewusst **keine** `Activity`: Sie
hereinzureichen hieße, sie durch Manager und ViewModels durchzureichen, und
damit entstünde genau die Kopplung, die sie verhindern soll.

**Die Nachbildung ist kein Platzhalter.** `FakeRewardedAdSource` lässt das
Laden dauern, gelegentlich fehlschlagen und den Spieler abbrechen — die drei
Eigenschaften, an denen die Anzeige tatsächlich hängt. Eine Nachbildung, die
immer sofort erfolgreich ist, ließe die zugehörigen Anzeigepfade ungetestet,
bis sie beim echten SDK erstmals auftreten.

**Vier Ausgänge statt eines Wahrheitswerts.** Belohnung erhalten, abgebrochen,
nicht bereit, fehlgeschlagen — sie führen zu vier verschiedenen Reaktionen. Wer
abbricht, hat nichts falsch gemacht und bekommt keine Fehlermeldung; wer kein
Netz hat, soll genau das erfahren und nicht glauben, das Angebot sei
verschwunden.

**Die Wartezeit beginnt erst mit der Gutschrift.** Sie ist der Preis der
Belohnung, nicht die Strafe für einen Abbruch oder für fehlendes Netz. Sie
liegt im Spielstand und unter der Prüfsumme: Im Arbeitsspeicher wäre sie durch
Schließen und Öffnen der App zu umgehen, und ohne Signatur wäre eine gelöschte
Zeile ein Booster im Minutentakt. Sie überlebt auch den Prestige-Reset — sonst
wäre der Reset der Weg, sie zu umgehen.

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
