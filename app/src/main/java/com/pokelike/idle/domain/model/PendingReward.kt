package com.pokelike.idle.domain.model

/**
 * Woher eine Belohnung stammt.
 *
 * Die Quelle wird als Typ gefuehrt und nicht als Text: Die Beschriftung
 * gehoert in die UI-Schicht, damit das Domaenenmodell frei von
 * Ressourcenverweisen bleibt und in einer reinen JVM-Umgebung uebersetzbar ist.
 *
 * Kuenftige Quellen - Login-Bonus, Events, Battle Pass - kommen als weitere
 * Auspraegungen hinzu und landen ohne weiteres Zutun in derselben
 * Benachrichtigung.
 */
sealed interface RewardSource {

    /** Eindeutiger Schluessel, damit dieselbe Belohnung nicht doppelt anliegt. */
    val id: String

    data class Achievement(val type: AchievementType) : RewardSource {
        override val id: String get() = "achievement:${type.id}"
    }

    data class Quest(val type: QuestType) : RewardSource {
        override val id: String get() = "quest:${type.id}"
    }
}

/**
 * Eine Belohnung, ueber die der Spieler noch benachrichtigt werden muss.
 *
 * Wichtig zum Verstaendnis: Der Betrag ist zu diesem Zeitpunkt **bereits
 * gutgeschrieben**. Diese Struktur ist eine Benachrichtigungsschlange, keine
 * Warteschlange fuer Auszahlungen.
 *
 * Der Unterschied ist keine Formsache: Wuerde erst beim Antippen des Popups
 * gutgeschrieben, ginge die Belohnung verloren, sobald die App zwischen
 * Freischaltung und Antippen beendet wird - und genau das passiert bei einem
 * Spiel, das im Hintergrund weiterlaeuft, staendig.
 *
 * @property source Ursprung der Belohnung.
 * @property bundle Was gutgeschrieben wurde.
 */
data class PendingReward(
    val source: RewardSource,
    val bundle: ResourceBundle,
) {
    val id: String get() = source.id
}
