import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import com.pokelike.game.*
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.math.abs

/**
 * Prueft am fertig gezeichneten Bild, ob der KP-Balken dem echten KP-Stand folgt.
 */
class Lebensbalken {

    private val W = 540
    private val H = 1140
    private val skala = W / 240f

    private val hpFarben = setOf(0xFF48C858.toInt(), 0xFFF0C020.toInt(), 0xFFE04030.toInt())

    /** Breite des farbigen Teils eines KP-Balkens in Pixeln. */
    private fun balkenBreite(bmp: Bitmap, yVirtuell: Float): Int {
        val y = (yVirtuell * skala).toInt()
        var n = 0
        for (x in 0 until W) if (bmp.getPixel(x, y) in hpFarben) n++
        return n
    }

    @Test
    fun balkenFolgtDenEchtenKp() {
        val ordner = File(System.getProperty("pokelike.bilder") ?: "build/bilder")
        val view = GameView(Context())
        view.layoutFor(W, H)
        val bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val g = view.game
        g.state.startNewGame()
        g.replaceAll(OverworldScene())

        val held = Monster("flammor", 30).apply {
            healFull(); moves = mutableListOf(MoveSlot.of("flammenwurf"))
        }
        g.state.party.add(held)
        val wild = Monster("steinkopf", 22).apply {
            healFull(); moves = mutableListOf(MoveSlot.of("steinwurf"))
        }
        val kampf = Battle(g.state.party, mutableListOf(wild), BattleKind.WILD)
        g.push(BattleScene(kampf) { })

        fun frames(n: Int) = repeat(n) { g.update(1f / 60f) }
        fun zeichne() { view.renderFrame(canvas) }
        fun tap(b: Btn) { g.input.set(b, true); frames(4); g.input.set(b, false); frames(3) }
        fun sichern(name: String) =
            File(ordner, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }

        // Einleitung abwarten, bis das Kampfmenue steht
        frames(900)
        zeichne()
        val gegnerVoll = balkenBreite(bmp, 31f)
        val eigenVoll = balkenBreite(bmp, view.game.worldH - 118f + 21f)
        sichern("30-balken-voll")
        println("Balken bei vollen KP: Gegner=$gegnerVoll px, eigenes=$eigenVoll px")
        assertTrue("Gegnerbalken nicht sichtbar", gegnerVoll > 200)
        assertTrue("Eigener Balken nicht sichtbar", eigenVoll > 200)

        // Angreifen und warten, bis sich die Balken nicht mehr bewegen
        tap(Btn.A)          // KAMPF
        tap(Btn.A)          // erste Attacke
        // Meldungen laufen von selbst weiter (1,5 s pro Zeile) - grosszuegig warten,
        // danach pruefen, dass sich wirklich nichts mehr bewegt.
        frames(2400)
        zeichne()
        var vorher = balkenBreite(bmp, 31f) + balkenBreite(bmp, view.game.worldH - 118f + 21f)
        var runden = 0
        while (runden++ < 20) {
            frames(120)
            zeichne()
            val jetzt = balkenBreite(bmp, 31f) + balkenBreite(bmp, view.game.worldH - 118f + 21f)
            if (jetzt == vorher) break
            vorher = jetzt
        }
        sichern("31-balken-nach-treffer")
        val gegnerNach = balkenBreite(bmp, 31f)
        val echterAnteil = wild.currentHp.toFloat() / wild.maxHp
        val gezeigterAnteil = gegnerNach.toFloat() / gegnerVoll
        println("Gegner nach Treffer: ${wild.currentHp}/${wild.maxHp} KP " +
                "-> echt ${(echterAnteil * 100).toInt()}%, angezeigt ${(gezeigterAnteil * 100).toInt()}%")

        assertTrue("Der Gegnerbalken hat sich nicht bewegt", gegnerNach < gegnerVoll - 10)
        assertTrue("Balken passt nicht zu den echten KP (echt ${echterAnteil}, gezeigt ${gezeigterAnteil})",
            abs(echterAnteil - gezeigterAnteil) < 0.12f)

        // Auch der eigene Balken muss sinken, wenn der Gegner trifft
        val eigenNach = balkenBreite(bmp, view.game.worldH - 118f + 21f)
        val eigenEcht = held.currentHp.toFloat() / held.maxHp
        println("Eigenes Monster: ${held.currentHp}/${held.maxHp} KP, Balken $eigenNach px von $eigenVoll px")
        if (held.currentHp < held.maxHp) {
            assertTrue("Eigener Balken folgt den KP nicht",
                abs(eigenEcht - eigenNach.toFloat() / eigenVoll) < 0.12f)
        }
    }
}
