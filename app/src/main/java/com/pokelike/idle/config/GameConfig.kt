package com.pokelike.idle.config

/**
 * Zentrale Sammlung aller Balancing- und Laufzeitkonstanten.
 *
 * Warum ein eigenes Objekt statt verstreuter Konstanten:
 * Ein Idle-Spiel wird ueber seine Zahlen gesteuert, nicht ueber seinen Code.
 * Sobald Werte in Screens, ViewModels oder Use Cases verteilt liegen, ist
 * Balancing eine Suchaktion durch die halbe Codebasis. Alles, was ein Designer
 * jemals drehen wollen wird, steht deshalb hier.
 *
 * Ab dem Remote-Config-Schritt wird dieses Objekt zur Fallback-Ebene: Firebase
 * liefert dann die aktiven Werte, und die Konstanten hier greifen, solange kein
 * Remote-Wert vorliegt (Erststart, kein Netz, Fetch fehlgeschlagen). Deshalb
 * sind die Werte bewusst so gewaehlt, dass das Spiel auch komplett offline
 * sinnvoll spielbar bleibt.
 */
object GameConfig {

    /**
     * Taktrate der Spiel-Engine.
     *
     * 100 ms sind ein bewusster Kompromiss: fein genug, damit Einkommen und
     * Booster-Restlaufzeiten fluessig wirken, aber grob genug, dass die Engine
     * unabhaengig von der Bildwiederholrate laeuft. Die Engine wird bewusst
     * NICHT an die Compose-Frameclock gekoppelt: bei 120 Hz wuerde das Spiel
     * sonst doppelt so schnell ticken wie bei 60 Hz.
     */
    const val TICK_INTERVAL_MS: Long = 100L

    /**
     * Obergrenze fuer die Zeitspanne, die ein einzelner Tick verrechnen darf.
     *
     * Wird der Prozess gedrosselt oder haelt das System die Coroutine an, kann
     * zwischen zwei Ticks deutlich mehr Zeit vergehen als geplant. Ohne Deckel
     * wuerde ein einziger Tick dann eine riesige Menge Ertrag gutschreiben.
     * Alles oberhalb dieser Grenze ist per Definition Offline-Zeit und wird vom
     * Offline-Progress-Pfad behandelt, nicht vom Live-Tick.
     */
    const val MAX_TICK_DELTA_MS: Long = 1_000L

    /**
     * Abstand zwischen zwei automatischen Speichervorgaengen.
     *
     * 30 Sekunden begrenzen den maximalen Fortschrittsverlust bei einem Crash
     * auf ein Mass, das Spieler erfahrungsgemaess noch akzeptieren, ohne die
     * Datenbank unnoetig oft zu beschreiben.
     */
    const val AUTOSAVE_INTERVAL_MS: Long = 30_000L

    /**
     * Preissteigerung pro gekauftem Gebaeude: `preis = basispreis * 1.15^anzahl`.
     *
     * 1.15 ist der etablierte Standardfaktor des Genres. Er erzeugt eine
     * Kaufkurve, bei der jedes Gebaeude sich zunaechst lohnt und spaeter
     * natuerlich von der naechsten Stufe abgeloest wird.
     */
    const val BUILDING_COST_GROWTH: Double = 1.15

    // --- Startausstattung -------------------------------------------------

    /**
     * Muenzen zu Spielbeginn.
     *
     * Bewusst null: Der erste Klick soll den ersten Muenzgewinn erzeugen. Ein
     * Startguthaben wuerde genau den Moment entwerten, auf dem die gesamte
     * Kernschleife des Genres aufbaut.
     */
    const val STARTING_COINS: Long = 0L

    /**
     * Diamanten zu Spielbeginn.
     *
     * Eine kleine Menge Premiumwaehrung ist eine bewusste Designentscheidung:
     * Sie erlaubt dem Spieler, den Premium-Shop einmal folgenlos auszuprobieren
     * und den Wert der Waehrung kennenzulernen. Wer nie erlebt hat, was
     * Diamanten bewirken, kauft auch keine.
     *
     * 25 reichen fuer genau einen guenstigen Booster - genug fuer das Erlebnis,
     * zu wenig, um den Einstieg zu ueberspringen.
     */
    const val STARTING_DIAMONDS: Long = 25L

    // --- Offline-Fortschritt ----------------------------------------------

    /**
     * Anteil des Einkommens, der offline gutgeschrieben wird.
     *
     * Bewusst unter 100 Prozent. Volle Offline-Produktion nimmt dem aktiven
     * Spielen jeden Vorteil - es gaebe keinen Grund, die App zu oeffnen. Die
     * Haelfte ist im Genre etabliert: spuerbar genug, dass sich Zurueckkommen
     * lohnt, klein genug, dass aktives Spielen ueberlegen bleibt.
     *
     * Zugleich der wichtigste Hebel fuer die Monetarisierung: Ein Offline-
     * Booster, der diesen Wert anhebt, ist eines der am besten angenommenen
     * Premiumangebote des Genres.
     */
    const val OFFLINE_EFFICIENCY: Double = 0.5

    /**
     * Obergrenze der anrechenbaren Offline-Zeit.
     *
     * Acht Stunden entsprechen etwa einer Nachtruhe. Der Deckel hat zwei
     * Aufgaben: Er haelt den Spieler in einem taeglichen Rhythmus, statt
     * wochenlanges Wegbleiben zu belohnen, und er begrenzt den Schaden, falls
     * die Manipulationserkennung je umgangen wird.
     */
    const val MAX_OFFLINE_MILLIS: Long = 8L * 60L * 60L * 1_000L

    /**
     * Kuerzeste Abwesenheit, die als Offline-Zeit zaehlt.
     *
     * Unterhalb einer Minute ist der Ertrag belanglos, ein Willkommensdialog
     * dafuer aber stoerend - etwa wenn der Spieler nur kurz eine Nachricht
     * beantwortet hat.
     */
    const val MIN_OFFLINE_MILLIS: Long = 60L * 1_000L

    // --- Persistenz -------------------------------------------------------

    /**
     * Zulaessiger Vorlauf der Systemuhr gegenueber dem letzten Speicherpunkt.
     *
     * Liegt der gespeicherte Zeitpunkt in der Zukunft, wurde die Uhr entweder
     * zurueckgestellt oder zuvor vorgestellt. Eine kleine Toleranz faengt
     * legitime Faelle ab: Zeitzonenwechsel, Sommerzeit und die Korrektur durch
     * einen Zeitserver bewegen die Uhr um Sekunden bis Minuten.
     */
    const val CLOCK_TOLERANCE_MILLIS: Long = 5L * 60L * 1_000L

    // --- Klick-System -----------------------------------------------------

    /**
     * Muenzen fuer einen Klick ohne jede Verbesserung.
     *
     * Genau eins. Der Wert selbst ist belanglos - entscheidend ist, dass er
     * der Bezugspunkt fuer jede spaetere Verbesserung ist. Ein hoeherer
     * Startwert wuerde die ersten Upgrades gefuehlt wirkungslos machen, weil
     * "+1 pro Klick" neben einer Zehn kaum auffaellt.
     */
    const val BASE_COINS_PER_CLICK: Long = 1L

    /**
     * Grundwahrscheinlichkeit fuer einen kritischen Treffer.
     *
     * Fuenf Prozent bedeuten im Schnitt jeden zwanzigsten Klick. Das ist haeufig
     * genug, dass ein Spieler den Effekt in den ersten Sekunden bemerkt, und
     * selten genug, dass er besonders bleibt. Wird sich unter zwanzig Prozent
     * kaum jemand ueberraschen lassen, oberhalb davon verliert der Treffer
     * seine Wirkung.
     */
    const val BASE_CRITICAL_CHANCE: Double = 0.05

    /**
     * Faktor eines kritischen Treffers.
     *
     * Fuenffach. Ein Faktor von zwei faellt neben dem Combo-Bonus kaum auf; ab
     * etwa zehnfach dominiert der Zufall den Ertrag so stark, dass gezieltes
     * Spielen sinnlos wirkt.
     */
    const val BASE_CRITICAL_MULTIPLIER: Double = 5.0

    /**
     * Zeitfenster, in dem ein Klick die Combo fortsetzt.
     *
     * 1,5 Sekunden sind bewusst grosszuegig. Ein enges Fenster belohnt
     * ausschliesslich schnelles Tippen und benachteiligt aeltere Geraete und
     * Spieler mit eingeschraenkter Feinmotorik. Die Combo soll fuer
     * Aufmerksamkeit belohnen, nicht fuer Fingerfertigkeit.
     */
    const val COMBO_WINDOW_MS: Long = 1_500L

    /**
     * Zuwachs des Combo-Faktors je Stufe.
     *
     * Zwei Prozent je Klick. Zusammen mit [COMBO_MAX_STEPS] ergibt das im
     * Bestfall den doppelten Ertrag.
     */
    const val COMBO_STEP_BONUS: Double = 0.02

    /**
     * Obergrenze der Combo-Stufen.
     *
     * Fuenfzig Stufen sind in gut einer Minute erreichbar. Ohne Deckel waere
     * dauerhaftes Tippen jedem Gebaeudeausbau ueberlegen, und das Spiel
     * verloere seinen Idle-Charakter.
     */
    const val COMBO_MAX_STEPS: Int = 50

    // --- Gebaeude ---------------------------------------------------------

    /**
     * Anteil des Grundpreises, ab dem ein Gebaeude sichtbar wird.
     *
     * Die Haelfte. Der Spieler sieht das naechste Ziel, bevor er es sich
     * leisten kann - das ist der Moment, der ihn weiterspielen laesst. Bei 1.0
     * erschiene ein Gebaeude erst, wenn es bereits kaufbar ist, und der
     * Vorfreude-Effekt entfiele.
     */
    const val BUILDING_UNLOCK_FRACTION: Double = 0.5

    /**
     * Obergrenze fuer einen einzelnen Sammelkauf.
     *
     * Begrenzt die Suche nach der groesstmoeglichen Kaufmenge. Ohne Deckel
     * koennte ein extrem hoher Kontostand eine Menge liefern, deren Berechnung
     * die Oberflaeche blockiert - und mehr als zehntausend Exemplare auf einmal
     * ist ohnehin keine sinnvolle Bedienhandlung.
     */
    const val MAX_BULK_PURCHASE: Int = 10_000

    // --- Prestige ---------------------------------------------------------

    /**
     * Muenzmenge, die einem Prestige-Punkt zugrunde liegt.
     *
     * Die Punkte folgen der Wurzelformel `punkte = wurzel(lebenssumme / basis)`.
     * Die Wurzel ist im Genre etabliert und aus einem guten Grund: Sie sorgt
     * dafuer, dass der zehnfache Ertrag nur etwa dreifach so viele Punkte
     * bringt. Ohne diese Daempfung waere ein einziger sehr langer Durchlauf
     * jedem regelmaessigen Spielen ueberlegen, und das Prestige-System verloere
     * seinen Zweck.
     *
     * 1e10 als Basis bedeutet: der erste Punkt bei zehn Milliarden verdienten
     * Muenzen, zehn Punkte bei einer Billion, hundert bei hundert Billionen.
     */
    const val PRESTIGE_BASE_COINS: Double = 1e10

    /**
     * Mindestzahl an Punkten, ab der ein Reset moeglich ist.
     *
     * Ohne diese Schwelle koennte ein Spieler fuer null Punkte zuruecksetzen
     * und seinen gesamten Fortschritt ohne Gegenwert verlieren - der
     * aergerlichste denkbare Fehlgriff.
     */
    const val PRESTIGE_MIN_POINTS: Double = 1.0

    /**
     * Dauerhafter Bonus je Prestige-Punkt.
     *
     * Bewusst additiv: Hundert Punkte ergeben den dreifachen Ertrag, nicht das
     * Zweihoch-Hundertfache. Multiplikativ waeren die Zahlen nach wenigen
     * Durchlaeufen so gross, dass jedes weitere Balancing wirkungslos bliebe -
     * Gebaeudepreise, Upgrade-Kosten und Angebotsgestaltung haetten dann keinen
     * Einfluss mehr auf den Spielverlauf.
     *
     * Der Bonus wirkt auf Klickertrag und Leerlaufeinkommen gleichermassen.
     */
    const val PRESTIGE_BONUS_PER_POINT: Double = 0.02

    // --- Taeglicher Bonus -------------------------------------------------

    /**
     * Preis einer Ladung Serienschutz in Diamanten.
     *
     * Der Betrag entspricht genau der Diamantensumme eines vollstaendigen
     * Zyklus (siehe `DailyRewardType`). Eine lueckenlose Woche bezahlt damit
     * genau eine Ladung - der Spieler kann das System aus sich heraus tragen,
     * ohne dass es zur reinen Selbstbedienung wird.
     */
    const val STREAK_PROTECTION_PRICE_DIAMONDS: Long = 50L

    /**
     * Hoechstzahl gleichzeitig gehaltener Ladungen.
     *
     * Ohne Deckel liesse sich ein Vorrat fuer Monate anlegen, und die Serie
     * waere keine Aussage mehr ueber regelmaessiges Spielen. Drei Ladungen
     * decken eine uebliche Abwesenheit ab - ein verlaengertes Wochenende,
     * einen Kurzurlaub - und nicht mehr.
     */
    const val STREAK_PROTECTION_MAX_CHARGES: Int = 3
}
