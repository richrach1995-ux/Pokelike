import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import com.pokelike.game.*
import org.junit.Test
import java.io.File

/** Haelt gezielt das Attackenmenue und den Beutel im Kampf fest. */
class Attacken {
    @Test
    fun attackenmenue() {
        val w = 540; val h = 1140
        val ordner = File(System.getProperty("pokelike.bilder") ?: "build/bilder")
        val view = GameView(Context())
        view.layoutFor(w, h)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val g = view.game
        g.state.startNewGame()
        g.replaceAll(OverworldScene())
        val held = Monster("flammor", 26).apply { healFull() }
        g.state.party.add(held)
        g.state.party.add(Monster("aquino", 24).apply { healFull() })
        g.state.bag.add("fangball", 5); g.state.bag.add("trank", 3); g.state.bag.add("hyperball", 2)
        val wild = Monster("steinkopf", 24).apply { healFull() }
        val kampf = Battle(g.state.party, mutableListOf(wild), BattleKind.WILD)
        g.push(BattleScene(kampf) { })

        fun frames(n: Int) = repeat(n) { g.update(1f / 60f) }
        fun tap(b: Btn) { g.input.set(b, true); frames(4); g.input.set(b, false); frames(3) }
        fun schuss(name: String) {
            view.renderFrame(canvas)
            File(ordner, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
            println("BILD $name gespeichert")
        }

        // Meldungen laufen von selbst weiter - einfach warten, bis das Menue steht
        frames(900)
        schuss("20-kampfmenue")
        tap(Btn.A); frames(20)              // KAMPF -> Attackenliste
        schuss("21-attackenmenue")
        tap(Btn.DOWN); frames(10)
        schuss("21b-attacke-gewaehlt")
        tap(Btn.B); frames(20)              // zurueck ins Kampfmenue
        tap(Btn.RIGHT); frames(10)          // BEUTEL
        tap(Btn.A); frames(30)
        schuss("22-beutel-im-kampf")
        println("Attacken: " + held.moves.joinToString { "${it.move.name} ${it.pp}/${it.maxPp}" })
    }
}
