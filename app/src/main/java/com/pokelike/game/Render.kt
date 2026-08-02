package com.pokelike.game

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface

/** Zeichenhilfen fuer Kacheln, Figuren und Oberflaeche. */
object Gfx {

    val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    }

    const val WHITE = 0xFFFFFFFF.toInt()
    const val BLACK = 0xFF101018.toInt()
    const val UI_BG = 0xFFF8F8F0.toInt()
    const val UI_BORDER = 0xFF283048.toInt()
    const val UI_SHADOW = 0xFFA0A8B8.toInt()
    const val ACCENT = 0xFFD03828.toInt()

    // ---------------------------------------------------------------- Text
    fun textWidth(s: String, size: Float): Float {
        tp.textSize = size
        return tp.measureText(s)
    }

    fun text(c: Canvas, s: String, x: Float, y: Float, size: Float, color: Int = BLACK, center: Boolean = false, right: Boolean = false) {
        tp.textSize = size
        tp.color = color
        val w = tp.measureText(s)
        val dx = when {
            center -> x - w / 2f
            right -> x - w
            else -> x
        }
        c.drawText(s, dx, y, tp)
    }

    fun textShadow(c: Canvas, s: String, x: Float, y: Float, size: Float, color: Int = WHITE, center: Boolean = false) {
        text(c, s, x + 1f, y + 1f, size, 0xC0000000.toInt(), center)
        text(c, s, x, y, size, color, center)
    }

    /** Bricht Text auf eine Zeichenbreite um. */
    fun wrap(s: String, maxChars: Int): List<String> {
        val out = mutableListOf<String>()
        var line = StringBuilder()
        for (word in s.split(" ")) {
            if (line.isEmpty()) line.append(word)
            else if (line.length + 1 + word.length <= maxChars) line.append(' ').append(word)
            else { out.add(line.toString()); line = StringBuilder(word) }
        }
        if (line.isNotEmpty()) out.add(line.toString())
        return out
    }

    // ---------------------------------------------------------------- Boxen
    fun panel(c: Canvas, x: Float, y: Float, w: Float, h: Float, bg: Int = UI_BG, border: Int = UI_BORDER) {
        p.style = Paint.Style.FILL
        p.color = 0x40000000
        c.drawRoundRect(RectF(x + 2, y + 3, x + w + 2, y + h + 3), 5f, 5f, p)
        p.color = bg
        c.drawRoundRect(RectF(x, y, x + w, y + h), 5f, 5f, p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 2f
        p.color = border
        c.drawRoundRect(RectF(x + 1, y + 1, x + w - 1, y + h - 1), 5f, 5f, p)
        p.style = Paint.Style.FILL
    }

    fun fillRect(c: Canvas, x: Float, y: Float, w: Float, h: Float, color: Int) {
        p.style = Paint.Style.FILL
        p.color = color
        c.drawRect(x, y, x + w, y + h, p)
    }

    fun strokeRect(c: Canvas, x: Float, y: Float, w: Float, h: Float, color: Int, sw: Float = 1.5f) {
        p.style = Paint.Style.STROKE
        p.strokeWidth = sw
        p.color = color
        c.drawRect(x, y, x + w, y + h, p)
        p.style = Paint.Style.FILL
    }

    /** Auswahlpfeil. */
    fun cursor(c: Canvas, x: Float, y: Float, size: Float = 7f, color: Int = ACCENT) {
        p.color = color
        p.style = Paint.Style.FILL
        val path = android.graphics.Path()
        path.moveTo(x, y - size / 2)
        path.lineTo(x + size * 0.8f, y)
        path.lineTo(x, y + size / 2)
        path.close()
        c.drawPath(path, p)
    }

    fun hpBar(c: Canvas, x: Float, y: Float, w: Float, h: Float, ratio: Float) {
        fillRect(c, x, y, w, h, 0xFF303848.toInt())
        val r = ratio.coerceIn(0f, 1f)
        val col = when {
            r > 0.5f -> 0xFF48C858.toInt()
            r > 0.2f -> 0xFFF0C020.toInt()
            else -> 0xFFE04030.toInt()
        }
        fillRect(c, x + 1, y + 1, (w - 2) * r, h - 2, col)
    }

    fun expBar(c: Canvas, x: Float, y: Float, w: Float, h: Float, ratio: Float) {
        fillRect(c, x, y, w, h, 0xFF303848.toInt())
        fillRect(c, x + 1, y + 1, (w - 2) * ratio.coerceIn(0f, 1f), h - 2, 0xFF48A8F0.toInt())
    }

    fun typeBadge(c: Canvas, t: Type, x: Float, y: Float, size: Float = 8f) {
        val label = t.deName.uppercase()
        val w = textWidth(label, size) + 8f
        p.color = t.color
        p.style = Paint.Style.FILL
        c.drawRoundRect(RectF(x, y, x + w, y + size + 5f), 3f, 3f, p)
        text(c, label, x + 4f, y + size + 0.5f, size, WHITE)
    }

    fun statusTag(c: Canvas, st: StatusKind, x: Float, y: Float, size: Float = 8f) {
        if (st == StatusKind.NONE) return
        val col = when (st) {
            StatusKind.GIFT -> 0xFFA040A0.toInt()
            StatusKind.BRAND -> 0xFFE06020.toInt()
            StatusKind.PARALYSE -> 0xFFD8B818.toInt()
            StatusKind.SCHLAF -> 0xFF7080A0.toInt()
            StatusKind.FROST -> 0xFF60B8E0.toInt()
            else -> 0xFF808080.toInt()
        }
        val w = textWidth(st.short, size) + 6f
        p.color = col
        c.drawRoundRect(RectF(x, y, x + w, y + size + 4f), 2f, 2f, p)
        text(c, st.short, x + 3f, y + size + 0.5f, size, WHITE)
    }

    // ---------------------------------------------------------------- Kacheln
    private fun hash(x: Int, y: Int): Int {
        var h = x * 374761393 + y * 668265263
        h = (h xor (h shr 13)) * 1274126177
        return h xor (h shr 16)
    }

    private fun rnd(x: Int, y: Int, i: Int): Float {
        val h = hash(x * 31 + i, y * 17 - i)
        return ((h and 0xFFFF) / 65535f)
    }

    fun drawTile(c: Canvas, ch: Char, x: Int, y: Int, sx: Float, sy: Float, s: Float, tick: Int, theme: String) {
        when (ch) {
            '.' -> {
                fillRect(c, sx, sy, s, s, themeGround(theme))
                p.color = Col.shade(themeGround(theme), 0.93f)
                for (i in 0 until 3) {
                    val rx = sx + rnd(x, y, i) * (s - 2f)
                    val ry = sy + rnd(x, y, i + 5) * (s - 2f)
                    c.drawRect(rx, ry, rx + 1.5f, ry + 1.5f, p)
                }
            }
            ',' -> {
                fillRect(c, sx, sy, s, s, 0xFF74C060.toInt())
                p.color = 0xFF5CA84A.toInt()
                for (i in 0 until 4) {
                    val rx = sx + rnd(x, y, i) * (s - 3f)
                    val ry = sy + rnd(x, y, i + 3) * (s - 3f)
                    c.drawRect(rx, ry + 1f, rx + 2f, ry + 3f, p)
                }
            }
            'g' -> {
                fillRect(c, sx, sy, s, s, 0xFF3E9A44.toInt())
                p.color = 0xFF2E7A34.toInt()
                for (i in 0 until 5) {
                    val rx = sx + rnd(x, y, i) * (s - 3f)
                    val ry = sy + rnd(x, y, i + 7) * (s - 6f) + 2f
                    c.drawRect(rx, ry, rx + 2f, ry + 5f, p)
                }
                p.color = 0xFF6CC060.toInt()
                c.drawRect(sx + 2f, sy + s - 4f, sx + s - 3f, sy + s - 3f, p)
            }
            '#' -> {
                fillRect(c, sx, sy, s, s, 0xFF2E7A34.toInt())
                p.color = 0xFF6B4A28.toInt()
                c.drawRect(sx + s * 0.42f, sy + s * 0.62f, sx + s * 0.6f, sy + s, p)
                p.color = 0xFF1E6428.toInt()
                c.drawCircle(sx + s / 2f, sy + s * 0.42f, s * 0.42f, p)
                p.color = 0xFF34A040.toInt()
                c.drawCircle(sx + s * 0.4f, sy + s * 0.36f, s * 0.26f, p)
            }
            '~' -> {
                val t = (tick / 12) % 2
                fillRect(c, sx, sy, s, s, 0xFF3878C8.toInt())
                p.color = 0xFF60A8E8.toInt()
                val off = if (t == 0) 0f else s * 0.25f
                c.drawRect(sx + off, sy + s * 0.25f, sx + off + s * 0.4f, sy + s * 0.25f + 2f, p)
                c.drawRect(sx + s * 0.5f - off, sy + s * 0.65f, sx + s * 0.5f - off + s * 0.35f, sy + s * 0.65f + 2f, p)
            }
            '^' -> {
                fillRect(c, sx, sy, s, s, 0xFF706860.toInt())
                p.color = 0xFF585048.toInt()
                c.drawRect(sx, sy + s * 0.5f, sx + s, sy + s * 0.5f + 1.5f, p)
                c.drawRect(sx + s * 0.5f, sy, sx + s * 0.5f + 1.5f, sy + s * 0.5f, p)
                p.color = 0xFF8A8078.toInt()
                c.drawRect(sx + 1f, sy + 1f, sx + s * 0.45f, sy + s * 0.45f, p)
            }
            '_' -> {
                fillRect(c, sx, sy, s, s, 0xFFE0CE96.toInt())
                p.color = 0xFFC8B47C.toInt()
                for (i in 0 until 3) {
                    val rx = sx + rnd(x, y, i) * (s - 2f)
                    val ry = sy + rnd(x, y, i + 2) * (s - 2f)
                    c.drawRect(rx, ry, rx + 1.5f, ry + 1.5f, p)
                }
            }
            'F' -> {
                fillRect(c, sx, sy, s, s, 0xFF74C060.toInt())
                val col = when ((hash(x, y) and 3)) {
                    0 -> 0xFFE85878.toInt(); 1 -> 0xFFF0D040.toInt(); 2 -> 0xFFE8E8F0.toInt(); else -> 0xFFB868E0.toInt()
                }
                p.color = col
                c.drawCircle(sx + s * 0.35f, sy + s * 0.4f, s * 0.12f, p)
                c.drawCircle(sx + s * 0.68f, sy + s * 0.62f, s * 0.12f, p)
            }
            'G' -> {
                fillRect(c, sx, sy, s, s, themeGround(theme))
                p.color = 0xFF8A6A40.toInt()
                c.drawRect(sx + 1f, sy + s * 0.3f, sx + s - 1f, sy + s * 0.42f, p)
                c.drawRect(sx + 1f, sy + s * 0.62f, sx + s - 1f, sy + s * 0.74f, p)
                c.drawRect(sx + s * 0.2f, sy + s * 0.2f, sx + s * 0.32f, sy + s, p)
            }
            'B' -> {
                fillRect(c, sx, sy, s, s, 0xFFD8C0A0.toInt())
                p.color = 0xFFB89878.toInt()
                c.drawRect(sx, sy + s * 0.45f, sx + s, sy + s * 0.5f, p)
                c.drawRect(sx + s * 0.45f, sy, sx + s * 0.5f, sy + s * 0.45f, p)
                c.drawRect(sx + s * 0.2f, sy + s * 0.5f, sx + s * 0.25f, sy + s, p)
            }
            'R' -> {
                fillRect(c, sx, sy, s, s, 0xFFC04848.toInt())
                p.color = 0xFF982E2E.toInt()
                c.drawRect(sx, sy + s * 0.55f, sx + s, sy + s * 0.62f, p)
                c.drawRect(sx, sy, sx + s, sy + s * 0.08f, p)
            }
            'D' -> {
                fillRect(c, sx, sy, s, s, 0xFFD8C0A0.toInt())
                p.color = 0xFF7A4A28.toInt()
                c.drawRect(sx + s * 0.16f, sy + s * 0.12f, sx + s * 0.84f, sy + s, p)
                p.color = 0xFFF0D060.toInt()
                c.drawCircle(sx + s * 0.7f, sy + s * 0.58f, s * 0.07f, p)
            }
            'W' -> {
                fillRect(c, sx, sy, s, s, 0xFFD8C0A0.toInt())
                p.color = 0xFF58A8D8.toInt()
                c.drawRect(sx + s * 0.18f, sy + s * 0.2f, sx + s * 0.82f, sy + s * 0.72f, p)
                p.color = 0xFFA8D8F0.toInt()
                c.drawRect(sx + s * 0.22f, sy + s * 0.24f, sx + s * 0.46f, sy + s * 0.44f, p)
            }
            'S' -> {
                fillRect(c, sx, sy, s, s, themeGround(theme))
                p.color = 0xFF6B4A28.toInt()
                c.drawRect(sx + s * 0.44f, sy + s * 0.5f, sx + s * 0.56f, sy + s, p)
                p.color = 0xFFC89858.toInt()
                c.drawRect(sx + s * 0.12f, sy + s * 0.14f, sx + s * 0.88f, sy + s * 0.56f, p)
                p.color = 0xFF6B4A28.toInt()
                c.drawRect(sx + s * 0.2f, sy + s * 0.24f, sx + s * 0.8f, sy + s * 0.3f, p)
                c.drawRect(sx + s * 0.2f, sy + s * 0.38f, sx + s * 0.66f, sy + s * 0.44f, p)
            }
            'H' -> {
                fillRect(c, sx, sy, s, s, 0xFFE8E8F0.toInt())
                p.color = 0xFFB8C0D0.toInt()
                c.drawRect(sx, sy, sx + s, sy + s * 0.18f, p)
                p.color = 0xFFE04040.toInt()
                c.drawRect(sx + s * 0.42f, sy + s * 0.3f, sx + s * 0.58f, sy + s * 0.8f, p)
                c.drawRect(sx + s * 0.22f, sy + s * 0.48f, sx + s * 0.78f, sy + s * 0.62f, p)
                if ((tick / 20) % 2 == 0) {
                    p.color = 0xFF60E060.toInt()
                    c.drawCircle(sx + s * 0.15f, sy + s * 0.12f, s * 0.06f, p)
                }
            }
            'T' -> {
                fillRect(c, sx, sy, s, s, 0xFFB08040.toInt())
                p.color = 0xFF8A6030.toInt()
                c.drawRect(sx, sy + s * 0.3f, sx + s, sy + s * 0.38f, p)
                p.color = 0xFFD8B070.toInt()
                c.drawRect(sx, sy, sx + s, sy + s * 0.22f, p)
            }
            'C' -> {
                fillRect(c, sx, sy, s, s, 0xFF6A6478.toInt())
                p.color = 0xFF585268.toInt()
                for (i in 0 until 3) {
                    val rx = sx + rnd(x, y, i) * (s - 3f)
                    val ry = sy + rnd(x, y, i + 4) * (s - 3f)
                    c.drawRect(rx, ry, rx + 2f, ry + 2f, p)
                }
            }
            'X' -> {
                fillRect(c, sx, sy, s, s, 0xFF6A6478.toInt())
                p.color = 0xFF908A9C.toInt()
                c.drawCircle(sx + s / 2, sy + s / 2, s * 0.4f, p)
                p.color = 0xFFB0AABC.toInt()
                c.drawCircle(sx + s * 0.38f, sy + s * 0.38f, s * 0.16f, p)
            }
            'I' -> {
                fillRect(c, sx, sy, s, s, 0xFFC8ECF8.toInt())
                p.color = 0xFFFFFFFF.toInt()
                c.drawRect(sx + s * 0.1f, sy + s * 0.18f, sx + s * 0.5f, sy + s * 0.26f, p)
                p.color = 0xFFA8D8EC.toInt()
                c.drawRect(sx + s * 0.5f, sy + s * 0.6f, sx + s * 0.9f, sy + s * 0.68f, p)
            }
            'L' -> {
                val gl = if ((tick / 15) % 2 == 0) 1.0f else 0.85f
                fillRect(c, sx, sy, s, s, Col.shade(0xFFE85818.toInt(), gl))
                p.color = 0xFFF8C030.toInt()
                c.drawCircle(sx + s * 0.35f, sy + s * 0.4f, s * 0.16f, p)
                c.drawCircle(sx + s * 0.66f, sy + s * 0.68f, s * 0.12f, p)
            }
            'V' -> {
                fillRect(c, sx, sy, s, s, 0xFF4A3E3A.toInt())
                p.color = 0xFF6A5A50.toInt()
                c.drawCircle(sx + s / 2, sy + s / 2, s * 0.38f, p)
                p.color = 0xFFE87838.toInt()
                c.drawRect(sx + s * 0.4f, sy + s * 0.44f, sx + s * 0.6f, sy + s * 0.5f, p)
            }
            'o' -> {
                fillRect(c, sx, sy, s, s, 0xFFD8B888.toInt())
                p.color = 0xFFC0A070.toInt()
                c.drawRect(sx, sy + s - 1.5f, sx + s, sy + s, p)
                c.drawRect(sx + s - 1.5f, sy, sx + s, sy + s, p)
            }
            'w' -> {
                fillRect(c, sx, sy, s, s, 0xFF8898B8.toInt())
                p.color = 0xFF6A7A9A.toInt()
                c.drawRect(sx, sy + s * 0.5f, sx + s, sy + s * 0.56f, p)
                c.drawRect(sx + s * 0.5f, sy, sx + s * 0.56f, sy + s * 0.5f, p)
            }
            'b' -> {
                fillRect(c, sx, sy, s, s, 0xFF7A5230.toInt())
                for (i in 0 until 4) {
                    p.color = intArrayOf(0xFFE05050.toInt(), 0xFF50A0E0.toInt(), 0xFF60C060.toInt(), 0xFFE0C050.toInt())[i]
                    c.drawRect(sx + 1f + i * (s - 2f) / 4f, sy + s * 0.18f, sx + 1f + (i + 0.7f) * (s - 2f) / 4f, sy + s * 0.55f, p)
                }
                p.color = 0xFF5A3A20.toInt()
                c.drawRect(sx, sy + s * 0.58f, sx + s, sy + s * 0.66f, p)
            }
            '=' -> {
                fillRect(c, sx, sy, s, s, 0xFFC85868.toInt())
                p.color = 0xFFE07888.toInt()
                c.drawRect(sx + 1f, sy + 1f, sx + s - 1f, sy + s - 1f, p)
                p.color = 0xFFC85868.toInt()
                c.drawRect(sx + 3f, sy + 3f, sx + s - 3f, sy + s - 3f, p)
            }
            'A' -> {
                val base = themeArena(theme)
                fillRect(c, sx, sy, s, s, if ((x + y) % 2 == 0) base else Col.shade(base, 0.92f))
                p.color = Col.shade(base, 0.85f)
                c.drawRect(sx, sy, sx + s, sy + 1f, p)
            }
            'M' -> {
                fillRect(c, sx, sy, s, s, themeGround(theme))
                p.color = 0xFF9098A8.toInt()
                c.drawRect(sx + s * 0.24f, sy + s * 0.1f, sx + s * 0.76f, sy + s, p)
                p.color = 0xFFB8C0D0.toInt()
                c.drawRect(sx + s * 0.3f, sy + s * 0.16f, sx + s * 0.52f, sy + s * 0.9f, p)
                p.color = 0xFF707888.toInt()
                c.drawRect(sx + s * 0.16f, sy + s * 0.82f, sx + s * 0.84f, sy + s, p)
            }
            'P' -> {
                fillRect(c, sx, sy, s, s, 0xFFB8B0A0.toInt())
                p.color = 0xFFA09888.toInt()
                c.drawRect(sx + s * 0.5f, sy, sx + s * 0.56f, sy + s, p)
                c.drawRect(sx, sy + s * 0.5f, sx + s, sy + s * 0.56f, p)
            }
            else -> fillRect(c, sx, sy, s, s, themeGround(theme))
        }
    }

    fun themeGround(theme: String): Int = when (theme) {
        "hoehle" -> 0xFF6A6478.toInt()
        "schatten" -> 0xFF5A5468.toInt()
        "gipfel" -> 0xFF7A7488.toInt()
        else -> 0xFFC8B888.toInt()
    }

    private fun themeArena(theme: String): Int = when (theme) {
        "kaefer" -> 0xFF9AB84C.toInt()
        "wasser" -> 0xFF68A8D8.toInt()
        "feuer" -> 0xFFD87848.toInt()
        "eis" -> 0xFFA8D8E8.toInt()
        "elektro" -> 0xFFE0C858.toInt()
        "schatten" -> 0xFF7060A0.toInt()
        "liga" -> 0xFFB8A0D8.toInt()
        else -> 0xFFC0B0A0.toInt()
    }

    // ---------------------------------------------------------------- Figuren
    private fun styleColors(style: String): IntArray = when (style) {
        "player" -> intArrayOf(0xFF3A2A18.toInt(), 0xFFE04040.toInt(), 0xFF3050A0.toInt(), 0xFFF0C090.toInt())
        "npc_f" -> intArrayOf(0xFF9A3A2A.toInt(), 0xFFE070B0.toInt(), 0xFF684898.toInt(), 0xFFF8D0A8.toInt())
        "npc_k" -> intArrayOf(0xFFE0C040.toInt(), 0xFF48B058.toInt(), 0xFF5878B8.toInt(), 0xFFF0C090.toInt())
        "npc_prof" -> intArrayOf(0xFFE0E0E8.toInt(), 0xFFF0F0F8.toInt(), 0xFF585868.toInt(), 0xFFF0C090.toInt())
        "npc_bug" -> intArrayOf(0xFF3A5A28.toInt(), 0xFF8AB040.toInt(), 0xFF6A5A30.toInt(), 0xFFF0C090.toInt())
        "npc_dark" -> intArrayOf(0xFF201828.toInt(), 0xFF483060.toInt(), 0xFF282030.toInt(), 0xFFD8B090.toInt())
        "npc_boss1" -> intArrayOf(0xFF5A3A18.toInt(), 0xFF9AB84C.toInt(), 0xFF6A5A30.toInt(), 0xFFF8D0A8.toInt())
        "npc_boss2" -> intArrayOf(0xFF204878.toInt(), 0xFF48A0E0.toInt(), 0xFF2A5A88.toInt(), 0xFFF8D0A8.toInt())
        "npc_boss3" -> intArrayOf(0xFF802818.toInt(), 0xFFE87038.toInt(), 0xFF603020.toInt(), 0xFFF0C090.toInt())
        "npc_boss4" -> intArrayOf(0xFFE8F0F8.toInt(), 0xFF98D8E8.toInt(), 0xFF6088A8.toInt(), 0xFFF8E0C8.toInt())
        "npc_boss5" -> intArrayOf(0xFF302838.toInt(), 0xFFE8C830.toInt(), 0xFF484058.toInt(), 0xFFF0C090.toInt())
        "npc_boss6" -> intArrayOf(0xFF181020.toInt(), 0xFF6830A0.toInt(), 0xFF201830.toInt(), 0xFFE0C0C8.toInt())
        "npc_champ" -> intArrayOf(0xFF382848.toInt(), 0xFFD8B040.toInt(), 0xFF503878.toInt(), 0xFFF0C090.toInt())
        else -> intArrayOf(0xFF503018.toInt(), 0xFF4878C0.toInt(), 0xFF404858.toInt(), 0xFFF0C090.toInt())
    }

    fun drawActor(c: Canvas, style: String, dir: Int, x: Float, y: Float, s: Float, walkFrame: Int) {
        when (style) {
            "sign" -> {
                p.color = 0xFF6B4A28.toInt()
                c.drawRect(x + s * 0.44f, y + s * 0.5f, x + s * 0.56f, y + s, p)
                p.color = 0xFFC89858.toInt()
                c.drawRect(x + s * 0.12f, y + s * 0.14f, x + s * 0.88f, y + s * 0.56f, p)
                p.color = 0xFF6B4A28.toInt()
                c.drawRect(x + s * 0.2f, y + s * 0.24f, x + s * 0.8f, y + s * 0.3f, p)
                c.drawRect(x + s * 0.2f, y + s * 0.38f, x + s * 0.66f, y + s * 0.44f, p)
                return
            }
            "item" -> {
                p.color = 0xFFE8B830.toInt()
                c.drawRect(x + s * 0.2f, y + s * 0.35f, x + s * 0.8f, y + s * 0.85f, p)
                p.color = 0xFF9A6A18.toInt()
                c.drawRect(x + s * 0.2f, y + s * 0.52f, x + s * 0.8f, y + s * 0.6f, p)
                c.drawRect(x + s * 0.44f, y + s * 0.35f, x + s * 0.56f, y + s * 0.85f, p)
                return
            }
            "ball" -> {
                p.color = 0xFFE03030.toInt()
                c.drawCircle(x + s / 2f, y + s * 0.55f, s * 0.3f, p)
                p.color = 0xFFF8F8F8.toInt()
                c.drawRect(x + s * 0.2f, y + s * 0.55f, x + s * 0.8f, y + s * 0.85f, p)
                p.color = 0xFF202028.toInt()
                c.drawRect(x + s * 0.2f, y + s * 0.52f, x + s * 0.8f, y + s * 0.58f, p)
                p.color = 0xFFF8F8F8.toInt()
                c.drawCircle(x + s / 2f, y + s * 0.55f, s * 0.09f, p)
                return
            }
            "npc_legend" -> {
                Sprites.drawMonster(c, "titanox", x - s * 0.35f, y - s * 0.5f, s * 1.7f)
                return
            }
        }
        val col = styleColors(style)
        val bmp = Sprites.character(style, dir, col[0], col[1], col[2], col[3])
        val bob = if (walkFrame == 1) 0.6f else 0f
        Sprites.draw(c, bmp, x, y - bob, s)
    }
}
