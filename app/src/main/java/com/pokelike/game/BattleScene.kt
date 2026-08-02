package com.pokelike.game

import android.graphics.Canvas
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** Der Kampfbildschirm: verarbeitet die Ereignisse der Kampf-Engine. */
class BattleScene(
    private val battle: Battle,
    private val onFinish: (BattleResult) -> Unit
) : Scene {

    private enum class Phase { EVENTS, MENU, MOVES, WAIT_SUB, DONE }

    private val queue = ArrayDeque<BEvent>()
    private var current: BEvent? = null
    private var phase = Phase.EVENTS

    private var msg = ""
    private var typed = 0f
    private var msgHold = 0f

    private var pHpShown = 0f
    private var fHpShown = 0f
    private var pExpShown = 0f

    private var shakeSide = 0          // 1 = Spieler, 2 = Gegner
    private var shakeTime = 0f
    private var faintSide = 0
    private var faintTime = 0f
    private var sendTime = 0f
    private var ballTime = 0f
    private var ballShakes = 0
    private var flashTime = 0f
    private var evolveTime = 0f
    private var evolveFrom = ""
    private var evolveTo = ""

    private var menuIndex = 0
    private var moveIndex = 0
    private var result: BattleResult = BattleResult.NONE
    private var finished = false

    private val menuItems = listOf("KAMPF", "BEUTEL", "TEAM", "FLUCHT")

    init {
        enqueue(battle.start())
        pHpShown = battle.activePlayer.currentHp.toFloat()
        fHpShown = battle.activeFoe.currentHp.toFloat()
        pExpShown = expRatio(battle.activePlayer)
    }

    private fun expRatio(m: Monster): Float =
        (m.expInLevel().toFloat() / m.expLevelSpan().toFloat()).coerceIn(0f, 1f)

    private fun enqueue(events: List<BEvent>) {
        events.forEach { queue.add(it) }
        if (phase != Phase.EVENTS) phase = Phase.EVENTS
    }

    // ------------------------------------------------------------------
    override fun update(g: Game, dt: Float) {
        if (shakeTime > 0f) shakeTime -= dt
        if (flashTime > 0f) flashTime -= dt

        // Ein offenes Textfenster hat immer Vorrang, egal in welcher Phase.
        if (g.dialog.active) {
            g.dialog.update(g, dt)
            return
        }

        when (phase) {
            Phase.EVENTS -> updateEvents(g, dt)
            Phase.MENU -> updateMenu(g)
            Phase.MOVES -> updateMoves(g)
            Phase.WAIT_SUB -> Unit
            Phase.DONE -> Unit
        }
    }

    private fun updateEvents(g: Game, dt: Float) {
        val ev = current
        if (ev == null) {
            val next = queue.removeFirstOrNull()
            if (next == null) {
                if (finished) {
                    phase = Phase.DONE
                    g.pop()
                    onFinish(result)
                } else {
                    phase = Phase.MENU
                    menuIndex = 0
                }
                return
            }
            beginEvent(g, next)
            return
        }
        when (ev) {
            is BEvent.Msg -> {
                if (typed < msg.length) {
                    typed += dt * 60f
                    if (g.input.pressed(Btn.A) || g.input.pressed(Btn.B)) typed = msg.length.toFloat()
                } else {
                    msgHold += dt
                    if (g.input.pressed(Btn.A) || msgHold > 1.5f) current = null
                }
            }
            is BEvent.Hp -> {
                val target = ev.to.toFloat()
                if (ev.playerSide) {
                    pHpShown = approach(pHpShown, target, dt, battle.activePlayer.maxHp)
                    if (abs(pHpShown - target) < 0.4f) { pHpShown = target; current = null }
                } else {
                    fHpShown = approach(fHpShown, target, dt, battle.activeFoe.maxHp)
                    if (abs(fHpShown - target) < 0.4f) { fHpShown = target; current = null }
                }
            }
            is BEvent.Hit -> {
                if (shakeTime <= 0f) current = null
            }
            is BEvent.Faint -> {
                faintTime -= dt
                if (faintTime <= 0f) { faintSide = 0; current = null }
            }
            is BEvent.SendOut -> {
                sendTime -= dt
                if (sendTime <= 0f) current = null
            }
            is BEvent.Ball -> {
                ballTime += dt
                if (ballTime > (1.0f + ballShakes * 0.45f)) current = null
            }
            is BEvent.Exp -> {
                val m = battle.playerParty.getOrNull(ev.monsterIndex)
                if (m == null || ev.monsterIndex != battle.playerIndex) { current = null }
                else {
                    val target = expRatio(m)
                    pExpShown += dt * 0.9f
                    if (pExpShown >= target || pExpShown >= 1f) { pExpShown = target; current = null }
                }
            }
            is BEvent.Evolve -> {
                evolveTime -= dt
                if (evolveTime <= 0f) current = null
            }
            else -> current = null
        }
    }

    private fun approach(shown: Float, target: Float, dt: Float, maxHp: Int): Float {
        val speed = max(12f, maxHp * 0.9f)
        return if (shown > target) max(target, shown - speed * dt) else min(target, shown + speed * dt)
    }

    private fun beginEvent(g: Game, ev: BEvent) {
        current = ev
        when (ev) {
            is BEvent.Msg -> { msg = ev.text; typed = 0f; msgHold = 0f }
            is BEvent.Hit -> {
                shakeSide = if (ev.playerSide) 1 else 2
                shakeTime = 0.28f
                flashTime = if (ev.effectiveness >= 2.0) 0.25f else 0f
            }
            is BEvent.Faint -> { faintSide = if (ev.playerSide) 1 else 2; faintTime = 0.7f }
            is BEvent.SendOut -> {
                sendTime = 0.4f
                if (ev.playerSide) {
                    pHpShown = battle.activePlayer.currentHp.toFloat()
                    pExpShown = expRatio(battle.activePlayer)
                } else fHpShown = battle.activeFoe.currentHp.toFloat()
            }
            is BEvent.Ball -> { ballTime = 0f; ballShakes = ev.shakes }
            is BEvent.LevelUp -> {
                val m = battle.playerParty.getOrNull(ev.monsterIndex)
                queue.addFirst(BEvent.Msg("${m?.name ?: "Monster"} erreicht Level ${ev.newLevel}!"))
                if (ev.monsterIndex == battle.playerIndex) pExpShown = 0f
                current = null
            }
            is BEvent.LearnMove -> {
                val m = battle.playerParty.getOrNull(ev.monsterIndex)
                queue.addFirst(BEvent.Msg("${m?.name ?: "Monster"} erlernt ${Moves.get(ev.moveId).name}!"))
                current = null
            }
            is BEvent.LearnPrompt -> {
                current = null
                phase = Phase.WAIT_SUB
                val m = battle.playerParty.getOrNull(ev.monsterIndex)
                if (m == null) { phase = Phase.EVENTS; return }
                g.dialog.ask(
                    listOf(
                        "${m.name} moechte ${Moves.get(ev.moveId).name} erlernen,",
                        "kennt aber schon 4 Attacken. Eine ersetzen?"
                    ),
                    listOf("Ja", "Nein")
                ) { idx ->
                    if (idx == 0) {
                        g.push(MoveLearnScene(m, ev.moveId) { phase = Phase.EVENTS })
                    } else {
                        g.dialog.say(listOf("${m.name} hat ${Moves.get(ev.moveId).name} nicht erlernt.")) {
                            phase = Phase.EVENTS
                        }
                    }
                }
            }
            is BEvent.Evolve -> {
                val m = battle.playerParty.getOrNull(ev.monsterIndex)
                if (m == null) { current = null; return }
                evolveFrom = m.speciesId
                evolveTo = ev.toId
                evolveTime = 1.6f
                val oldName = m.name
                m.evolveInto(ev.toId)
                queue.addFirst(BEvent.Msg("Glueckwunsch! $oldName entwickelt sich zu ${m.name}!"))
            }
            is BEvent.RequestSwitch -> {
                current = null
                phase = Phase.WAIT_SUB
                g.push(PartyScene(forcedSwitch = true, onChosen = { idx ->
                    enqueue(battle.switchAfterFaint(idx))
                    phase = Phase.EVENTS
                }))
            }
            is BEvent.End -> {
                result = ev.result
                finished = true
                current = null
            }
            else -> current = null
        }
    }

    // ------------------------------------------------------------------
    private fun updateMenu(g: Game) {
        if (g.input.repeated(Btn.LEFT) && menuIndex % 2 == 1) menuIndex--
        if (g.input.repeated(Btn.RIGHT) && menuIndex % 2 == 0) menuIndex++
        if (g.input.repeated(Btn.UP) && menuIndex >= 2) menuIndex -= 2
        if (g.input.repeated(Btn.DOWN) && menuIndex < 2) menuIndex += 2
        if (g.input.pressed(Btn.A)) {
            when (menuIndex) {
                0 -> { phase = Phase.MOVES; moveIndex = 0 }
                1 -> openBag(g)
                2 -> openParty(g)
                3 -> enqueue(battle.playerRun())
            }
        }
    }

    private fun updateMoves(g: Game) {
        val moves = battle.activePlayer.moves
        if (g.input.repeated(Btn.UP) && moveIndex >= 2) moveIndex -= 2
        if (g.input.repeated(Btn.DOWN) && moveIndex + 2 < moves.size) moveIndex += 2
        if (g.input.repeated(Btn.LEFT) && moveIndex % 2 == 1) moveIndex--
        if (g.input.repeated(Btn.RIGHT) && moveIndex % 2 == 0 && moveIndex + 1 < moves.size) moveIndex++
        if (g.input.pressed(Btn.B)) { phase = Phase.MENU; return }
        if (g.input.pressed(Btn.A)) {
            val noPp = moves.none { it.pp > 0 }
            if (noPp) { enqueue(battle.playerMove(0)); return }
            val slot = moves.getOrNull(moveIndex) ?: return
            if (slot.pp <= 0) return
            enqueue(battle.playerMove(moveIndex))
        }
    }

    private fun openBag(g: Game) {
        phase = Phase.WAIT_SUB
        g.push(BagScene(inBattle = true, onClose = { phase = Phase.MENU }, onUse = { itemId ->
            val item = Items.get(itemId)
            if (item.needsTarget && item.ballRate <= 0.0) {
                g.push(PartyScene(forcedSwitch = false, onChosen = { idx ->
                    useItem(g, itemId, idx)
                }, onCancel = { phase = Phase.MENU }))
            } else {
                useItem(g, itemId, battle.playerIndex)
            }
        }))
    }

    private fun useItem(g: Game, itemId: String, target: Int) {
        val events = battle.playerItem(itemId, target)
        if (battle.lastItemConsumed) g.state.bag.remove(itemId, 1)
        enqueue(events)
        phase = Phase.EVENTS
    }

    private fun openParty(g: Game) {
        phase = Phase.WAIT_SUB
        g.push(PartyScene(forcedSwitch = false, onChosen = { idx ->
            if (idx == battle.playerIndex) {
                g.toast("${battle.activePlayer.name} kaempft bereits!")
                phase = Phase.MENU
            } else if (battle.playerParty[idx].isFainted) {
                g.toast("Dieses Monster ist besiegt!")
                phase = Phase.MENU
            } else {
                enqueue(battle.playerSwitch(idx))
                phase = Phase.EVENTS
            }
        }, onCancel = { phase = Phase.MENU }))
    }

    // ------------------------------------------------------------------
    override fun draw(g: Game, c: Canvas) {
        val w = Game.VW
        val h = g.worldH

        // Hintergrund
        Gfx.fillRect(c, 0f, 0f, w, h, 0xFF98D0F0.toInt())
        Gfx.fillRect(c, 0f, h * 0.42f, w, h * 0.58f, 0xFF9AC868.toInt())
        Gfx.fillRect(c, 0f, h * 0.42f, w, 3f, 0xFF7AA850.toInt())
        // Plattformen
        Gfx.p.color = 0xFF88B858.toInt()
        c.drawOval(android.graphics.RectF(126f, h * 0.34f, 226f, h * 0.44f), Gfx.p)
        c.drawOval(android.graphics.RectF(12f, h - 118f, 122f, h - 92f), Gfx.p)

        val foeShake = if (shakeSide == 2 && shakeTime > 0f) ((shakeTime * 60).toInt() % 2) * 3f - 1.5f else 0f
        val plShake = if (shakeSide == 1 && shakeTime > 0f) ((shakeTime * 60).toInt() % 2) * 3f - 1.5f else 0f

        // Gegner
        val foe = battle.activeFoe
        val foeY = h * 0.40f - 56f + (if (faintSide == 2) (0.7f - faintTime) * 60f else 0f)
        if (!(faintSide == 2 && faintTime < 0.2f)) {
            val alpha = if (faintSide == 2) (faintTime / 0.7f * 255).toInt().coerceIn(0, 255) else 255
            if (evolveTime > 0f && evolveTo.isNotEmpty()) {
                // Entwicklungsanimation betrifft das eigene Monster
            }
            Sprites.drawMonster(c, foe.speciesId, 150f + foeShake, foeY, 56f, flip = false, alpha = alpha)
        }

        // Eigenes Monster
        val pl = battle.activePlayer
        val plY = h - 158f + (if (faintSide == 1) (0.7f - faintTime) * 60f else 0f)
        if (!(faintSide == 1 && faintTime < 0.2f)) {
            val alpha = if (faintSide == 1) (faintTime / 0.7f * 255).toInt().coerceIn(0, 255) else 255
            val speciesToDraw = if (evolveTime > 0f) {
                if ((g.tick / 4) % 2 == 0) evolveFrom else evolveTo
            } else pl.speciesId
            Sprites.drawMonster(c, speciesToDraw, 28f + plShake, plY, 66f, flip = true, alpha = alpha)
        }

        if (flashTime > 0f) {
            Gfx.fillRect(c, 0f, 0f, w, h, (0x60FFFFFF).toInt())
        }

        // Ballwurf
        if (current is BEvent.Ball) drawBall(c, h)

        // Infoboxen
        drawFoeBox(c, foe)
        drawPlayerBox(c, pl, h)

        // Unterer Bereich
        when {
            g.dialog.active -> g.dialog.draw(g, c)
            phase == Phase.EVENTS || phase == Phase.WAIT_SUB || phase == Phase.DONE -> drawTextBox(c, g, h)
            phase == Phase.MENU -> drawMenu(c, g, h)
            phase == Phase.MOVES -> drawMoves(c, g, h)
        }
    }

    private fun drawTextBox(c: Canvas, g: Game, h: Float) {
        val bh = 52f
        val y = h - bh - 4f
        Gfx.panel(c, 4f, y, Game.VW - 8f, bh)
        val visible = msg.substring(0, min(msg.length, typed.toInt()))
        for ((i, ln) in Gfx.wrap(visible, 33).withIndex()) {
            if (i > 2) break
            Gfx.text(c, ln, 12f, y + 16f + i * 12f, 10f)
        }
    }

    private fun drawMenu(c: Canvas, g: Game, h: Float) {
        val bh = 52f
        val y = h - bh - 4f
        Gfx.panel(c, 4f, y, 118f, bh)
        Gfx.text(c, "Was tust du?", 12f, y + 20f, 10f)
        Gfx.text(c, battle.activePlayer.name, 12f, y + 36f, 9f, 0xFF606880.toInt())
        Gfx.panel(c, 126f, y, Game.VW - 130f, bh)
        for (i in menuItems.indices) {
            val cx = 126f + 16f + (i % 2) * 52f
            val cy = y + 20f + (i / 2) * 18f
            Gfx.text(c, menuItems[i], cx, cy, 10f)
            if (i == menuIndex) Gfx.cursor(c, cx - 9f, cy - 4f)
        }
    }

    private fun drawMoves(c: Canvas, g: Game, h: Float) {
        val bh = 52f
        val y = h - bh - 4f
        val moves = battle.activePlayer.moves
        Gfx.panel(c, 4f, y, Game.VW - 8f, bh)
        for (i in moves.indices) {
            val mv = moves[i].move
            val cx = 16f + (i % 2) * 112f
            val cy = y + 17f + (i / 2) * 17f
            val col = if (moves[i].pp <= 0) 0xFFA0A0A8.toInt() else Gfx.BLACK
            Gfx.text(c, mv.name, cx, cy, 9.5f, col)
            if (i == moveIndex) Gfx.cursor(c, cx - 9f, cy - 4f)
        }
        val sel = moves.getOrNull(moveIndex)
        if (sel != null) {
            if (moves.none { it.pp > 0 }) Unit
            else Gfx.text(c, "AP ${sel.pp}/${sel.maxPp}  (B = zurueck)", Game.VW - 12f, y + 47f, 8.5f,
                0xFF404858.toInt(), right = true)
            Gfx.typeBadge(c, sel.move.type, 10f, y + 38f, 8f)
            val kat = when (sel.move.cat) {
                MoveCategory.PHYSISCH -> "PHYS"
                MoveCategory.SPEZIAL -> "SPEZ"
                else -> "STAT"
            }
            Gfx.text(c, "$kat  ST ${if (sel.move.power > 0) sel.move.power.toString() else "-"}",
                62f, y + 47f, 9f, 0xFF404858.toInt())
        }
        if (moves.none { it.pp > 0 }) {
            Gfx.text(c, "Keine AP! -> Verzweifler", Game.VW - 12f, y + 47f, 8f, Gfx.ACCENT, right = true)
        }
    }

    private fun drawFoeBox(c: Canvas, foe: Monster) {
        Gfx.panel(c, 6f, 10f, 124f, 40f)
        Gfx.text(c, foe.name, 12f, 24f, 10f)
        Gfx.text(c, "Lv${foe.level}", 124f, 24f, 9f, right = true)
        Gfx.hpBar(c, 12f, 28f, 112f, 6f, fHpShown / max(1, foe.maxHp))
        Gfx.statusTag(c, foe.status, 12f, 37f, 6.5f)
        var bx = if (foe.status == StatusKind.NONE) 12f else 42f
        for (t in foe.species.types) {
            Gfx.typeBadge(c, t, bx, 37f, 6.5f)
            bx += Gfx.textWidth(t.deName.uppercase(), 6.5f) + 12f
        }
    }

    private fun drawPlayerBox(c: Canvas, pl: Monster, h: Float) {
        val x = Game.VW - 130f
        val y = h - 118f
        Gfx.panel(c, x, y, 124f, 44f)
        Gfx.text(c, pl.name, x + 6f, y + 14f, 10f)
        Gfx.text(c, "Lv${pl.level}", x + 118f, y + 14f, 9f, right = true)
        Gfx.hpBar(c, x + 6f, y + 18f, 112f, 7f, pHpShown / max(1, pl.maxHp))
        Gfx.text(c, "${pHpShown.toInt()}/${pl.maxHp}", x + 118f, y + 33f, 9f, right = true)
        Gfx.statusTag(c, pl.status, x + 6f, y + 26f, 7f)
        Gfx.expBar(c, x + 6f, y + 36f, 112f, 4f, pExpShown)
    }

    private fun drawBall(c: Canvas, h: Float) {
        val t = ballTime
        val startX = 60f
        val startY = h - 130f
        val endX = 172f
        val endY = h * 0.2f + 20f
        val flight = t.coerceAtMost(0.6f) / 0.6f
        val bx = startX + (endX - startX) * flight
        val by = startY + (endY - startY) * flight - 60f * kotlin.math.sin(flight * Math.PI).toFloat()
        val shakePhase = ((t - 0.7f) * 2.2f).toInt()
        val wob = if (t > 0.7f && shakePhase < ballShakes) kotlin.math.sin(t * 26f) * 3f else 0f
        Gfx.p.color = 0xFFE03030.toInt()
        c.drawCircle(bx + wob, by, 7f, Gfx.p)
        Gfx.p.color = 0xFFF8F8F8.toInt()
        c.drawRect(bx + wob - 7f, by, bx + wob + 7f, by + 7f, Gfx.p)
        Gfx.p.color = 0xFF202028.toInt()
        c.drawRect(bx + wob - 7f, by - 1f, bx + wob + 7f, by + 1.5f, Gfx.p)
        Gfx.p.color = 0xFFF8F8F8.toInt()
        c.drawCircle(bx + wob, by, 2.5f, Gfx.p)
    }
}
