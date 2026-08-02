package com.pokelike.game

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF

/**
 * Alle Grafiken des Spiels werden zur Laufzeit aus Pixel-Vorlagen erzeugt.
 * Dadurch braucht das Projekt keine Bilddateien.
 *
 * Zeichen der Vorlagen:
 *   .  transparent      o  Umriss (dunkles Primaer)
 *   p  Primaerfarbe     l  helles Primaer
 *   s  Sekundaerfarbe   d  dunkles Sekundaer
 *   w  weiss/Bauch      e  Augenweiss      b  Pupille
 */
object SpriteArt {

    const val SIZE = 16

    private fun t(vararg rows: String): Array<String> {
        val out = Array(SIZE) { "................" }
        for (i in 0 until minOf(SIZE, rows.size)) {
            var r = rows[i].replace(' ', '.')
            r = if (r.length >= SIZE) r.substring(0, SIZE) else r.padEnd(SIZE, '.')
            out[i] = r
        }
        return out
    }

    val monsterTemplates: Map<String, Array<String>> = mapOf(
        "QUAD" to t(
            "................",
            "..o..........o..",
            ".opo........opo.",
            ".oppooooooooppo.",
            "..oppppppppppo..",
            ".oppppppppppppo.",
            "opebppppppbeppo.",
            "opppwwwwwwwpppo.",
            "opppwwwwwwwpppo.",
            "oppppwwwwwppppo.",
            ".oppppppppppppo.",
            "..oppppppppppo..",
            "..o.o......o.o..",
            "..opo......opo..",
            "..ooo......ooo..",
            "................"
        ),
        "BIPED" to t(
            "................",
            ".....oooo.......",
            "....oppppo......",
            "...oppppppo.....",
            "...opebbepo.....",
            "...oppppppo.....",
            "....oppppo......",
            "..oosppppsoo....",
            ".opsspppppsspo..",
            ".opsopppppposo..",
            "...opppppppo....",
            "...oppo.oppo....",
            "...opo...opo....",
            "..ooo.....ooo...",
            "................",
            "................"
        ),
        "BIRD" to t(
            "................",
            "......oooo......",
            ".....oppppo.....",
            "....opppppppo...",
            "....opebppposs..",
            "....oppppppposs.",
            "...oppppppppo...",
            "..oppsssppppppo.",
            ".osppsssspppppo.",
            ".osppppsssppppo.",
            "..oppppppppppo..",
            "...oppppppppo...",
            "....o.o..o.o....",
            "...oo.o..o.oo...",
            "................",
            "................"
        ),
        "BUG" to t(
            "................",
            "................",
            "....oo....oo....",
            "...o..o..o..o...",
            "..ooppoooppoo...",
            ".oppppppppppppo.",
            "opebppppppbepo..",
            "opppppppppppppo.",
            "opppsspppsspppo.",
            ".oppppppppppppo.",
            "..oppppppppppo..",
            "...oo.oo.oo.o...",
            "................",
            "................",
            "................",
            "................"
        ),
        "COCOON" to t(
            "................",
            ".....oooo.......",
            "....oppppo......",
            "...oppssppo.....",
            "..opppsspppo....",
            "..opppppppppo...",
            "..opssppssppo...",
            "..opppppppppo...",
            "..opppssppppo...",
            "...opppppppo....",
            "....opppppo.....",
            ".....oooo.......",
            "................",
            "................",
            "................",
            "................"
        ),
        "MOTH" to t(
            "................",
            "..ss........ss..",
            ".ssss......ssss.",
            "ssssso.oo.osssss",
            "sssssoppposssss.",
            ".sssopebbepooss.",
            "..soppppppppos..",
            "..sopppppppppos.",
            ".ssoppppppppposs",
            "ssssoppppppposss",
            ".sss.opppppo.ss.",
            "..s...oppo....s.",
            "................",
            "................",
            "................",
            "................"
        ),
        "BLOB" to t(
            "................",
            "................",
            ".....oooo.......",
            "...ooppppoo.....",
            "..opppppppppo...",
            ".oppppppppppo...",
            ".opebppppbepo...",
            "opppppppppppppo.",
            "oppppppppppppppo",
            "opppsspppssppppo",
            "oppppppppppppppo",
            ".oppppppppppppo.",
            "..oooooooooooo..",
            "................",
            "................",
            "................"
        ),
        "ROCK" to t(
            "................",
            "................",
            "....oooooo......",
            "...opplllpo.....",
            "..oppppppppo....",
            ".opppplllpppo...",
            ".opebppppbeppo..",
            "oppppppppppppo..",
            "opppssppppsppo..",
            "oppppppppppppo..",
            "ooppppppppppoo..",
            ".oooooooooooo...",
            "................",
            "................",
            "................",
            "................"
        ),
        "GHOST" to t(
            "................",
            ".....oooo.......",
            "...oopppppoo....",
            "..oppppppppppo..",
            ".oppppppppppppo.",
            ".opebppppbeppppo",
            ".opppppppppppppo",
            ".opppppssspppppo",
            ".opppppppppppppo",
            "..opppppppppppo.",
            "..oppppppppppo..",
            "..o.oo.oo.oo.o..",
            "...o..o..o..o...",
            "................",
            "................",
            "................"
        ),
        "FISH" to t(
            "................",
            "..........s.....",
            ".....oooo.ss....",
            "...ooppppposs...",
            "..opppppppppos..",
            ".opebppppppppos.",
            ".opppppppppppos.",
            "oppwwwwpppppposs",
            "oppwwwwppppppos.",
            ".oppppppppppos..",
            "..ooosssoooo....",
            "....sss.........",
            "................",
            "................",
            "................",
            "................"
        ),
        "DRAGON" to t(
            "................",
            ".s............s.",
            ".sso........oss.",
            ".ssoo.oooo.osss.",
            ".ssoopppppooss..",
            "..sopebbbepoos..",
            "...opppppppppo..",
            "..osppppppppso..",
            ".osspppppppssso.",
            ".osppppppppppso.",
            "..oppppppppppo..",
            "...opo....opo...",
            "...oo......oo...",
            "................",
            "................",
            "................"
        ),
        "PLANT" to t(
            "................",
            "....ss..ss......",
            "...sssossso.....",
            "...ssoooosss....",
            "....oppppo......",
            "...opebbepo.....",
            "...opppppppo....",
            "..opppppppppo...",
            "..oppssppsspo...",
            "..opppppppppo...",
            "...opppppppo....",
            "....oppppo......",
            "....oo..oo......",
            "................",
            "................",
            "................"
        ),
        "SPARK" to t(
            "................",
            "...o.......o....",
            "..opo.....opo...",
            "..oppo...oppo...",
            "...oppooooppo...",
            "...opppppppppo..",
            "..opebpppbepo...",
            "..opppppppppo...",
            "..oppwwwwwppo...",
            "..opppppppppo...",
            "...opppppppo....",
            "...opo...opo....",
            "...oo.....oo....",
            "................",
            "................",
            "................"
        ),
        "ICE" to t(
            "................",
            ".......o........",
            "......olo.......",
            ".....ollllo.....",
            "....ollpllo.....",
            "...olppppplo....",
            "...olebppbelo...",
            "..olppppppplo...",
            "..olpppppppplo..",
            "..olppppppppplo.",
            "...olpppppppplo.",
            "....ollllllllo..",
            ".....oooooooo...",
            "................",
            "................",
            "................"
        )
    )

    // ------------------------------------------------------------------
    // Figuren der Oberwelt: h = Haare, f = Haut, b = Kleidung, p = Hose
    // ------------------------------------------------------------------
    val charDown = t(
        "................",
        ".....oooo.......",
        "....ohhhho......",
        "...ohhhhhho.....",
        "...offfffo......",
        "...ofefefo......",
        "...offfffo......",
        "....offfo.......",
        "...obbbbbo......",
        "..obbbbbbbo.....",
        "..obbbbbbbo.....",
        "..ofbbbbbfo.....",
        "...opppppo......",
        "...oppoppo......",
        "...ooo.ooo......",
        "................"
    )
    val charUp = t(
        "................",
        ".....oooo.......",
        "....ohhhho......",
        "...ohhhhhho.....",
        "...ohhhhhho.....",
        "...ohhhhho......",
        "...ohhhhho......",
        "....ohhho.......",
        "...obbbbbo......",
        "..obbbbbbbo.....",
        "..obbbbbbbo.....",
        "..ofbbbbbfo.....",
        "...opppppo......",
        "...oppoppo......",
        "...ooo.ooo......",
        "................"
    )
    val charSide = t(
        "................",
        ".....oooo.......",
        "....ohhhho......",
        "...ohhhhhho.....",
        "...ohffffo......",
        "...ohffefo......",
        "...ohffffo......",
        "....offfo.......",
        "...obbbbo.......",
        "..obbbbbbo......",
        "..obbbbbbo......",
        "...obbbbo.......",
        "...oppppo.......",
        "...oppo.o.......",
        "...ooo..........",
        "................"
    )
}

/** Farbhilfen. */
object Col {
    fun shade(c: Int, f: Float): Int {
        val a = Color.alpha(c)
        val r = (Color.red(c) * f).toInt().coerceIn(0, 255)
        val g = (Color.green(c) * f).toInt().coerceIn(0, 255)
        val b = (Color.blue(c) * f).toInt().coerceIn(0, 255)
        return Color.argb(a, r, g, b)
    }

    fun lighten(c: Int, f: Float): Int {
        val a = Color.alpha(c)
        val r = (Color.red(c) + (255 - Color.red(c)) * f).toInt().coerceIn(0, 255)
        val g = (Color.green(c) + (255 - Color.green(c)) * f).toInt().coerceIn(0, 255)
        val b = (Color.blue(c) + (255 - Color.blue(c)) * f).toInt().coerceIn(0, 255)
        return Color.argb(a, r, g, b)
    }

    fun mix(a: Int, b: Int, t: Float): Int {
        val ia = 1f - t
        return Color.argb(
            255,
            (Color.red(a) * ia + Color.red(b) * t).toInt().coerceIn(0, 255),
            (Color.green(a) * ia + Color.green(b) * t).toInt().coerceIn(0, 255),
            (Color.blue(a) * ia + Color.blue(b) * t).toInt().coerceIn(0, 255)
        )
    }
}

/** Erzeugt und cached die Bitmaps. */
object Sprites {

    private val cache = HashMap<String, Bitmap>()
    private val paint = Paint().apply { isFilterBitmap = false; isAntiAlias = false }
    private val srcRect = Rect(0, 0, SpriteArt.SIZE, SpriteArt.SIZE)

    private fun build(rows: Array<String>, palette: Map<Char, Int>): Bitmap {
        val bmp = Bitmap.createBitmap(SpriteArt.SIZE, SpriteArt.SIZE, Bitmap.Config.ARGB_8888)
        for (y in 0 until SpriteArt.SIZE) {
            val row = rows[y]
            for (x in 0 until SpriteArt.SIZE) {
                val c = row[x]
                val col = palette[c] ?: 0
                bmp.setPixel(x, y, col)
            }
        }
        return bmp
    }

    fun monster(speciesId: String): Bitmap {
        val key = "m:$speciesId"
        cache[key]?.let { return it }
        val sp = Dex.get(speciesId)
        val rows = SpriteArt.monsterTemplates[sp.sprite] ?: SpriteArt.monsterTemplates.getValue("BLOB")
        val palette = mapOf(
            '.' to 0,
            'o' to Col.shade(sp.colorA, 0.42f),
            'p' to sp.colorA,
            'l' to Col.lighten(sp.colorA, 0.35f),
            's' to sp.colorB,
            'd' to Col.shade(sp.colorB, 0.5f),
            'w' to 0xFFF8F8F0.toInt(),
            'e' to 0xFFFFFFFF.toInt(),
            'b' to 0xFF201820.toInt()
        )
        val bmp = build(rows, palette)
        cache[key] = bmp
        return bmp
    }

    /** Figur der Oberwelt. dir: 0 unten, 1 oben, 2 links, 3 rechts. */
    fun character(styleId: String, dir: Int, hair: Int, shirt: Int, pants: Int, skin: Int): Bitmap {
        val key = "c:$styleId:$dir"
        cache[key]?.let { return it }
        val rows = when (dir) {
            1 -> SpriteArt.charUp
            2, 3 -> SpriteArt.charSide
            else -> SpriteArt.charDown
        }
        val palette = mapOf(
            '.' to 0,
            'o' to 0xFF201828.toInt(),
            'h' to hair,
            'f' to skin,
            'b' to shirt,
            'p' to pants,
            'e' to 0xFF201828.toInt()
        )
        var bmp = build(rows, palette)
        if (dir == 2) bmp = mirror(bmp)
        cache[key] = bmp
        return bmp
    }

    private fun mirror(src: Bitmap): Bitmap {
        val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        for (y in 0 until src.height) for (x in 0 until src.width) {
            out.setPixel(src.width - 1 - x, y, src.getPixel(x, y))
        }
        return out
    }

    fun draw(c: Canvas, bmp: Bitmap, x: Float, y: Float, size: Float) {
        val dst = RectF(x, y, x + size, y + size)
        c.drawBitmap(bmp, srcRect, dst, paint)
    }

    /** Zeichnet ein Monster in eine Flaeche, optional gespiegelt und eingefaerbt. */
    fun drawMonster(c: Canvas, speciesId: String, x: Float, y: Float, size: Float, flip: Boolean = false, alpha: Int = 255) {
        val bmp = monster(speciesId)
        paint.alpha = alpha
        if (flip) {
            c.save()
            c.translate(x + size, y)
            c.scale(-1f, 1f)
            c.drawBitmap(bmp, srcRect, RectF(0f, 0f, size, size), paint)
            c.restore()
        } else {
            c.drawBitmap(bmp, srcRect, RectF(x, y, x + size, y + size), paint)
        }
        paint.alpha = 255
    }
}
