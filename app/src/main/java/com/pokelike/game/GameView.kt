package com.pokelike.game

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sqrt

class GameView(context: Context) : SurfaceView(context), SurfaceHolder.Callback, Runnable {

    private var thread: Thread? = null
    @Volatile private var running = false

    val game = Game(context)

    private var scale = 1f
    private var offX = 0f
    private var offY = 0f

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Bedienelemente in virtuellen Koordinaten
    private var dpadCx = 48f
    private var dpadCy = 0f
    private val dpadR = 44f
    private var aCx = 198f
    private var aCy = 0f
    private val aR = 23f
    private var bCx = 152f
    private var bCy = 0f
    private val bR = 19f
    private var startX = 96f
    private var startY = 0f
    private val startW = 48f
    private val startH = 18f

    init {
        holder.addCallback(this)
        isFocusable = true
        game.push(TitleScene())
    }

    private fun layoutControls() {
        val base = game.worldH
        dpadCy = base + 56f
        aCy = base + 46f
        bCy = base + 74f
        startY = base + 92f
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        running = true
        thread = Thread(this).also { it.start() }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        computeLayout(width, height)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        running = false
        try { thread?.join(800) } catch (e: InterruptedException) { }
        thread = null
    }

    private fun computeLayout(w: Int, h: Int) {
        if (w <= 0 || h <= 0) return
        val vh = (Game.VW * h / w).coerceIn(360f, 680f)
        game.vh = vh
        scale = min(w / Game.VW, h / vh)
        offX = (w - Game.VW * scale) / 2f
        offY = (h - vh * scale) / 2f
        layoutControls()
    }

    override fun run() {
        var last = System.nanoTime()
        while (running) {
            val now = System.nanoTime()
            var dt = (now - last) / 1_000_000_000f
            last = now
            if (dt > 0.05f) dt = 0.05f
            try {
                game.update(dt)
            } catch (e: Exception) {
                // Ein Fehler im Spiel soll die App nicht beenden
            }
            val canvas = holder.lockCanvas() ?: continue
            try {
                canvas.drawColor(0xFF000000.toInt())
                canvas.save()
                canvas.translate(offX, offY)
                canvas.scale(scale, scale)
                game.draw(canvas)
                drawControls(canvas)
                canvas.restore()
            } catch (e: Exception) {
                // ignorieren, naechster Frame
            } finally {
                holder.unlockCanvasAndPost(canvas)
            }
            val frameMs = (System.nanoTime() - now) / 1_000_000L
            val sleep = 16L - frameMs
            if (sleep > 0) {
                try { Thread.sleep(sleep) } catch (e: InterruptedException) { }
            }
        }
    }

    // ------------------------------------------------------------------
    private fun drawControls(c: Canvas) {
        val top = game.worldH
        Gfx.fillRect(c, 0f, top, Game.VW, game.vh - top, 0xFF1C2230.toInt())
        Gfx.fillRect(c, 0f, top, Game.VW, 2f, 0xFF3A4458.toInt())

        // Steuerkreuz
        paint.color = 0xFF2E3A50.toInt()
        c.drawCircle(dpadCx, dpadCy, dpadR, paint)
        paint.color = 0xFF4A587A.toInt()
        val arm = 14f
        c.drawRect(dpadCx - arm, dpadCy - dpadR + 6f, dpadCx + arm, dpadCy + dpadR - 6f, paint)
        c.drawRect(dpadCx - dpadR + 6f, dpadCy - arm, dpadCx + dpadR - 6f, dpadCy + arm, paint)
        paint.color = 0xFF8894B0.toInt()
        drawTri(c, dpadCx, dpadCy - dpadR + 10f, 8f, 0, game.input.isHeld(Btn.UP))
        drawTri(c, dpadCx, dpadCy + dpadR - 10f, 8f, 1, game.input.isHeld(Btn.DOWN))
        drawTri(c, dpadCx - dpadR + 10f, dpadCy, 8f, 2, game.input.isHeld(Btn.LEFT))
        drawTri(c, dpadCx + dpadR - 10f, dpadCy, 8f, 3, game.input.isHeld(Btn.RIGHT))

        // A / B
        paint.color = if (game.input.isHeld(Btn.A)) 0xFFF08050.toInt() else 0xFFD05038.toInt()
        c.drawCircle(aCx, aCy, aR, paint)
        paint.color = if (game.input.isHeld(Btn.B)) 0xFF80A0E0.toInt() else 0xFF4868B0.toInt()
        c.drawCircle(bCx, bCy, bR, paint)
        Gfx.text(c, "A", aCx, aCy + 5f, 14f, 0xFFFFF0E0.toInt(), center = true)
        Gfx.text(c, "B", bCx, bCy + 4f, 12f, 0xFFE8F0FF.toInt(), center = true)

        // START
        paint.color = if (game.input.isHeld(Btn.START)) 0xFF708098.toInt() else 0xFF48566E.toInt()
        c.drawRoundRect(android.graphics.RectF(startX, startY, startX + startW, startY + startH), 8f, 8f, paint)
        Gfx.text(c, "MENUE", startX + startW / 2f, startY + startH - 5f, 9f, 0xFFE0E8F8.toInt(), center = true)
    }

    private fun drawTri(c: Canvas, x: Float, y: Float, s: Float, dir: Int, active: Boolean) {
        paint.color = if (active) 0xFFF8E8A0.toInt() else 0xFF98A4C0.toInt()
        val p = android.graphics.Path()
        when (dir) {
            0 -> { p.moveTo(x, y - s / 2); p.lineTo(x - s / 2, y + s / 2); p.lineTo(x + s / 2, y + s / 2) }
            1 -> { p.moveTo(x, y + s / 2); p.lineTo(x - s / 2, y - s / 2); p.lineTo(x + s / 2, y - s / 2) }
            2 -> { p.moveTo(x - s / 2, y); p.lineTo(x + s / 2, y - s / 2); p.lineTo(x + s / 2, y + s / 2) }
            else -> { p.moveTo(x + s / 2, y); p.lineTo(x - s / 2, y - s / 2); p.lineTo(x - s / 2, y + s / 2) }
        }
        p.close()
        c.drawPath(p, paint)
    }

    // ------------------------------------------------------------------
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val up = BooleanArray(7)
        val action = event.actionMasked
        for (i in 0 until event.pointerCount) {
            if ((action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP)
                && i == event.actionIndex
            ) continue
            val vx = (event.getX(i) - offX) / scale
            val vy = (event.getY(i) - offY) / scale
            hit(vx, vy, up)
        }
        if (action == MotionEvent.ACTION_CANCEL) up.fill(false)
        for (b in Btn.values()) game.input.set(b, up[b.ordinal])
        return true
    }

    private fun hit(vx: Float, vy: Float, out: BooleanArray) {
        val dx = vx - dpadCx
        val dy = vy - dpadCy
        if (sqrt(dx * dx + dy * dy) <= dpadR + 12f) {
            if (abs(dx) > abs(dy)) {
                if (dx < -6f) out[Btn.LEFT.ordinal] = true
                else if (dx > 6f) out[Btn.RIGHT.ordinal] = true
            } else {
                if (dy < -6f) out[Btn.UP.ordinal] = true
                else if (dy > 6f) out[Btn.DOWN.ordinal] = true
            }
            return
        }
        val adx = vx - aCx
        val ady = vy - aCy
        if (sqrt(adx * adx + ady * ady) <= aR + 10f) { out[Btn.A.ordinal] = true; return }
        val bdx = vx - bCx
        val bdy = vy - bCy
        if (sqrt(bdx * bdx + bdy * bdy) <= bR + 10f) { out[Btn.B.ordinal] = true; return }
        if (vx >= startX - 6f && vx <= startX + startW + 6f && vy >= startY - 6f && vy <= startY + startH + 6f) {
            out[Btn.START.ordinal] = true
        }
    }

    fun pause() {
        running = false
        try { thread?.join(500) } catch (e: InterruptedException) { }
        thread = null
        if (game.state.flags.contains("starter")) GameState.save(context, game.state)
    }

    fun resume() {
        if (running) return
        if (holder.surface != null && holder.surface.isValid) {
            running = true
            thread = Thread(this).also { it.start() }
        }
    }
}
