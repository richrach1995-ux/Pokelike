import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import com.pokelike.game.*
import org.junit.Test
import java.io.File

/** Startet das echte Spiel, bedient es ueber die Tasten und macht Bildschirmfotos. */
class Spielen {

    private val W = 540
    private val H = 1140
    private val ordner = File(System.getProperty("pokelike.bilder") ?: "build/bilder")

    private lateinit var view: GameView
    private lateinit var bmp: Bitmap
    private lateinit var canvas: Canvas

    private fun start() {
        ordner.mkdirs()
        ordner.listFiles()?.forEach { it.delete() }
        view = GameView(Context())
        view.layoutFor(W, H)
        bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        canvas = Canvas(bmp)
    }

    private fun frames(n: Int, dt: Float = 1f / 60f) { repeat(n) { view.game.update(dt) } }

    private fun tap(b: Btn, halten: Int = 4) {
        view.game.input.set(b, true); frames(halten)
        view.game.input.set(b, false); frames(3)
    }

    private fun halten(b: Btn, n: Int) {
        view.game.input.set(b, true); frames(n)
        view.game.input.set(b, false); frames(3)
    }

    private fun schuss(name: String) {
        view.renderFrame(canvas)
        val f = File(ordner, "$name.png")
        f.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val farben = HashSet<Int>()
        for (y in 0 until H step 11) for (x in 0 until W step 9) farben.add(bmp.getPixel(x, y))
        println("BILD $name: ${f.length()} Bytes, ${farben.size} Farben")
        if (farben.size < 6) throw AssertionError("Bild $name wirkt leer (${farben.size} Farben)")
    }

    @Test
    fun spielDurchspielen() {
        start()
        frames(5)
        schuss("01-titel")

        // Neues Spiel
        tap(Btn.A)
        frames(70)
        println("Start: ${view.game.state.mapId} bei ${view.game.state.px},${view.game.state.py}")
        schuss("02-zuhause")

        // Aus dem Haus nach draussen
        halten(Btn.DOWN, 40)
        frames(50)
        println("Nach der Tuer: ${view.game.state.mapId} bei ${view.game.state.px},${view.game.state.py}")
        schuss("03-dorf")

        // Zum Labor laufen (Tuer links oben bei 4,4)
        halten(Btn.LEFT, 110)
        halten(Btn.UP, 90)
        frames(60)
        println("Beim Labor: ${view.game.state.mapId} bei ${view.game.state.px},${view.game.state.py}")
        schuss("04-labor-oder-dorf")

        // Menue und Typen-Info
        tap(Btn.START); frames(10)
        schuss("05-menue")
        tap(Btn.DOWN); tap(Btn.DOWN); tap(Btn.DOWN)
        tap(Btn.A); frames(10)
        schuss("06-typen-info")
        tap(Btn.B); frames(5); tap(Btn.B); frames(10)

        // Kampf mit echtem Team
        val g = view.game
        g.state.party.add(Monster("flamki", 12).apply { healFull() })
        g.state.party.add(Monster("aquino", 11).apply { healFull() })
        g.state.bag.add("fangball", 5)
        g.state.setFlag("starter")
        val wild = Monster("knospel", 11).apply { healFull() }
        val kampf = Battle(g.state.party, mutableListOf(wild), BattleKind.WILD)
        g.push(BattleScene(kampf) { println("Kampf beendet: ${it}") })
        frames(40)
        schuss("07-kampfbeginn")

        repeat(12) { tap(Btn.A); frames(20) }
        schuss("08-kampfmenue")

        tap(Btn.A); frames(12)           // KAMPF
        schuss("09-attackenauswahl")
        tap(Btn.A); frames(60)           // erste Attacke
        repeat(8) { tap(Btn.A); frames(25) }
        schuss("10-kampfverlauf")
        println("Gegner: ${wild.currentHp}/${wild.maxHp} KP, eigenes: ${kampf.activePlayer.name} ${kampf.activePlayer.currentHp}/${kampf.activePlayer.maxHp}")

        // Weiterkaempfen wie ein Spieler: bei erzwungenem Wechsel das gesunde Monster nehmen
        var schritte = 0
        while (!kampf.over && schritte++ < 300) {
            val oben = g.top()
            if (oben is PartyScene) {
                // Auswahlbildschirm nach einer Niederlage: gesundes Monster bestaetigen
                tap(Btn.A); frames(8)
            } else {
                tap(Btn.A); frames(8)
            }
        }
        frames(40)
        println("Ergebnis: ${kampf.result} nach $schritte Eingaben, Gegner ${wild.currentHp}/${wild.maxHp} KP")
        schuss("11-kampfende")
        if (kampf.result == BattleResult.NONE) throw AssertionError("Kampf blieb haengen")

        // Team- und Uebersichtsbildschirm
        while (g.top() !is OverworldScene) g.pop()
        g.push(PartyScene()); frames(10)
        schuss("12-team")
        tap(Btn.A); frames(6); tap(Btn.A); frames(12)
        schuss("13-monster-uebersicht")

        // Beutel
        while (g.top() !is OverworldScene) g.pop()
        g.push(BagScene(inBattle = false)); frames(10)
        schuss("14-beutel")

        println("Bilder liegen in $ordner")
    }
}
