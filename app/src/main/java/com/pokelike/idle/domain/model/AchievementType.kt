package com.pokelike.idle.domain.model

/** Gliederung der Achievement-Liste. */
enum class AchievementCategory(val id: String) {
    CLICKING("clicking"),
    WEALTH("wealth"),
    BUILDING("building"),
    PROGRESSION("progression"),
}

/**
 * Alle Achievements des Spiels.
 *
 * Wie Gebaeude und Upgrades rein deklarativ. Die Bedingung ist eine
 * [UnlockCondition] und damit dieselbe Struktur, die schon Upgrades
 * freischaltet - ein Beleg dafuer, dass die Entscheidung fuer ein sealed
 * interface statt eines Enums mit Zahlenfeldern getragen hat.
 *
 * Jedes Achievement gibt eine Belohnung. Ein Achievement ohne Gegenwert ist im
 * Genre ein reiner Haken auf einer Liste; mit Belohnung wird es zu einem Ziel,
 * auf das der Spieler zusteuert.
 *
 * Belohnt wird ueberwiegend in Diamanten. Muenzen waeren im spaeten Spiel
 * bedeutungslos - ein Betrag, der zu Beginn grosszuegig wirkt, ist nach drei
 * Stunden weniger als eine Sekunde Einkommen.
 *
 * @property id Stabiler Schluessel fuer Spielstand und Analytics.
 * @property category Einordnung fuer die Gliederung der Liste.
 * @property condition Bedingung fuer die Freischaltung.
 * @property reward Einmalige Belohnung.
 */
enum class AchievementType(
    val id: String,
    val category: AchievementCategory,
    val condition: UnlockCondition,
    val reward: ResourceBundle,
) {

    // --- Klicken ----------------------------------------------------------

    FIRST_CLICK(
        id = "first_click",
        category = AchievementCategory.CLICKING,
        condition = UnlockCondition.TotalClicks(1L),
        reward = diamonds(5),
    ),
    HUNDRED_CLICKS(
        id = "hundred_clicks",
        category = AchievementCategory.CLICKING,
        condition = UnlockCondition.TotalClicks(100L),
        reward = diamonds(10),
    ),
    THOUSAND_CLICKS(
        id = "thousand_clicks",
        category = AchievementCategory.CLICKING,
        condition = UnlockCondition.TotalClicks(1_000L),
        reward = diamonds(25),
    ),
    TEN_THOUSAND_CLICKS(
        id = "ten_thousand_clicks",
        category = AchievementCategory.CLICKING,
        condition = UnlockCondition.TotalClicks(10_000L),
        reward = diamonds(75),
    ),
    FIRST_CRITICAL(
        id = "first_critical",
        category = AchievementCategory.CLICKING,
        condition = UnlockCondition.TotalCriticalClicks(1L),
        reward = diamonds(5),
    ),
    HUNDRED_CRITICALS(
        id = "hundred_criticals",
        category = AchievementCategory.CLICKING,
        condition = UnlockCondition.TotalCriticalClicks(100L),
        reward = diamonds(20),
    ),

    // --- Reichtum ---------------------------------------------------------

    THOUSAND_COINS(
        id = "thousand_coins",
        category = AchievementCategory.WEALTH,
        condition = UnlockCondition.LifetimeCoins(BigNumber.of(1_000)),
        reward = diamonds(5),
    ),
    HUNDRED_THOUSAND_COINS(
        id = "hundred_thousand_coins",
        category = AchievementCategory.WEALTH,
        condition = UnlockCondition.LifetimeCoins(BigNumber.of(100_000)),
        reward = diamonds(15),
    ),
    MILLION_COINS(
        id = "million_coins",
        category = AchievementCategory.WEALTH,
        condition = UnlockCondition.LifetimeCoins(BigNumber.of(1_000_000)),
        reward = diamonds(30),
    ),
    BILLION_COINS(
        id = "billion_coins",
        category = AchievementCategory.WEALTH,
        condition = UnlockCondition.LifetimeCoins(BigNumber.of(1_000_000_000)),
        reward = diamonds(60),
    ),
    TRILLION_COINS(
        id = "trillion_coins",
        category = AchievementCategory.WEALTH,
        condition = UnlockCondition.LifetimeCoins(BigNumber.of(1.0, 12)),
        reward = diamonds(120),
    ),

    // --- Ausbau -----------------------------------------------------------

    FIRST_BUILDING(
        id = "first_building",
        category = AchievementCategory.BUILDING,
        condition = UnlockCondition.BuildingsPurchased(1L),
        reward = diamonds(5),
    ),
    TEN_BUILDINGS(
        id = "ten_buildings",
        category = AchievementCategory.BUILDING,
        condition = UnlockCondition.BuildingsPurchased(10L),
        reward = diamonds(10),
    ),
    HUNDRED_BUILDINGS(
        id = "hundred_buildings",
        category = AchievementCategory.BUILDING,
        condition = UnlockCondition.BuildingsPurchased(100L),
        reward = diamonds(30),
    ),
    THOUSAND_BUILDINGS(
        id = "thousand_buildings",
        category = AchievementCategory.BUILDING,
        condition = UnlockCondition.BuildingsPurchased(1_000L),
        reward = diamonds(90),
    ),
    ALL_BUILDING_TYPES(
        id = "all_building_types",
        category = AchievementCategory.BUILDING,
        condition = UnlockCondition.All(
            BuildingType.entries.map { UnlockCondition.BuildingCount(it, 1) },
        ),
        reward = diamonds(150),
    ),

    // --- Fortschritt ------------------------------------------------------

    FIRST_UPGRADE(
        id = "first_upgrade",
        category = AchievementCategory.PROGRESSION,
        condition = UnlockCondition.UpgradesOwned(1),
        reward = diamonds(5),
    ),
    FIVE_UPGRADES(
        id = "five_upgrades",
        category = AchievementCategory.PROGRESSION,
        condition = UnlockCondition.UpgradesOwned(5),
        reward = diamonds(20),
    ),
    FIRST_PRESTIGE(
        id = "first_prestige",
        category = AchievementCategory.PROGRESSION,
        condition = UnlockCondition.PrestigeCount(1),
        reward = diamonds(50),
    ),
    FIFTH_PRESTIGE(
        id = "fifth_prestige",
        category = AchievementCategory.PROGRESSION,
        condition = UnlockCondition.PrestigeCount(5),
        reward = diamonds(200),
    ),

    // --- Treue ------------------------------------------------------------

    /** Eine vollstaendige Woche - genau ein Durchlauf des Belohnungszyklus. */
    WEEK_STREAK(
        id = "week_streak",
        category = AchievementCategory.PROGRESSION,
        condition = UnlockCondition.LoginStreak(7),
        reward = diamonds(25),
    ),

    MONTH_STREAK(
        id = "month_streak",
        category = AchievementCategory.PROGRESSION,
        condition = UnlockCondition.LoginStreak(30),
        reward = diamonds(150),
    );

    companion object {

        private val byId: Map<String, AchievementType> = entries.associateBy { it.id }

        /** Liefert `null` bei unbekanntem Schluessel. */
        fun fromId(id: String): AchievementType? = byId[id]
    }
}

/**
 * Kurzform fuer eine Diamantenbelohnung.
 *
 * Als Funktion auf Dateiebene, damit die Tabelle oben lesbar bleibt: Ein
 * ausgeschriebenes `ResourceBundle.single(ResourceType.DIAMONDS, ...)` je
 * Eintrag wuerde die eigentlichen Werte im Rauschen verschwinden lassen.
 */
private fun diamonds(amount: Int): ResourceBundle =
    ResourceBundle.single(ResourceType.DIAMONDS, BigNumber.of(amount))
