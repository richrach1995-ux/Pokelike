package com.pokelike.game

import android.content.Context
import android.graphics.Canvas
import kotlin.math.max
import kotlin.math.min

enum class Btn { UP, DOWN, LEFT, RIGHT, A, B, START }

class Input {
    private val held = BooleanArray(7)
    private val prev = BooleanArray(7)
    private val timers = FloatArray(7)

    fun set(b: Btn, v: Boolean) { held[b.ordinal] = v }

    fun isHeld(b: Btn) = held[b.ordinal]

    /** Einmaliger Tastendruck. */
    fun pressed(b: Btn): Boolean = held[b.ordinal] && !prev[b.ordinal]

    /** Tastendruck mit Wiederholung (fuer Menues). */
    fun repeated(b: Btn): Boolean {
        val i = b.ordinal
        if (!held[i]) return false
        if (!prev[i]) return true
        return timers[i] <= 0f
    }

    fun update(dt: Float) {
        for (i in held.indices) {
            if (held[i]) {
                if (!prev[i]) timers[i] = 0.36f
                else {
                    timers[i] -= dt
                    if (timers[i] <= 0f) timers[i] = 0.11f
                }
            } else timers[i] = 0f
            prev[i] = held[i]
        }
    }
}

interface Scene {
    fun update(g: Game, dt: Float)
    fun draw(g: Game, c: Canvas)
    val opaque: Boolean get() = true
}

/** Textfenster mit Schreibmaschineneffekt und optionaler Auswahl. */
class DialogBox {
    private val pages = ArrayDeque<String>()
    private var current: String = ""
    private var shown: Float = 0f
    var active = false; private set
    private var choices: List<String>? = null
    private var choiceIndex = 0
    private var onChoice: ((Int) -> Unit)? = null
    private var onDone: (() -> Unit)? = null
    private var pendingChoices: List<String>? = null

    fun say(lines: List<String>, onDone: (() -> Unit)? = null) {
        pages.clear()
        lines.forEach { pages.add(it) }
        this.onDone = onDone
        this.pendingChoices = null
        this.choices = null
        nextPage()
        active = true
    }

    fun ask(lines: List<String>, options: List<String>, onChoice: (Int) -> Unit) {
        pages.clear()
        lines.forEach { pages.add(it) }
        this.pendingChoices = options
        this.onChoice = onChoice
        this.onDone = null
        this.choices = null
        nextPage()
        active = true
    }

    private fun nextPage() {
        current = pages.removeFirstOrNull() ?: ""
        shown = 0f
    }

    private val isTyping: Boolean get() = shown < current.length

    fun update(g: Game, dt: Float) {
        if (!active) return
        if (choices != null) {
            val opts = choices!!
            if (g.input.repeated(Btn.UP)) choiceIndex = (choiceIndex - 1 + opts.size) % opts.size
            if (g.input.repeated(Btn.DOWN)) choiceIndex = (choiceIndex + 1) % opts.size
            if (g.input.pressed(Btn.A)) {
                val cb = onChoice
                val idx = choiceIndex
                close()
                cb?.invoke(idx)
            } else if (g.input.pressed(Btn.B) && opts.size == 2) {
                val cb = onChoice
                close()
                cb?.invoke(1)
            }
            return
        }
        if (isTyping) {
            shown += dt * 48f
            if (g.input.pressed(Btn.A) || g.input.pressed(Btn.B)) shown = current.length.toFloat()
        } else if (g.input.pressed(Btn.A)) {
            if (pages.isNotEmpty()) nextPage()
            else if (pendingChoices != null) {
                choices = pendingChoices
                choiceIndex = 0
            } else {
                val cb = onDone
                close()
                cb?.invoke()
            }
        }
    }

    fun close() {
        active = false
        pages.clear()
        current = ""
        choices = null
        pendingChoices = null
        onDone = null
        onChoice = null
    }

    fun draw(g: Game, c: Canvas) {
        if (!active) return
        val h = 52f
        val y = g.worldH - h - 4f
        Gfx.panel(c, 4f, y, Game.VW - 8f, h)
        val visible = current.substring(0, min(current.length, shown.toInt()))
        val lines = Gfx.wrap(visible, 33)
        for ((i, ln) in lines.withIndex()) {
            if (i > 2) break
            Gfx.text(c, ln, 12f, y + 16f + i * 12f, 10f)
        }
        if (!isTyping && choices == null) {
            if ((g.tick / 20) % 2 == 0) Gfx.cursor(c, Game.VW - 18f, y + h - 10f, 6f)
        }
        choices?.let { opts ->
            val w = 74f
            val ch = 6f + opts.size * 13f
            val cx = Game.VW - w - 8f
            val cy = y - ch - 3f
            Gfx.panel(c, cx, cy, w, ch)
            for ((i, o) in opts.withIndex()) {
                Gfx.text(c, o, cx + 14f, cy + 15f + i * 13f, 10f)
                if (i == choiceIndex) Gfx.cursor(c, cx + 6f, cy + 11f + i * 13f)
            }
        }
    }
}

class Game(val ctx: Context) {

    companion object {
        const val VW = 240f
        const val CTRL_H = 118f
        const val TILE = 16f
    }

    val state = GameState()
    val input = Input()
    val dialog = DialogBox()
    private val stack = ArrayList<Scene>()

    var vh: Float = 420f
    val worldH: Float get() = vh - CTRL_H
    var tick: Int = 0

    // Ueberblendung
    private var fadeTime = 0f
    private var fadeDur = 0f
    private var fadeMid: (() -> Unit)? = null

    var message: String = ""
    private var messageTime = 0f

    fun push(s: Scene) { stack.add(s) }
    fun pop() { if (stack.size > 1) stack.removeAt(stack.size - 1) }
    fun replaceAll(s: Scene) { stack.clear(); stack.add(s) }
    fun top(): Scene? = stack.lastOrNull()

    fun toast(s: String) { message = s; messageTime = 2.0f }

    fun fade(duration: Float, onMid: () -> Unit) {
        fadeDur = duration
        fadeTime = 0f
        fadeMid = onMid
    }

    fun save(): Boolean = GameState.save(ctx, state)

    fun update(dt: Float) {
        tick++
        if (messageTime > 0f) { messageTime -= dt; if (messageTime <= 0f) message = "" }
        if (fadeDur > 0f) {
            val before = fadeTime
            fadeTime += dt
            if (before < fadeDur / 2f && fadeTime >= fadeDur / 2f) {
                fadeMid?.invoke()
                fadeMid = null
            }
            if (fadeTime >= fadeDur) { fadeDur = 0f; fadeTime = 0f }
        } else {
            stack.lastOrNull()?.update(this, dt)
        }
        state.playTimeMs += (dt * 1000).toLong()
        input.update(dt)
    }

    fun draw(c: Canvas) {
        Gfx.fillRect(c, 0f, 0f, VW, vh, 0xFF101018.toInt())
        var start = stack.size - 1
        while (start > 0 && !stack[start].opaque) start--
        for (i in start until stack.size) stack[i].draw(this, c)
        if (message.isNotEmpty()) {
            val w = Gfx.textWidth(message, 10f) + 16f
            Gfx.panel(c, (VW - w) / 2f, worldH - 84f, w, 20f)
            Gfx.text(c, message, VW / 2f, worldH - 70f, 10f, center = true)
        }
        if (fadeDur > 0f) {
            val t = fadeTime / fadeDur
            val a = if (t < 0.5f) t * 2f else (1f - t) * 2f
            Gfx.fillRect(c, 0f, 0f, VW, vh, (((a * 255).toInt().coerceIn(0, 255)) shl 24))
        }
    }
}

// ======================================================================
//  Titelbildschirm
// ======================================================================
class TitleScene : Scene {
    private var index = 0
    private var options = listOf("Neues Spiel")

    init { }

    private fun buildOptions(g: Game) {
        options = if (GameState.hasSave(g.ctx)) listOf("Weiterspielen", "Neues Spiel")
        else listOf("Neues Spiel")
    }

    private var built = false

    override fun update(g: Game, dt: Float) {
        if (!built) { buildOptions(g); built = true }
        if (g.input.repeated(Btn.UP)) index = (index - 1 + options.size) % options.size
        if (g.input.repeated(Btn.DOWN)) index = (index + 1) % options.size
        if (g.input.pressed(Btn.A)) {
            when (options[index]) {
                "Weiterspielen" -> {
                    val loaded = GameState.load(g.ctx)
                    if (loaded != null) {
                        g.state.loadJson(loaded.toJson())
                        g.fade(0.5f) { g.replaceAll(OverworldScene()) }
                    } else g.toast("Spielstand beschaedigt!")
                }
                else -> {
                    g.state.startNewGame()
                    g.fade(0.5f) { g.replaceAll(OverworldScene()); }
                }
            }
        }
    }

    override fun draw(g: Game, c: Canvas) {
        Gfx.fillRect(c, 0f, 0f, Game.VW, g.vh, 0xFF1A2038.toInt())
        // Sternenhimmel
        for (i in 0 until 40) {
            val x = ((i * 7919) % 240).toFloat()
            val y = ((i * 104729) % (g.worldH.toInt())).toFloat()
            val b = ((g.tick / 8 + i) % 10 < 5)
            Gfx.fillRect(c, x, y, 1.5f, 1.5f, if (b) 0xFFFFFFFF.toInt() else 0xFF8890B0.toInt())
        }
        Gfx.fillRect(c, 0f, g.worldH * 0.62f, Game.VW, g.vh, 0xFF243052.toInt())
        Sprites.drawMonster(c, "flamki", 20f, g.worldH * 0.42f, 44f)
        Sprites.drawMonster(c, "aquino", 98f, g.worldH * 0.40f, 44f)
        Sprites.drawMonster(c, "sproutz", 176f, g.worldH * 0.42f, 44f)
        Gfx.textShadow(c, "POKELIKE", Game.VW / 2f, g.worldH * 0.22f, 30f, 0xFFF8D040.toInt(), true)
        Gfx.textShadow(c, "Die Monster von Auronia", Game.VW / 2f, g.worldH * 0.29f, 11f, 0xFFE8E8F8.toInt(), true)

        val by = g.worldH * 0.72f
        Gfx.panel(c, Game.VW / 2f - 60f, by, 120f, 8f + options.size * 16f)
        for ((i, o) in options.withIndex()) {
            Gfx.text(c, o, Game.VW / 2f, by + 20f + i * 16f, 11f, center = true)
            if (i == index) Gfx.cursor(c, Game.VW / 2f - 52f, by + 16f + i * 16f)
        }
        Gfx.text(c, "A = Auswaehlen", Game.VW / 2f, g.worldH - 8f, 9f, 0xFFB0B8D0.toInt(), true)
    }
}

// ======================================================================
//  Oberwelt
// ======================================================================
class OverworldScene : Scene {

    private var moveProgress = 0f
    private var moving = false
    private var fromX = 0
    private var fromY = 0
    private var walkFrame = 0
    private var stepCounter = 0
    private var sliding = false
    private var pendingBattleTrainer: NpcDef? = null

    private val map: GameMap get() = MapData.get(mapId)
    private var mapId: String = ""

    private fun ensureMap(g: Game) {
        if (mapId != g.state.mapId) {
            mapId = g.state.mapId
            moving = false
            moveProgress = 0f
        }
    }

    // ------------------------------------------------------------------
    override fun update(g: Game, dt: Float) {
        ensureMap(g)
        if (g.dialog.active) { g.dialog.update(g, dt); return }

        if (moving) {
            moveProgress += dt * 6.2f
            walkFrame = if ((moveProgress * 2).toInt() % 2 == 0) 0 else 1
            if (moveProgress >= 1f) {
                moving = false
                moveProgress = 0f
                onStepFinished(g)
            }
            return
        }

        if (g.input.pressed(Btn.START)) { g.push(MenuScene()); return }
        if (g.input.pressed(Btn.A)) { interact(g); return }

        val d = when {
            g.input.isHeld(Btn.UP) -> 1
            g.input.isHeld(Btn.DOWN) -> 0
            g.input.isHeld(Btn.LEFT) -> 2
            g.input.isHeld(Btn.RIGHT) -> 3
            else -> -1
        }
        if (d >= 0) {
            if (g.state.dir != d) {
                g.state.dir = d
                stepCounter = 0
            }
            tryStep(g, d)
        }
    }

    private fun dxOf(d: Int) = when (d) { 2 -> -1; 3 -> 1; else -> 0 }
    private fun dyOf(d: Int) = when (d) { 1 -> -1; 0 -> 1; else -> 0 }

    private fun blockedBy(g: Game, x: Int, y: Int): Boolean {
        if (x < 0 || y < 0 || x >= map.width || y >= map.height) return true
        if (map.solidAt(x, y)) return true
        for (n in visibleNpcs(g)) if (n.blocks && n.x == x && n.y == y) return true
        return false
    }

    private fun visibleNpcs(g: Game): List<NpcDef> = map.npcs.filter { npcVisible(g, it) }

    private fun npcVisible(g: Game, n: NpcDef): Boolean {
        if (n.requiresFlag.isNotEmpty() && !g.state.flags.contains(n.requiresFlag)) return false
        if (n.hiddenFlag.isNotEmpty() && g.state.flags.contains(n.hiddenFlag)) return false
        if (n.kind == NpcKind.ITEM && g.state.flags.contains(itemFlag(n))) return false
        return true
    }

    private fun itemFlag(n: NpcDef) = "item_${mapId}_${n.id}"
    private fun trainerFlag(n: NpcDef) = "trainer_${n.trainer?.id ?: n.id}"

    private fun tryStep(g: Game, d: Int) {
        val nx = g.state.px + dxOf(d)
        val ny = g.state.py + dyOf(d)
        if (blockedBy(g, nx, ny)) {
            sliding = false
            return
        }
        fromX = g.state.px; fromY = g.state.py
        g.state.px = nx; g.state.py = ny
        moving = true
        moveProgress = 0f
    }

    private fun onStepFinished(g: Game) {
        g.state.steps++
        val t = map.tile(g.state.px, g.state.py)

        // Warp?
        val warp = map.warpAt(g.state.px, g.state.py)
        if (warp != null) {
            if (warp.requiresFlag.isNotEmpty() && !g.state.flags.contains(warp.requiresFlag)) {
                g.dialog.say(listOf(blockedWarpText(warp)))
                // Zurueck auf das vorherige Feld
                g.state.px = fromX; g.state.py = fromY
                return
            }
            sliding = false
            g.fade(0.45f) {
                g.state.mapId = warp.to
                g.state.px = warp.tx
                g.state.py = warp.ty
                mapId = warp.to
            }
            return
        }

        // Eis: weiterrutschen
        if (Tiles.isIce(t)) {
            val nx = g.state.px + dxOf(g.state.dir)
            val ny = g.state.py + dyOf(g.state.dir)
            if (!blockedBy(g, nx, ny)) {
                sliding = true
                fromX = g.state.px; fromY = g.state.py
                g.state.px = nx; g.state.py = ny
                moving = true
                moveProgress = 0f
                return
            }
            sliding = false
        }

        // Trainer in Sichtweite?
        val spotted = findSpotter(g)
        if (spotted != null) {
            startTrainerEncounter(g, spotted)
            return
        }

        // Wilde Monster
        if (Tiles.isTallGrass(t)) {
            if (g.state.repelSteps > 0) {
                g.state.repelSteps--
            } else if ((0 until 100).random() < map.encounterRate) {
                val wild = map.randomEncounter()
                if (wild != null) startWildBattle(g, wild)
            }
        }
    }

    private fun blockedWarpText(w: Warp): String = when (w.requiresFlag) {
        "lampe_erhalten" -> "Es ist stockdunkel. Ohne Licht gehst du hier keinen Schritt weiter."
        "morgana_besiegt" -> "Der Weg ist von Ordensleuten versperrt. Erst muss Morgana besiegt werden."
        "champion_besiegt" -> "Eine schwere Tuer. Sie oeffnet sich erst fuer den Champion."
        else -> "Hier geht es nicht weiter."
    }

    private fun findSpotter(g: Game): NpcDef? {
        for (n in visibleNpcs(g)) {
            val t = n.trainer ?: continue
            if (n.sight <= 0) continue
            if (g.state.flags.contains(trainerFlag(n))) continue
            val dx = dxOf(n.dir)
            val dy = dyOf(n.dir)
            for (i in 1..n.sight) {
                val cx = n.x + dx * i
                val cy = n.y + dy * i
                if (map.solidAt(cx, cy)) break
                if (cx == g.state.px && cy == g.state.py) return n
            }
        }
        return null
    }

    private fun startTrainerEncounter(g: Game, n: NpcDef) {
        g.dialog.say(n.lines) { beginTrainerBattle(g, n) }
    }

    private fun beginTrainerBattle(g: Game, n: NpcDef) {
        val t = n.trainer ?: return
        if (!g.state.anyAlive()) { g.dialog.say(listOf("Dein Team ist nicht kampfbereit!")); return }
        val battle = Battle(
            g.state.party, t.buildParty(),
            if (t.boss) BattleKind.BOSS else BattleKind.TRAINER,
            trainerName = t.name, rewardMoney = t.money, trainerStyle = n.style
        )
        g.fade(0.4f) {
            g.push(BattleScene(battle) { res ->
                onTrainerBattleEnd(g, n, t, res)
            })
        }
    }

    private fun onTrainerBattleEnd(g: Game, n: NpcDef, t: TrainerDef, res: BattleResult) {
        if (res == BattleResult.WIN) {
            g.state.setFlag(trainerFlag(n))
            g.state.money += t.money
            val after = mutableListOf<String>()
            after.add(t.winLine)
            after.addAll(n.afterLines)
            if (t.badge.isNotEmpty()) {
                g.state.setFlag(t.badge)
                if (t.badge.startsWith("siegel")) after.add("Du erhaeltst das ${badgeName(t.badge)}!")
            }
            if (t.rewardItem.isNotEmpty()) {
                g.state.bag.add(t.rewardItem, t.rewardCount)
                after.add("Du erhaeltst ${t.rewardCount}x ${Items.get(t.rewardItem).name}!")
            }
            g.dialog.say(after)
        } else if (res == BattleResult.LOSE) {
            whiteout(g)
        }
    }

    private fun badgeName(flag: String): String = when (flag) {
        "siegel1" -> "KUPFER-SIEGEL"
        "siegel2" -> "WELLEN-SIEGEL"
        "siegel3" -> "GLUT-SIEGEL"
        "siegel4" -> "FROST-SIEGEL"
        "siegel5" -> "STURM-SIEGEL"
        else -> "SIEGEL"
    }

    private fun startWildBattle(g: Game, wild: Monster) {
        g.state.seen.add(wild.speciesId)
        val battle = Battle(g.state.party, mutableListOf(wild), BattleKind.WILD)
        g.fade(0.4f) {
            g.push(BattleScene(battle) { res ->
                if (res == BattleResult.LOSE) whiteout(g)
                else if (res == BattleResult.CAUGHT) {
                    val m = battle.caughtMonster
                    if (m != null) {
                        val toParty = g.state.addMonster(m)
                        if (!toParty) g.toast("${m.name} wurde in die Box geschickt.")
                    }
                }
            })
        }
    }

    fun whiteout(g: Game) {
        g.dialog.say(
            listOf(
                "Du hast keine kampffaehigen Monster mehr ...",
                "Schnell zurueck zur letzten Heilstation!"
            )
        ) {
            g.fade(0.6f) {
                g.state.healParty()
                g.state.mapId = g.state.healMap
                g.state.px = g.state.healX
                g.state.py = g.state.healY
                mapId = g.state.healMap
                g.state.dir = 0
            }
        }
    }

    // ------------------------------------------------------------------
    private fun interact(g: Game) {
        val fx = g.state.px + dxOf(g.state.dir)
        val fy = g.state.py + dyOf(g.state.dir)
        val npc = visibleNpcs(g).firstOrNull { it.x == fx && it.y == fy }
        if (npc != null) { talkTo(g, npc); return }
        when (map.tile(fx, fy)) {
            'H' -> useHealStation(g)
            'T' -> {
                if (map.shop.isEmpty()) g.dialog.say(listOf("Der Stand ist gerade unbesetzt."))
                else g.push(ShopScene(map.shop))
            }
            '~' -> {
                if (g.state.bag.has("angel")) askFishing(g)
                else g.dialog.say(listOf("Das Wasser ist tief und klar."))
            }
            'b' -> g.dialog.say(listOf("Regale voller Buecher ueber Monster und ihre Typen."))
            'M' -> g.dialog.say(listOf("Eine steinerne Statue. Sie zeigt einen alten Trainer."))
            else -> Unit
        }
    }

    private fun askFishing(g: Game) {
        g.dialog.ask(listOf("Moechtest du die Angel auswerfen?"), listOf("Ja", "Nein")) { idx ->
            if (idx == 0) {
                val fish = map.randomFish()
                if (fish == null) g.dialog.say(listOf("Hier beisst nichts an ..."))
                else g.dialog.say(listOf("Etwas hat angebissen!")) { startWildBattle(g, fish) }
            }
        }
    }

    private fun useHealStation(g: Game) {
        g.dialog.ask(
            listOf("HEILSTATION: Soll dein Team vollstaendig geheilt werden?"),
            listOf("Ja", "Nein")
        ) { idx ->
            if (idx == 0) {
                g.state.healParty()
                g.state.healMap = mapId
                g.state.healX = g.state.px
                g.state.healY = g.state.py
                g.dialog.say(
                    listOf(
                        "Die Maschine summt ... dein Team ist wieder topfit!",
                        "Dieser Ort ist nun dein Rueckkehrpunkt."
                    )
                )
            }
        }
    }

    private fun talkTo(g: Game, n: NpcDef) {
        // Startmonster im Labor
        if (n.id.startsWith("ball_")) { chooseStarter(g, n); return }
        if (n.id == "titanox") { legendaryEncounter(g, n); return }

        when (n.kind) {
            NpcKind.ITEM -> {
                g.state.bag.add(n.itemId, n.itemCount)
                g.state.setFlag(itemFlag(n))
                val it = Items.get(n.itemId)
                g.dialog.say(listOf("Du findest ${n.itemCount}x ${it.name}!", it.desc))
            }
            NpcKind.HEALER -> {
                g.state.healParty()
                val lines = mutableListOf<String>()
                lines.addAll(n.lines)
                lines.add("Dein Team ist wieder vollstaendig geheilt!")
                if (mapId == "zuhause") {
                    g.state.healMap = "zuhause"; g.state.healX = 4; g.state.healY = 4
                }
                g.dialog.say(lines)
            }
            NpcKind.TRAINER -> {
                val t = n.trainer
                if (t == null) { g.dialog.say(n.lines); return }
                if (g.state.flags.contains(trainerFlag(n))) {
                    g.dialog.say(if (n.afterLines.isNotEmpty()) n.afterLines else n.lines)
                } else {
                    g.dialog.say(n.lines) { beginTrainerBattle(g, n) }
                }
            }
            else -> {
                // Nach einem Story-Fortschritt reden manche Leute anders
                if (n.afterFlag.isNotEmpty() && g.state.flags.contains(n.afterFlag) && n.afterLines.isNotEmpty()) {
                    g.dialog.say(n.afterLines)
                    return
                }
                // Bedingungen fuer Geschenke / Story-NPCs
                if (n.requiresBadges > 0 && g.state.badges < n.requiresBadges) {
                    g.dialog.say(n.lines)
                    return
                }
                if (n.givesItem.isNotEmpty()) {
                    val flag = "gift_${mapId}_${n.id}"
                    if (!g.state.flags.contains(flag)) {
                        g.state.bag.add(n.givesItem, n.givesItemCount)
                        g.state.setFlag(flag)
                        g.state.setFlag(n.setsFlag)
                        // Bei Bedingungen steht die Uebergabe in afterLines, sonst in lines.
                        val handover = if (n.requiresBadges > 0 && n.afterLines.isNotEmpty()) n.afterLines else n.lines
                        g.dialog.say(handover)
                        return
                    }
                    g.dialog.say(if (n.afterLines.isNotEmpty()) n.afterLines else n.lines)
                    return
                }
                if (n.requiresBadges > 0 && g.state.badges >= n.requiresBadges && n.afterLines.isNotEmpty()) {
                    g.state.setFlag(n.setsFlag)
                    g.dialog.say(n.afterLines)
                    return
                }
                g.dialog.say(n.lines)
            }
        }
    }

    private fun chooseStarter(g: Game, n: NpcDef) {
        if (g.state.flags.contains("starter")) {
            g.dialog.say(listOf("Die Kugel ist leer."))
            return
        }
        val id = n.id.removePrefix("ball_")
        val sp = Dex.get(id)
        g.dialog.ask(
            listOf("${sp.name} (${sp.types.joinToString("/") { it.deName }}) - moechtest du dieses Monster?"),
            listOf("Ja", "Nein")
        ) { idx ->
            if (idx == 0) {
                val m = Monster(id, 5)
                m.healFull()
                g.state.addMonster(m)
                g.state.setFlag("starter")
                g.state.bag.add("fangball", 5)
                g.state.bag.add("trank", 3)
                g.dialog.say(
                    listOf(
                        "Du erhaeltst ${sp.name}!",
                        "Prof. Eibe: Ausgezeichnete Wahl!",
                        "Prof. Eibe: Nimm noch diese 5 Fangbaelle und 3 Traenke mit.",
                        "Prof. Eibe: Und nun zieh los - Auronia wartet auf dich!"
                    )
                )
            }
        }
    }

    private fun legendaryEncounter(g: Game, n: NpcDef) {
        if (g.state.flags.contains("titanox_erledigt")) {
            g.dialog.say(n.afterLines)
            return
        }
        g.dialog.say(n.lines) {
            val wild = Monster("titanox", 50)
            wild.healFull()
            g.state.seen.add("titanox")
            val battle = Battle(g.state.party, mutableListOf(wild), BattleKind.WILD)
            g.fade(0.5f) {
                g.push(BattleScene(battle) { res ->
                    if (res == BattleResult.LOSE) whiteout(g)
                    else {
                        g.state.setFlag("titanox_erledigt")
                        if (res == BattleResult.CAUGHT) {
                            battle.caughtMonster?.let { m ->
                                val inParty = g.state.addMonster(m)
                                if (!inParty) g.toast("TITANOX wurde in die Box geschickt.")
                            }
                        }
                    }
                })
            }
        }
    }

    // ------------------------------------------------------------------
    override fun draw(g: Game, c: Canvas) {
        ensureMap(g)
        val m = map
        val t = Game.TILE
        val viewW = Game.VW
        val viewH = g.worldH

        val interp = if (moving) moveProgress else 1f
        val pxf = fromX + (g.state.px - fromX) * interp
        val pyf = fromY + (g.state.py - fromY) * interp
        val camX0 = pxf * t + t / 2f - viewW / 2f
        val camY0 = pyf * t + t / 2f - viewH / 2f
        val maxX = max(0f, m.width * t - viewW)
        val maxY = max(0f, m.height * t - viewH)
        val camX = if (m.width * t <= viewW) -(viewW - m.width * t) / 2f else camX0.coerceIn(0f, maxX)
        val camY = if (m.height * t <= viewH) -(viewH - m.height * t) / 2f else camY0.coerceIn(0f, maxY)

        Gfx.fillRect(c, 0f, 0f, viewW, viewH, if (m.indoor) 0xFF302838.toInt() else 0xFF204020.toInt())

        val x0 = ((camX / t).toInt() - 1).coerceAtLeast(0)
        val y0 = ((camY / t).toInt() - 1).coerceAtLeast(0)
        val x1 = (((camX + viewW) / t).toInt() + 1).coerceAtMost(m.width - 1)
        val y1 = (((camY + viewH) / t).toInt() + 1).coerceAtMost(m.height - 1)

        for (y in y0..y1) for (x in x0..x1) {
            Gfx.drawTile(c, m.tile(x, y), x, y, x * t - camX, y * t - camY, t, g.tick, m.theme)
        }

        // NPCs
        for (n in visibleNpcs(g)) {
            if (n.x < x0 - 1 || n.x > x1 + 1 || n.y < y0 - 1 || n.y > y1 + 1) continue
            Gfx.drawActor(c, n.style, n.dir, n.x * t - camX, n.y * t - camY, t, 0)
            if (n.trainer != null && !g.state.flags.contains(trainerFlag(n))) {
                Gfx.text(c, "!", n.x * t - camX + t / 2f, n.y * t - camY - 2f, 9f, 0xFFF8D040.toInt(), center = true)
            }
        }

        // Spieler
        Gfx.drawActor(c, "player", g.state.dir, pxf * t - camX, pyf * t - camY, t, if (moving) walkFrame else 0)

        // Dunkle Hoehle
        if (m.dark) {
            val cx = pxf * t - camX + t / 2f
            val cy = pyf * t - camY + t / 2f
            drawDarkness(c, viewW, viewH, cx, cy, 52f)
        }

        // Kopfzeile
        Gfx.panel(c, 3f, 3f, 96f, 22f)
        Gfx.text(c, m.name, 9f, 17f, 10f)
        if (g.state.badges > 0) {
            Gfx.panel(c, Game.VW - 52f, 3f, 49f, 22f)
            Gfx.text(c, "Siegel ${g.state.badges}/5", Game.VW - 48f, 17f, 9f)
        }

        g.dialog.draw(g, c)
    }

    private fun drawDarkness(c: Canvas, w: Float, h: Float, cx: Float, cy: Float, r: Float) {
        val dark = 0xE8000000.toInt()
        Gfx.fillRect(c, 0f, 0f, w, cy - r, dark)
        Gfx.fillRect(c, 0f, cy + r, w, h - (cy + r), dark)
        Gfx.fillRect(c, 0f, cy - r, cx - r, 2 * r, dark)
        Gfx.fillRect(c, cx + r, cy - r, w - (cx + r), 2 * r, dark)
        // weiche Ecken
        Gfx.p.color = 0xB0000000.toInt()
        Gfx.p.style = android.graphics.Paint.Style.STROKE
        Gfx.p.strokeWidth = 10f
        c.drawCircle(cx, cy, r + 5f, Gfx.p)
        Gfx.p.style = android.graphics.Paint.Style.FILL
    }
}
