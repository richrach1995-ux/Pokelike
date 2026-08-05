package com.pokelike.idle.domain.model

import com.pokelike.idle.config.GameConfig

/**
 * Alle Booster des Spiels.
 *
 * **Warum die Wirkung ein [UpgradeEffect] ist.** Ein Booster ist inhaltlich
 * nichts anderes als ein Upgrade auf Zeit. Die Wirkungsarten werden bereits in
 * [com.pokelike.idle.domain.usecases.CalculateModifiersUseCase] ausgewertet,
 * und dieselbe Auswertung noch einmal fuer Booster zu schreiben hiesse, zwei
 * Kopien derselben Balancing-Regeln zu fuehren. So genuegt es, die Wirkungen
 * der aktiven Booster in denselben Topf zu geben - eine neue Wirkungsart wirkt
 * damit ohne weiteres Zutun in beiden Systemen.
 *
 * Bezahlt wird in Diamanten. Ab dem Werbeschritt kommt derselbe Booster als
 * Belohnung fuer ein Video hinzu; er laeuft dann ueber denselben Use Case und
 * unterscheidet sich nur darin, dass nichts abgebucht wird.
 *
 * @property id Stabiler Schluessel fuer Spielstand und Analytics.
 * @property effect Wirkung, solange der Booster laeuft.
 * @property durationMillis Grundlaufzeit.
 * @property price Preis eines Starts.
 */
enum class BoosterType(
    val id: String,
    val effect: UpgradeEffect,
    val durationMillis: Long,
    val price: ResourceBundle,
) {

    /** Verdoppelt den Klickertrag. Der Booster fuer eine aktive Sitzung. */
    DOUBLE_CLICKS(
        id = "double_clicks",
        effect = UpgradeEffect.ClickMultiplier(2.0),
        durationMillis = 15L * MILLIS_PER_MINUTE,
        price = diamonds(20),
    ),

    /** Verdoppelt das Leerlaufeinkommen. */
    DOUBLE_INCOME(
        id = "double_income",
        effect = UpgradeEffect.IncomeMultiplier(2.0),
        durationMillis = 30L * MILLIS_PER_MINUTE,
        price = diamonds(30),
    ),

    /**
     * Erhoeht die Kritchance deutlich.
     *
     * Die lange Laufzeit ist Absicht: Kritische Treffer wirken nur ueber viele
     * Klicks hinweg spuerbar. Bei fuenf Minuten waere der Unterschied blosses
     * Rauschen, und der Spieler haette den Eindruck, nichts gekauft zu haben.
     */
    LUCKY_HOUR(
        id = "lucky_hour",
        effect = UpgradeEffect.CriticalChanceBonus(0.20),
        durationMillis = 60L * MILLIS_PER_MINUTE,
        price = diamonds(40),
    ),

    /**
     * Sehr starker, sehr kurzer Einkommensschub.
     *
     * Der Gegenentwurf zu den uebrigen: Er belohnt es, gerade jetzt am Geraet
     * zu sein, und ist deshalb der Booster, der sich spaeter am besten als
     * Belohnung fuer ein Werbevideo eignet.
     */
    GOLD_RUSH(
        id = "gold_rush",
        effect = UpgradeEffect.IncomeMultiplier(5.0),
        durationMillis = 5L * MILLIS_PER_MINUTE,
        price = diamonds(50),
    );

    /** Hoechste Restlaufzeit, die sich durch Stapeln erreichen laesst. */
    val maxStackedDurationMillis: Long
        get() = durationMillis * GameConfig.BOOSTER_MAX_STACK_FACTOR

    companion object {

        private val byId: Map<String, BoosterType> = entries.associateBy { it.id }

        /** Liefert `null` bei unbekanntem Schluessel. */
        fun fromId(id: String): BoosterType? = byId[id]
    }
}

private const val MILLIS_PER_MINUTE = 60L * 1_000L

/**
 * Kurzform fuer einen Diamantenpreis.
 *
 * Nur innerhalb dieser Datei sichtbar: Sie existiert allein, damit die
 * Aufzaehlung oben lesbar bleibt.
 */
private fun diamonds(amount: Long): ResourceBundle =
    ResourceBundle.single(ResourceType.DIAMONDS, BigNumber.of(amount))
