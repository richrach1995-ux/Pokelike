package com.pokelike.idle.domain.model

/**
 * Zeitraum, ueber den eine Quest laeuft.
 *
 * @property id Stabiler Schluessel fuer den Spielstand.
 */
enum class QuestPeriod(val id: String) {
    /** Setzt sich mit jedem Kalendertag zurueck. */
    DAILY("daily"),

    /** Setzt sich mit jeder Kalenderwoche zurueck. */
    WEEKLY("weekly"),

    /** Laeuft ueber die gesamte Spielzeit und wird nie zurueckgesetzt. */
    LIFETIME("lifetime"),
}

/**
 * Messgroesse, an der eine Quest ihren Fortschritt misst.
 *
 * Bewusst eine kleine, feste Auswahl statt beliebiger Bedingungen: Eine Quest
 * braucht nicht nur die Antwort "erfuellt oder nicht", sondern einen Stand wie
 * "37 von 100". Das leistet nur eine Groesse, die sich zaehlen laesst.
 *
 * Alle Werte sind nur steigend. Das ist Voraussetzung dafuer, dass der
 * Fortschritt eines Zeitraums als Differenz zu einem Ausgangswert bestimmt
 * werden kann - der Kern des Zuruecksetzens ohne eigene Zaehler je Quest.
 */
enum class QuestMetric(val id: String) {
    CLICKS("clicks"),
    CRITICAL_CLICKS("critical_clicks"),
    COINS_EARNED("coins_earned"),
    BUILDINGS_PURCHASED("buildings_purchased"),
    UPGRADES_OWNED("upgrades_owned");

    /** Aktueller Stand dieser Groesse im Spielstand. */
    fun readFrom(state: GameState): BigNumber = when (this) {
        CLICKS -> BigNumber.of(state.statistics.totalClicks)
        CRITICAL_CLICKS -> BigNumber.of(state.statistics.totalCriticalClicks)
        COINS_EARNED -> state.statistics.lifetimeEarned[ResourceType.COINS]
        BUILDINGS_PURCHASED -> BigNumber.of(state.statistics.totalBuildingsPurchased)
        UPGRADES_OWNED -> BigNumber.of(state.upgrades.count)
    }

    companion object {
        private val byId: Map<String, QuestMetric> = entries.associateBy { it.id }

        fun fromId(id: String): QuestMetric? = byId[id]
    }
}

/**
 * Alle Quests des Spiels.
 *
 * Rein deklarativ wie Gebaeude, Upgrades und Achievements.
 *
 * Belohnt wird gemischt: Muenzen bei Tagesquests, weil sie sofort in den
 * naechsten Kauf fliessen, Diamanten bei Wochen- und Lebenszeitquests, weil
 * deren Wert nicht mit dem Fortschritt verfaellt.
 *
 * @property id Stabiler Schluessel fuer Spielstand und Analytics.
 * @property period Zeitraum, siehe [QuestPeriod].
 * @property metric Messgroesse, siehe [QuestMetric].
 * @property target Zielwert innerhalb des Zeitraums.
 * @property reward Belohnung bei Abholung.
 */
enum class QuestType(
    val id: String,
    val period: QuestPeriod,
    val metric: QuestMetric,
    val target: BigNumber,
    val reward: ResourceBundle,
) {

    // --- Taeglich ---------------------------------------------------------

    DAILY_HUNDRED_CLICKS(
        id = "daily_hundred_clicks",
        period = QuestPeriod.DAILY,
        metric = QuestMetric.CLICKS,
        target = BigNumber.of(100),
        reward = ResourceBundle.of(
            ResourceType.COINS to BigNumber.of(5_000),
            ResourceType.DIAMONDS to BigNumber.of(2),
        ),
    ),
    DAILY_FIVE_HUNDRED_CLICKS(
        id = "daily_five_hundred_clicks",
        period = QuestPeriod.DAILY,
        metric = QuestMetric.CLICKS,
        target = BigNumber.of(500),
        reward = ResourceBundle.of(
            ResourceType.COINS to BigNumber.of(25_000),
            ResourceType.DIAMONDS to BigNumber.of(5),
        ),
    ),
    DAILY_TEN_BUILDINGS(
        id = "daily_ten_buildings",
        period = QuestPeriod.DAILY,
        metric = QuestMetric.BUILDINGS_PURCHASED,
        target = BigNumber.of(10),
        reward = ResourceBundle.of(
            ResourceType.COINS to BigNumber.of(10_000),
            ResourceType.DIAMONDS to BigNumber.of(3),
        ),
    ),
    DAILY_TWENTY_CRITICALS(
        id = "daily_twenty_criticals",
        period = QuestPeriod.DAILY,
        metric = QuestMetric.CRITICAL_CLICKS,
        target = BigNumber.of(20),
        reward = ResourceBundle.of(
            ResourceType.COINS to BigNumber.of(15_000),
            ResourceType.DIAMONDS to BigNumber.of(3),
        ),
    ),

    // --- Woechentlich -----------------------------------------------------

    WEEKLY_FIVE_THOUSAND_CLICKS(
        id = "weekly_five_thousand_clicks",
        period = QuestPeriod.WEEKLY,
        metric = QuestMetric.CLICKS,
        target = BigNumber.of(5_000),
        reward = ResourceBundle.single(ResourceType.DIAMONDS, BigNumber.of(25)),
    ),
    WEEKLY_HUNDRED_BUILDINGS(
        id = "weekly_hundred_buildings",
        period = QuestPeriod.WEEKLY,
        metric = QuestMetric.BUILDINGS_PURCHASED,
        target = BigNumber.of(100),
        reward = ResourceBundle.single(ResourceType.DIAMONDS, BigNumber.of(30)),
    ),
    WEEKLY_MILLION_COINS(
        id = "weekly_million_coins",
        period = QuestPeriod.WEEKLY,
        metric = QuestMetric.COINS_EARNED,
        target = BigNumber.of(1_000_000),
        reward = ResourceBundle.single(ResourceType.DIAMONDS, BigNumber.of(20)),
    ),

    // --- Lebenszeit -------------------------------------------------------

    LIFETIME_TEN_THOUSAND_CLICKS(
        id = "lifetime_ten_thousand_clicks",
        period = QuestPeriod.LIFETIME,
        metric = QuestMetric.CLICKS,
        target = BigNumber.of(10_000),
        reward = ResourceBundle.single(ResourceType.DIAMONDS, BigNumber.of(50)),
    ),
    LIFETIME_FIVE_UPGRADES(
        id = "lifetime_five_upgrades",
        period = QuestPeriod.LIFETIME,
        metric = QuestMetric.UPGRADES_OWNED,
        target = BigNumber.of(5),
        reward = ResourceBundle.single(ResourceType.DIAMONDS, BigNumber.of(40)),
    ),
    LIFETIME_THOUSAND_BUILDINGS(
        id = "lifetime_thousand_buildings",
        period = QuestPeriod.LIFETIME,
        metric = QuestMetric.BUILDINGS_PURCHASED,
        target = BigNumber.of(1_000),
        reward = ResourceBundle.single(ResourceType.DIAMONDS, BigNumber.of(80)),
    );

    companion object {

        private val byId: Map<String, QuestType> = entries.associateBy { it.id }

        /** Liefert `null` bei unbekanntem Schluessel. */
        fun fromId(id: String): QuestType? = byId[id]

        /** Alle Quests eines Zeitraums. */
        fun ofPeriod(period: QuestPeriod): List<QuestType> = entries.filter { it.period == period }
    }
}
