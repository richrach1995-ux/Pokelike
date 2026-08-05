package com.pokelike.idle.ads

import com.pokelike.idle.domain.model.RewardedAdPlacement
import kotlinx.coroutines.flow.StateFlow

/**
 * Ausgang eines Videoversuchs.
 *
 * Bewusst vier unterschiedene Faelle statt eines `Boolean`. Sie fuehren zu
 * vier verschiedenen Reaktionen, und ein Wahrheitswert wuerde sie zu einer
 * einzigen zusammenziehen: Wer abbricht, hat nichts verdient, aber auch nichts
 * falsch gemacht - ihm eine Fehlermeldung zu zeigen waere unangebracht. Wer
 * kein Netz hat, soll genau das erfahren und nicht glauben, das Angebot sei
 * verschwunden.
 */
sealed interface AdResult {

    /** Das Video wurde zu Ende gesehen; die Belohnung steht zu. */
    data object EarnedReward : AdResult

    /**
     * Der Spieler hat vorzeitig abgebrochen.
     *
     * Kein Fehler und keine Meldung wert. Die Wartezeit beginnt hier
     * ausdruecklich **nicht**: Sie ist der Preis der Belohnung, nicht die
     * Strafe fuer einen Abbruch.
     */
    data object Dismissed : AdResult

    /** Es stand kein geladenes Video bereit. */
    data object NotReady : AdResult

    /**
     * Der Versuch ist gescheitert - kein Netz, keine Werbung verfuegbar,
     * Fehler im SDK.
     *
     * @property reason Kurzbeschreibung fuer das Protokoll. Nicht zur Anzeige
     *   gedacht: Der Wortlaut kommt aus dem SDK, ist unuebersetzt und fuer den
     *   Spieler bedeutungslos.
     */
    data class Failed(val reason: String) : AdResult
}

/**
 * Zugang zu Belohnungsvideos.
 *
 * Die Grenze zum Werbe-SDK und der einzige Ort, an dem es spaeter auftaucht.
 * Alles darueber - Wartezeit, Belohnung, Anzeige - kennt ausschliesslich diese
 * Schnittstelle. Das ist nicht Formsache: Werbevermittler werden gewechselt,
 * und ein SDK, das sich durch ViewModels und Use Cases zieht, macht daraus ein
 * Umbauprojekt statt eines Austauschs.
 *
 * Bewusst **ohne** `Activity` in der Signatur. Ein Werbe-SDK braucht zum
 * Anzeigen eine Activity; sie hier hereinzureichen wuerde jedoch bedeuten,
 * dass Manager und ViewModels eine Activity durchreichen muessen - und damit
 * genau die Kopplung entsteht, die diese Schnittstelle verhindern soll. Die
 * Beschaffung ist Sache der Umsetzung.
 *
 * [show] ist `suspend`, weil ein Video eine Handlung mit Anfang und Ende ist.
 * Als Rueckruf formuliert braeuchte jeder Aufrufer eine eigene
 * Zustandsverwaltung fuer "laeuft gerade".
 */
interface RewardedAdSource {

    /**
     * Ob fuer die jeweilige Stelle ein geladenes Video bereitsteht.
     *
     * Als Fluss und nicht als Abfrage: Das Laden dauert, und die Oberflaeche
     * soll den Knopf freigeben, sobald es abgeschlossen ist - ohne zu pollen.
     */
    val readyPlacements: StateFlow<Set<RewardedAdPlacement>>

    /**
     * Laedt ein Video im Voraus.
     *
     * Muss aufgerufen werden, bevor [show] Aussicht auf Erfolg hat. Ein Video
     * erst beim Antippen zu laden bedeutete mehrere Sekunden Wartezeit mit
     * offenem Ausgang - der Spieler haette getippt und saehe nichts.
     */
    suspend fun prepare(placement: RewardedAdPlacement)

    /**
     * Zeigt das geladene Video und kehrt zurueck, wenn es beendet ist.
     *
     * Die Umsetzung laedt anschliessend selbsttaetig nach, damit das naechste
     * Video bereitsteht, sobald die Wartezeit abgelaufen ist.
     */
    suspend fun show(placement: RewardedAdPlacement): AdResult
}
