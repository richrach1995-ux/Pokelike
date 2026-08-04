package com.pokelike.idle.domain.model

/**
 * Grobe Einordnung eines Upgrades.
 *
 * Dient der Gliederung der Liste. Ohne sie stuenden zwanzig Eintraege
 * unsortiert untereinander, und der Spieler koennte nicht erkennen, welches
 * davon zu seiner Spielweise passt.
 */
enum class UpgradeCategory(val id: String) {
    CLICK("click"),
    CRITICAL("critical"),
    INCOME("income"),
    BUILDING("building"),
    OFFLINE("offline"),
}

/**
 * Alle Upgrades des Spiels.
 *
 * Rein deklarativ: Ein neues Upgrade ist ein zusaetzlicher Eintrag mit Preis,
 * Wirkung und Freischaltbedingung. Weder Kaufablauf noch Berechnung noch
 * Oberflaeche muessen dafuer angefasst werden - genau das war die Vorgabe
 * "neue Upgrades nur ueber Datenobjekte".
 *
 * Upgrades werden einmalig gekauft und bleiben dauerhaft wirksam. Deshalb gibt
 * es keine Anzahl, sondern nur "besessen oder nicht".
 *
 * @property id Stabiler Schluessel fuer Spielstand und Analytics, getrennt vom
 *   Enum-Namen: Eine Umbenennung im Code darf keinen Spielstand unlesbar
 *   machen.
 * @property price Preis in Muenzen.
 * @property category Einordnung fuer die Gliederung der Liste.
 * @property effect Wirkung, siehe [UpgradeEffect].
 * @property unlockCondition Bedingung fuer die Sichtbarkeit.
 */
enum class UpgradeType(
    val id: String,
    val price: Double,
    val category: UpgradeCategory,
    val effect: UpgradeEffect,
    val unlockCondition: UnlockCondition,
) {

    // --- Klick ------------------------------------------------------------

    STRONGER_FINGERS(
        id = "stronger_fingers",
        price = 100.0,
        category = UpgradeCategory.CLICK,
        effect = UpgradeEffect.ClickMultiplier(2.0),
        unlockCondition = UnlockCondition.TotalClicks(50L),
    ),
    IRON_FINGERS(
        id = "iron_fingers",
        price = 5_000.0,
        category = UpgradeCategory.CLICK,
        effect = UpgradeEffect.ClickMultiplier(2.0),
        unlockCondition = UnlockCondition.TotalClicks(500L),
    ),
    GOLDEN_TOUCH(
        id = "golden_touch",
        price = 250_000.0,
        category = UpgradeCategory.CLICK,
        effect = UpgradeEffect.ClickMultiplier(3.0),
        unlockCondition = UnlockCondition.TotalClicks(5_000L),
    ),
    HEAVY_HANDS(
        id = "heavy_hands",
        price = 1_000.0,
        category = UpgradeCategory.CLICK,
        effect = UpgradeEffect.ClickFlatBonus(BigNumber.of(10)),
        unlockCondition = UnlockCondition.TotalClicks(100L),
    ),
    THUNDER_STRIKE(
        id = "thunder_strike",
        price = 100_000.0,
        category = UpgradeCategory.CLICK,
        effect = UpgradeEffect.ClickFlatBonus(BigNumber.of(250)),
        unlockCondition = UnlockCondition.TotalClicks(1_000L),
    ),

    // --- Kritische Treffer ------------------------------------------------

    LUCKY_CHARM(
        id = "lucky_charm",
        price = 10_000.0,
        category = UpgradeCategory.CRITICAL,
        effect = UpgradeEffect.CriticalChanceBonus(0.05),
        unlockCondition = UnlockCondition.TotalClicks(200L),
    ),
    FOUR_LEAF_CLOVER(
        id = "four_leaf_clover",
        price = 1_000_000.0,
        category = UpgradeCategory.CRITICAL,
        effect = UpgradeEffect.CriticalChanceBonus(0.05),
        unlockCondition = UnlockCondition.TotalClicks(2_000L),
    ),
    CRITICAL_MASS(
        id = "critical_mass",
        price = 500_000.0,
        category = UpgradeCategory.CRITICAL,
        effect = UpgradeEffect.CriticalMultiplierBonus(5.0),
        unlockCondition = UnlockCondition.TotalClicks(1_000L),
    ),

    // --- Einkommen --------------------------------------------------------

    PRODUCTION_LINE(
        id = "production_line",
        price = 10_000.0,
        category = UpgradeCategory.INCOME,
        effect = UpgradeEffect.IncomeMultiplier(2.0),
        unlockCondition = UnlockCondition.TotalBuildings(10),
    ),
    AUTOMATION(
        id = "automation",
        price = 1_000_000.0,
        category = UpgradeCategory.INCOME,
        effect = UpgradeEffect.IncomeMultiplier(2.0),
        unlockCondition = UnlockCondition.TotalBuildings(50),
    ),
    GLOBAL_LOGISTICS(
        id = "global_logistics",
        price = 500_000_000.0,
        category = UpgradeCategory.INCOME,
        effect = UpgradeEffect.IncomeMultiplier(3.0),
        unlockCondition = UnlockCondition.TotalBuildings(150),
    ),

    // --- Einzelne Gebaeude ------------------------------------------------

    NIMBLE_FINGERS(
        id = "nimble_fingers",
        price = 500.0,
        category = UpgradeCategory.BUILDING,
        effect = UpgradeEffect.BuildingIncomeMultiplier(BuildingType.FINGER, 3.0),
        unlockCondition = UnlockCondition.BuildingCount(BuildingType.FINGER, 10),
    ),
    ERGONOMIC_CURSOR(
        id = "ergonomic_cursor",
        price = 5_000.0,
        category = UpgradeCategory.BUILDING,
        effect = UpgradeEffect.BuildingIncomeMultiplier(BuildingType.CURSOR, 3.0),
        unlockCondition = UnlockCondition.BuildingCount(BuildingType.CURSOR, 10),
    ),
    DEEP_DRILLING(
        id = "deep_drilling",
        price = 250_000.0,
        category = UpgradeCategory.BUILDING,
        effect = UpgradeEffect.BuildingIncomeMultiplier(BuildingType.MINE, 3.0),
        unlockCondition = UnlockCondition.BuildingCount(BuildingType.MINE, 10),
    ),
    FERTILIZER(
        id = "fertilizer",
        price = 2_500_000.0,
        category = UpgradeCategory.BUILDING,
        effect = UpgradeEffect.BuildingIncomeMultiplier(BuildingType.FARM, 3.0),
        unlockCondition = UnlockCondition.BuildingCount(BuildingType.FARM, 10),
    ),
    BULK_DISCOUNT(
        id = "bulk_discount",
        price = 100_000.0,
        category = UpgradeCategory.BUILDING,
        effect = UpgradeEffect.BuildingDiscount(0.05),
        unlockCondition = UnlockCondition.TotalBuildings(25),
    ),
    WHOLESALE(
        id = "wholesale",
        price = 50_000_000.0,
        category = UpgradeCategory.BUILDING,
        effect = UpgradeEffect.BuildingDiscount(0.10),
        unlockCondition = UnlockCondition.TotalBuildings(100),
    ),

    // --- Offline ----------------------------------------------------------

    NIGHT_SHIFT(
        id = "night_shift",
        price = 1_000_000.0,
        category = UpgradeCategory.OFFLINE,
        effect = UpgradeEffect.OfflineEfficiencyBonus(0.25),
        unlockCondition = UnlockCondition.LifetimeCoins(BigNumber.of(5_000_000)),
    ),
    DREAM_FACTORY(
        id = "dream_factory",
        price = 250_000_000.0,
        category = UpgradeCategory.OFFLINE,
        effect = UpgradeEffect.OfflineEfficiencyBonus(0.25),
        unlockCondition = UnlockCondition.LifetimeCoins(BigNumber.of(1_000_000_000)),
    );

    /** Preis als grosser Zahlentyp, wie ihn Kontostand und Kauf verwenden. */
    val priceAsBigNumber: BigNumber = BigNumber.of(price)

    companion object {

        /** Nachschlagetabelle, damit [fromId] nicht jedes Mal linear sucht. */
        private val byId: Map<String, UpgradeType> = entries.associateBy { it.id }

        /**
         * Sucht ein Upgrade anhand seines stabilen Schluessels.
         *
         * Liefert `null` bei unbekanntem Schluessel - etwa wenn der Spieler
         * nach einem Update auf eine aeltere App-Version zurueckwechselt.
         */
        fun fromId(id: String): UpgradeType? = byId[id]
    }
}
