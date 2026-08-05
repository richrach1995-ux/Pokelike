package com.pokelike.idle.domain.model

/**
 * Die Stellen, an denen ein Belohnungsvideo angeboten wird.
 *
 * Deklarativ wie Gebaeude, Upgrades und Booster: Wartezeit und Belohnung
 * stehen am Eintrag, nicht als Fallunterscheidung im Ablauf.
 *
 * **Zur Wartezeit.** Sie ist kein Beiwerk, sondern das einzige, was die
 * Wirtschaft des Spiels vor der Werbung schuetzt. Ohne sie koennte sich ein
 * Spieler unbegrenzt Booster ansehen, und weder Diamanten noch Kaeufe haetten
 * noch einen Zweck. Fuenf Minuten begrenzen den Ertrag auf zwoelf Videos je
 * Stunde und bleiben trotzdem so kurz, dass das Angebot als Angebot wirkt und
 * nicht als Sperre.
 *
 * **Zur Belohnung.** Sie ist hier ein Booster, weil es bisher genau eine
 * Stelle gibt. Sobald eine Stelle etwas anderes gewaehrt - verdoppelter
 * Offline-Ertrag, ein Ticket, ein zweiter Anlauf bei einem Event -, gehoert an
 * diese Stelle ein eigener, geschlossener Belohnungstyp. Ihn jetzt schon
 * anzulegen hiesse, eine Hierarchie mit einer einzigen Auspraegung zu bauen.
 *
 * @property id Stabiler Schluessel fuer Spielstand und Analytics.
 * @property cooldownMillis Wartezeit bis zum naechsten Video an dieser Stelle.
 * @property rewardedBooster Booster, der nach dem Video startet.
 */
enum class RewardedAdPlacement(
    val id: String,
    val cooldownMillis: Long,
    val rewardedBooster: BoosterType,
) {

    /**
     * Der Gratis-Booster im Booster-Angebot.
     *
     * Belohnt wird bewusst der mittlere Booster und nicht der staerkste: Waere
     * das Video der beste Weg zum besten Booster, haette der Diamantenpreis
     * daneben keinen Sinn mehr.
     */
    BOOSTER_REWARD(
        id = "booster_reward",
        cooldownMillis = 5L * 60L * 1_000L,
        rewardedBooster = BoosterType.DOUBLE_INCOME,
    );

    companion object {

        private val byId: Map<String, RewardedAdPlacement> = entries.associateBy { it.id }

        /** Liefert `null` bei unbekanntem Schluessel. */
        fun fromId(id: String): RewardedAdPlacement? = byId[id]
    }
}
