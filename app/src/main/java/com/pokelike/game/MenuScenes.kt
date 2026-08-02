package com.pokelike.game

import android.graphics.Canvas
import kotlin.math.max
import kotlin.math.min

// ======================================================================
//  Hauptmenue
// ======================================================================
class MenuScene : Scene {
    private val items = listOf("MONSTER", "BEUTEL", "MONSTERDEX", "TYPEN-INFO", "BOX", "TRAINER", "SPEICHERN", "ZURUECK")
    private var index = 0
    override val opaque = false

    override fun update(g: Game, dt: Float) {
        if (g.dialog.active) { g.dialog.update(g, dt); return }
        if (g.input.repeated(Btn.UP)) index = (index - 1 + items.size) % items.size
        if (g.input.repeated(Btn.DOWN)) index = (index + 1) % items.size
        if (g.input.pressed(Btn.B) || g.input.pressed(Btn.START)) { g.pop(); return }
        if (g.input.pressed(Btn.A)) {
            when (items[index]) {
                "MONSTER" -> g.push(PartyScene())
                "BEUTEL" -> g.push(BagScene(inBattle = false))
                "MONSTERDEX" -> g.push(DexScene())
                "TYPEN-INFO" -> g.push(TypeInfoScene())
                "BOX" -> g.push(BoxScene())
                "TRAINER" -> g.push(TrainerCardScene())
                "SPEICHERN" -> {
                    g.dialog.ask(listOf("Spielstand speichern?"), listOf("Ja", "Nein")) { i ->
                        if (i == 0) {
                            if (g.save()) g.dialog.say(listOf("Spielstand gespeichert!"))
                            else g.dialog.say(listOf("Speichern fehlgeschlagen."))
                        }
                    }
                }
                else -> g.pop()
            }
        }
    }

    override fun draw(g: Game, c: Canvas) {
        val w = 108f
        val x = Game.VW - w - 5f
        val h = 12f + items.size * 15f
        Gfx.panel(c, x, 5f, w, h)
        for ((i, it) in items.withIndex()) {
            Gfx.text(c, it, x + 16f, 22f + i * 15f, 10f)
            if (i == index) Gfx.cursor(c, x + 7f, 18f + i * 15f)
        }
        Gfx.panel(c, 5f, 5f, 92f, 40f)
        Gfx.text(c, g.state.playerName, 11f, 19f, 10f)
        Gfx.text(c, "${g.state.money} Muenzen", 11f, 32f, 9f)
        Gfx.text(c, "Siegel: ${g.state.badges}/5", 11f, 42f, 9f)
    }
}

// ======================================================================
//  Team
// ======================================================================
class PartyScene(
    private val forcedSwitch: Boolean = false,
    private val onChosen: ((Int) -> Unit)? = null,
    private val onCancel: (() -> Unit)? = null,
    private val selectOnly: Boolean = false,
    private val itemToUse: String = ""
) : Scene {

    private var index = 0
    private var actionIndex = 0
    private var showActions = false
    private var swapFrom = -1

    override fun update(g: Game, dt: Float) {
        if (g.dialog.active) { g.dialog.update(g, dt); return }
        val party = g.state.party
        if (party.isEmpty()) { g.pop(); onCancel?.invoke(); return }

        if (showActions) {
            val actions = listOf("UEBERSICHT", "TAUSCHEN", "ZURUECK")
            if (g.input.repeated(Btn.UP)) actionIndex = (actionIndex - 1 + actions.size) % actions.size
            if (g.input.repeated(Btn.DOWN)) actionIndex = (actionIndex + 1) % actions.size
            if (g.input.pressed(Btn.B)) { showActions = false; return }
            if (g.input.pressed(Btn.A)) {
                when (actionIndex) {
                    0 -> { showActions = false; g.push(SummaryScene(party[index])) }
                    1 -> { showActions = false; swapFrom = index; g.toast("Zweites Monster waehlen") }
                    else -> showActions = false
                }
            }
            return
        }

        if (g.input.repeated(Btn.UP)) index = (index - 1 + party.size) % party.size
        if (g.input.repeated(Btn.DOWN)) index = (index + 1) % party.size

        if (g.input.pressed(Btn.B)) {
            if (swapFrom >= 0) { swapFrom = -1; return }
            if (forcedSwitch) { g.toast("Du musst ein Monster einsetzen!"); return }
            g.pop()
            onCancel?.invoke()
            return
        }

        if (g.input.pressed(Btn.A)) {
            if (swapFrom >= 0) {
                if (swapFrom != index) {
                    val tmp = party[swapFrom]
                    party[swapFrom] = party[index]
                    party[index] = tmp
                }
                swapFrom = -1
                return
            }
            if (forcedSwitch) {
                if (party[index].isFainted) { g.toast("Dieses Monster kann nicht kaempfen!"); return }
                g.pop()
                onChosen?.invoke(index)
                return
            }
            if (itemToUse.isNotEmpty()) {
                applyFieldItem(g, itemToUse, index)
                return
            }
            if (onChosen != null) {
                g.pop()
                onChosen.invoke(index)
                return
            }
            showActions = true
            actionIndex = 0
        }
    }

    private fun applyFieldItem(g: Game, itemId: String, target: Int) {
        val item = Items.get(itemId)
        val mon = g.state.party[target]
        var msg: String? = null
        when {
            item.levelUp -> {
                if (mon.level >= Monster.MAX_LEVEL) msg = null
                else {
                    val lv = mon.gainExp(max(1, Monster.expForLevel(mon.level + 1) - mon.exp))
                    msg = "${mon.name} erreicht Level ${mon.level}!"
                    for (l in lv) for (mv in mon.movesLearnedAt(l)) {
                        if (mon.moves.size < 4) { mon.learnMove(mv); msg += " Erlernt ${Moves.get(mv).name}!" }
                    }
                }
            }
            item.vitaminStat != null -> {
                val idx = when (item.vitaminStat) {
                    Stat.HP -> 0; Stat.ATK -> 1; Stat.DEF -> 2; Stat.SPA -> 3; Stat.SPD -> 4; else -> 5
                }
                if (mon.bonus[idx] >= 30) msg = null
                else {
                    mon.bonus[idx] = min(30, mon.bonus[idx] + 5)
                    if (idx == 0) mon.currentHp = min(mon.maxHp, mon.currentHp + 5)
                    msg = "${item.vitaminStat.deName} von ${mon.name} steigt!"
                }
            }
            item.stone != null -> {
                val target2 = mon.evolutionTarget(item.stone)
                if (target2 == null) msg = null
                else {
                    val old = mon.name
                    mon.evolveInto(target2)
                    msg = "$old entwickelt sich zu ${mon.name}!"
                }
            }
            else -> msg = Battle.applyHealingItem(item, mon)
        }
        if (msg == null) {
            g.dialog.say(listOf("Das haette keine Wirkung."))
        } else {
            g.state.bag.remove(itemId, 1)
            val m = msg
            g.dialog.say(listOf("Du benutzt ${item.name}.", m)) {
                if (!g.state.bag.has(itemId)) { g.pop() }
            }
        }
    }

    override fun draw(g: Game, c: Canvas) {
        Gfx.fillRect(c, 0f, 0f, Game.VW, g.worldH, 0xFF404C68.toInt())
        Gfx.text(c, if (forcedSwitch) "Wer soll kaempfen?" else "TEAM", 8f, 15f, 11f, Gfx.WHITE)
        if (itemToUse.isNotEmpty()) Gfx.text(c, "Ziel fuer ${Items.get(itemToUse).name}", 8f, 26f, 9f, 0xFFF0D060.toInt())

        val party = g.state.party
        val top = 30f
        val rowH = 34f
        for (i in party.indices) {
            val m = party[i]
            val y = top + i * rowH
            val bg = if (i == index) 0xFFF8F0D0.toInt() else Gfx.UI_BG
            Gfx.panel(c, 6f, y, Game.VW - 12f, rowH - 3f, bg)
            Sprites.drawMonster(c, m.speciesId, 8f, y - 2f, 30f)
            Gfx.text(c, m.name, 40f, y + 13f, 10f)
            Gfx.text(c, "Lv${m.level}", 40f, y + 25f, 9f)
            Gfx.hpBar(c, 84f, y + 8f, 92f, 6f, m.hpRatio)
            Gfx.text(c, "${m.currentHp}/${m.maxHp}", 84f, y + 25f, 9f)
            Gfx.statusTag(c, m.status, 140f, y + 18f, 7f)
            var ty = y + 4f
            for (t in m.species.types) {
                Gfx.typeBadge(c, t, 182f, ty, 6f)
                ty += 12f
            }
            if (i == swapFrom) Gfx.strokeRect(c, 6f, y, Game.VW - 12f, rowH - 3f, 0xFFE0A020.toInt(), 2f)
        }

        if (showActions) {
            val actions = listOf("UEBERSICHT", "TAUSCHEN", "ZURUECK")
            val w = 92f
            val x = Game.VW - w - 8f
            val y = g.worldH - 8f - (10f + actions.size * 15f)
            Gfx.panel(c, x, y, w, 10f + actions.size * 15f)
            for ((i, a) in actions.withIndex()) {
                Gfx.text(c, a, x + 16f, y + 20f + i * 15f, 10f)
                if (i == actionIndex) Gfx.cursor(c, x + 7f, y + 16f + i * 15f)
            }
        } else {
            Gfx.text(c, "A = Auswahl   B = zurueck", Game.VW / 2f, g.worldH - 8f, 9f, 0xFFD8E0F0.toInt(), center = true)
        }
        g.dialog.draw(g, c)
    }
}

// ======================================================================
//  Monster-Uebersicht
// ======================================================================
class SummaryScene(private val m: Monster) : Scene {
    private var page = 0

    override fun update(g: Game, dt: Float) {
        if (g.input.pressed(Btn.B)) { g.pop(); return }
        if (g.input.repeated(Btn.LEFT) || g.input.repeated(Btn.RIGHT) || g.input.pressed(Btn.A)) page = 1 - page
    }

    override fun draw(g: Game, c: Canvas) {
        Gfx.fillRect(c, 0f, 0f, Game.VW, g.worldH, 0xFF404C68.toInt())
        Gfx.panel(c, 5f, 5f, Game.VW - 10f, 62f)
        Sprites.drawMonster(c, m.speciesId, 8f, 6f, 58f)
        Gfx.text(c, m.name, 70f, 20f, 12f)
        Gfx.text(c, "Nr. ${m.species.dexNo}   Lv${m.level}", 70f, 33f, 9f)
        var bx = 70f
        for (t in m.species.types) {
            Gfx.typeBadge(c, t, bx, 38f, 7f)
            bx += Gfx.textWidth(t.deName.uppercase(), 7f) + 14f
        }
        Gfx.hpBar(c, 70f, 54f, 100f, 7f, m.hpRatio)
        Gfx.text(c, "${m.currentHp}/${m.maxHp}", 174f, 61f, 9f)

        if (page == 0) {
            Gfx.panel(c, 5f, 71f, Game.VW - 10f, 108f)
            val stats = listOf(
                "Angriff" to m.stat(Stat.ATK), "Verteidigung" to m.stat(Stat.DEF),
                "Sp-Angriff" to m.stat(Stat.SPA), "Sp-Verteidigung" to m.stat(Stat.SPD),
                "Initiative" to m.stat(Stat.SPE)
            )
            Gfx.text(c, "WERTE", 12f, 85f, 10f, Gfx.ACCENT)
            for ((i, s) in stats.withIndex()) {
                Gfx.text(c, s.first, 12f, 100f + i * 13f, 9.5f)
                Gfx.text(c, s.second.toString(), 130f, 100f + i * 13f, 9.5f, right = true)
                Gfx.fillRect(c, 136f, 94f + i * 13f, (s.second.coerceAtMost(200) / 200f) * 90f, 6f, 0xFF68A8E0.toInt())
            }
            Gfx.text(c, "EP bis Level ${m.level + 1}: ${m.expToNext()}", 12f, 172f, 9f, 0xFF505868.toInt())
            Gfx.expBar(c, 12f, 162f, Game.VW - 34f, 5f, m.expInLevel().toFloat() / m.expLevelSpan())
            Gfx.panel(c, 5f, 183f, Game.VW - 10f, 40f)
            var ty = 196f
            for (ln in Gfx.wrap(m.species.dexText, 36)) {
                Gfx.text(c, ln, 12f, ty, 9f)
                ty += 11f
            }
        } else {
            Gfx.panel(c, 5f, 71f, Game.VW - 10f, 150f)
            Gfx.text(c, "ATTACKEN", 12f, 85f, 10f, Gfx.ACCENT)
            for ((i, slot) in m.moves.withIndex()) {
                val y = 94f + i * 34f
                val mv = slot.move
                Gfx.text(c, mv.name, 12f, y + 12f, 10f)
                Gfx.typeBadge(c, mv.type, 12f, y + 15f, 6.5f)
                val kat = when (mv.cat) {
                    MoveCategory.PHYSISCH -> "PHYS"; MoveCategory.SPEZIAL -> "SPEZ"; else -> "STATUS"
                }
                Gfx.text(c, "$kat  ST ${if (mv.power > 0) mv.power else "-"}  GEN ${if (mv.acc > 0) mv.acc else "-"}",
                    70f, y + 24f, 8.5f, 0xFF505868.toInt())
                Gfx.text(c, "AP ${slot.pp}/${slot.maxPp}", Game.VW - 14f, y + 12f, 9f, right = true)
            }
        }
        Gfx.text(c, "LINKS/RECHTS = Seite    B = zurueck", Game.VW / 2f, g.worldH - 8f, 9f, 0xFFD8E0F0.toInt(), center = true)
    }
}

// ======================================================================
//  Beutel
// ======================================================================
class BagScene(
    private val inBattle: Boolean,
    private val onClose: (() -> Unit)? = null,
    private val onUse: ((String) -> Unit)? = null
) : Scene {

    private val cats = ItemCategory.values().toList()
    private var cat = 0
    private var index = 0

    private fun list(g: Game): List<Pair<Item, Int>> = g.state.bag.byCategory(cats[cat])

    override fun update(g: Game, dt: Float) {
        if (g.dialog.active) { g.dialog.update(g, dt); return }
        if (g.input.repeated(Btn.LEFT)) { cat = (cat - 1 + cats.size) % cats.size; index = 0 }
        if (g.input.repeated(Btn.RIGHT)) { cat = (cat + 1) % cats.size; index = 0 }
        val items = list(g)
        if (items.isNotEmpty()) {
            if (g.input.repeated(Btn.UP)) index = (index - 1 + items.size) % items.size
            if (g.input.repeated(Btn.DOWN)) index = (index + 1) % items.size
        }
        if (g.input.pressed(Btn.B)) { g.pop(); onClose?.invoke(); return }
        if (g.input.pressed(Btn.A) && items.isNotEmpty()) {
            val item = items[index.coerceIn(0, items.size - 1)].first
            if (inBattle) {
                if (!item.usableInBattle) { g.toast("Das geht im Kampf nicht."); return }
                g.pop()
                onUse?.invoke(item.id)
            } else {
                useInField(g, item)
            }
        }
    }

    private fun useInField(g: Game, item: Item) {
        when {
            item.ballRate > 0.0 -> g.dialog.say(listOf("Baelle kannst du nur im Kampf einsetzen."))
            item.repelSteps > 0 -> {
                g.state.repelSteps = item.repelSteps
                g.state.bag.remove(item.id, 1)
                g.dialog.say(listOf("Du benutzt ${item.name}.", "Wilde Monster halten sich fern!"))
            }
            item.escapeRope -> {
                g.state.bag.remove(item.id, 1)
                g.dialog.say(listOf("Du benutzt das Fluchtseil!")) {
                    g.fade(0.6f) {
                        g.state.mapId = g.state.healMap
                        g.state.px = g.state.healX
                        g.state.py = g.state.healY
                        while (g.top() !is OverworldScene) g.pop()
                    }
                }
            }
            item.id == "angel" -> g.dialog.say(listOf("Stell dich ans Ufer und benutze A am Wasser."))
            item.keyItem -> g.dialog.say(listOf(item.desc))
            item.needsTarget -> g.push(PartyScene(itemToUse = item.id))
            else -> g.dialog.say(listOf(item.desc))
        }
    }

    override fun draw(g: Game, c: Canvas) {
        Gfx.fillRect(c, 0f, 0f, Game.VW, g.worldH, 0xFF3E5A48.toInt())
        Gfx.panel(c, 5f, 5f, Game.VW - 10f, 20f)
        Gfx.text(c, "< ${cats[cat].deName} >", Game.VW / 2f, 19f, 11f, center = true)

        val items = list(g)
        Gfx.panel(c, 5f, 29f, Game.VW - 10f, g.worldH - 90f)
        if (items.isEmpty()) {
            Gfx.text(c, "Keine Items in dieser Kategorie.", Game.VW / 2f, 60f, 9f, center = true)
        } else {
            val maxRows = ((g.worldH - 100f) / 16f).toInt().coerceAtLeast(3)
            val start = (index - maxRows / 2).coerceIn(0, max(0, items.size - maxRows))
            for (i in start until min(items.size, start + maxRows)) {
                val y = 45f + (i - start) * 16f
                val (item, n) = items[i]
                Gfx.text(c, item.name, 22f, y, 10f)
                Gfx.text(c, "x$n", Game.VW - 16f, y, 10f, right = true)
                if (i == index) Gfx.cursor(c, 12f, y - 4f)
            }
        }
        val sel = items.getOrNull(index)
        Gfx.panel(c, 5f, g.worldH - 58f, Game.VW - 10f, 40f)
        if (sel != null) {
            var y = g.worldH - 44f
            for (ln in Gfx.wrap(sel.first.desc, 36)) {
                Gfx.text(c, ln, 12f, y, 9f)
                y += 11f
            }
        }
        Gfx.text(c, "A = benutzen   B = zurueck", Game.VW / 2f, g.worldH - 6f, 9f, 0xFFD8E0F0.toInt(), center = true)
        g.dialog.draw(g, c)
    }
}

// ======================================================================
//  Laden
// ======================================================================
class ShopScene(private val stock: List<String>) : Scene {
    private var index = 0
    private var amount = 1

    override fun update(g: Game, dt: Float) {
        if (g.dialog.active) { g.dialog.update(g, dt); return }
        if (stock.isEmpty()) { g.pop(); return }
        if (g.input.repeated(Btn.UP)) { index = (index - 1 + stock.size) % stock.size; amount = 1 }
        if (g.input.repeated(Btn.DOWN)) { index = (index + 1) % stock.size; amount = 1 }
        if (g.input.repeated(Btn.RIGHT)) amount = min(99, amount + 1)
        if (g.input.repeated(Btn.LEFT)) amount = max(1, amount - 1)
        if (g.input.pressed(Btn.B)) { g.pop(); return }
        if (g.input.pressed(Btn.A)) {
            val item = Items.get(stock[index])
            val cost = item.price * amount
            if (cost > g.state.money) {
                g.dialog.say(listOf("Dafuer hast du nicht genug Muenzen!"))
            } else {
                g.state.money -= cost
                g.state.bag.add(item.id, amount)
                g.dialog.say(listOf("${amount}x ${item.name} gekauft! (-$cost Muenzen)"))
                amount = 1
            }
        }
    }

    override fun draw(g: Game, c: Canvas) {
        Gfx.fillRect(c, 0f, 0f, Game.VW, g.worldH, 0xFF3E4A68.toInt())
        Gfx.panel(c, 5f, 5f, Game.VW - 10f, 22f)
        Gfx.text(c, "MARKTSTAND", 12f, 20f, 11f)
        Gfx.text(c, "${g.state.money} Muenzen", Game.VW - 12f, 20f, 10f, right = true)

        Gfx.panel(c, 5f, 31f, Game.VW - 10f, g.worldH - 96f)
        val maxRows = ((g.worldH - 106f) / 16f).toInt().coerceAtLeast(3)
        val start = (index - maxRows / 2).coerceIn(0, max(0, stock.size - maxRows))
        for (i in start until min(stock.size, start + maxRows)) {
            val item = Items.get(stock[i])
            val y = 47f + (i - start) * 16f
            Gfx.text(c, item.name, 22f, y, 10f)
            Gfx.text(c, "${item.price}", Game.VW - 16f, y, 10f, right = true)
            if (i == index) Gfx.cursor(c, 12f, y - 4f)
        }
        val item = Items.get(stock[index.coerceIn(0, stock.size - 1)])
        Gfx.panel(c, 5f, g.worldH - 62f, Game.VW - 10f, 44f)
        var y = g.worldH - 48f
        for (ln in Gfx.wrap(item.desc, 36)) { Gfx.text(c, ln, 12f, y, 9f); y += 11f }
        Gfx.text(c, "Menge: $amount   Preis: ${item.price * amount}", 12f, g.worldH - 24f, 9.5f, Gfx.ACCENT)
        Gfx.text(c, "LINKS/RECHTS = Menge   A = kaufen   B = zurueck",
            Game.VW / 2f, g.worldH - 6f, 8.5f, 0xFFD8E0F0.toInt(), center = true)
        g.dialog.draw(g, c)
    }
}

// ======================================================================
//  Monsterdex
// ======================================================================
class DexScene : Scene {
    private var index = 0
    private var detail = false

    override fun update(g: Game, dt: Float) {
        val all = Dex.all()
        if (g.input.repeated(Btn.UP)) index = (index - 1 + all.size) % all.size
        if (g.input.repeated(Btn.DOWN)) index = (index + 1) % all.size
        if (g.input.pressed(Btn.A)) detail = !detail
        if (g.input.pressed(Btn.B)) { if (detail) detail = false else g.pop() }
    }

    override fun draw(g: Game, c: Canvas) {
        Gfx.fillRect(c, 0f, 0f, Game.VW, g.worldH, 0xFF5A3E4A.toInt())
        val all = Dex.all()
        val sp = all[index]
        val seen = g.state.seen.contains(sp.id)
        val caught = g.state.caught.contains(sp.id)

        Gfx.panel(c, 5f, 5f, Game.VW - 10f, 20f)
        Gfx.text(c, "MONSTERDEX", 12f, 19f, 11f)
        Gfx.text(c, "${g.state.caught.size}/${Dex.count()} gefangen", Game.VW - 12f, 19f, 9f, right = true)

        if (!detail) {
            Gfx.panel(c, 5f, 29f, Game.VW - 10f, g.worldH - 42f)
            val maxRows = ((g.worldH - 56f) / 15f).toInt().coerceAtLeast(4)
            val start = (index - maxRows / 2).coerceIn(0, max(0, all.size - maxRows))
            for (i in start until min(all.size, start + maxRows)) {
                val s = all[i]
                val y = 44f + (i - start) * 15f
                val known = g.state.seen.contains(s.id)
                val nm = if (known) s.name else "-----"
                Gfx.text(c, String.format("%03d", s.dexNo), 22f, y, 9f, 0xFF707888.toInt())
                Gfx.text(c, nm, 52f, y, 10f)
                if (g.state.caught.contains(s.id)) Gfx.text(c, "*", 42f, y, 10f, Gfx.ACCENT)
                if (i == index) Gfx.cursor(c, 12f, y - 4f)
            }
        } else {
            Gfx.panel(c, 5f, 29f, Game.VW - 10f, g.worldH - 42f)
            if (!seen) {
                Gfx.text(c, "Noch keine Daten.", Game.VW / 2f, 80f, 10f, center = true)
            } else {
                Sprites.drawMonster(c, sp.id, 10f, 34f, 64f)
                Gfx.text(c, "${String.format("%03d", sp.dexNo)}  ${sp.name}", 82f, 50f, 12f)
                var bx = 82f
                for (t in sp.types) {
                    Gfx.typeBadge(c, t, bx, 58f, 7f)
                    bx += Gfx.textWidth(t.deName.uppercase(), 7f) + 14f
                }
                Gfx.text(c, if (caught) "Gefangen" else "Gesichtet", 82f, 86f, 9f, Gfx.ACCENT)
                var y = 112f
                for (ln in Gfx.wrap(sp.dexText, 36)) { Gfx.text(c, ln, 12f, y, 9.5f); y += 12f }
                y += 6f
                Gfx.text(c, "Basiswerte", 12f, y, 10f, Gfx.ACCENT); y += 13f
                val stats = listOf("KP" to sp.hp, "ANG" to sp.atk, "VER" to sp.def,
                    "SPA" to sp.spa, "SPV" to sp.spd, "INI" to sp.spe)
                for ((i, s) in stats.withIndex()) {
                    val yy = y + (i / 2) * 13f
                    val xx = 12f + (i % 2) * 112f
                    Gfx.text(c, "${s.first} ${s.second}", xx, yy, 9f)
                    Gfx.fillRect(c, xx + 44f, yy - 6f, (s.second / 150f).coerceIn(0f, 1f) * 60f, 5f, 0xFF68A8E0.toInt())
                }
                sp.evolution?.let {
                    val txt = if (it.stone != null) "Entwickelt sich mit ${Items.get(it.stone).name}"
                    else "Entwickelt sich ab Level ${it.level}"
                    Gfx.text(c, txt, 12f, y + 32f, 9f, 0xFF505868.toInt())
                }
            }
        }
        Gfx.text(c, "A = Details   B = zurueck", Game.VW / 2f, g.worldH - 6f, 9f, 0xFFE8D8E0.toInt(), center = true)
    }
}

// ======================================================================
//  Typen-Info
// ======================================================================
class TypeInfoScene : Scene {
    private var index = 0

    override fun update(g: Game, dt: Float) {
        val types = Type.values()
        if (g.input.repeated(Btn.LEFT) || g.input.repeated(Btn.UP)) index = (index - 1 + types.size) % types.size
        if (g.input.repeated(Btn.RIGHT) || g.input.repeated(Btn.DOWN)) index = (index + 1) % types.size
        if (g.input.pressed(Btn.B)) g.pop()
    }

    override fun draw(g: Game, c: Canvas) {
        Gfx.fillRect(c, 0f, 0f, Game.VW, g.worldH, 0xFF34405C.toInt())
        val t = Type.values()[index]
        Gfx.panel(c, 5f, 5f, Game.VW - 10f, 20f)
        Gfx.text(c, "TYPEN-INFO", 12f, 19f, 11f)

        Gfx.panel(c, 5f, 29f, Game.VW - 10f, 34f)
        Gfx.text(c, "Angreifender Typ:", 12f, 43f, 9f)
        Gfx.typeBadge(c, t, 12f, 47f, 10f)
        Gfx.text(c, "< >", Game.VW - 20f, 50f, 12f, right = true)

        val strong = Type.values().filter { TypeChart.single(t, it) > 1.0 }
        val weak = Type.values().filter { TypeChart.single(t, it) in 0.01..0.99 }
        val none = Type.values().filter { TypeChart.single(t, it) == 0.0 }
        val defWeak = Type.values().filter { TypeChart.single(it, t) > 1.0 }
        val defRes = Type.values().filter { TypeChart.single(it, t) in 0.01..0.99 }

        Gfx.panel(c, 5f, 67f, Game.VW - 10f, g.worldH - 82f)
        var y = 82f
        fun block(title: String, list: List<Type>, color: Int) {
            Gfx.text(c, title, 12f, y, 9.5f, color)
            y += 12f
            if (list.isEmpty()) {
                Gfx.text(c, "-", 16f, y, 9f); y += 13f
            } else {
                var x = 14f
                for (tt in list) {
                    val w = Gfx.textWidth(tt.deName.uppercase(), 6.5f) + 12f
                    if (x + w > Game.VW - 16f) { x = 14f; y += 13f }
                    Gfx.typeBadge(c, tt, x, y - 7f, 6.5f)
                    x += w
                }
                y += 17f
            }
        }
        block("Sehr effektiv gegen (x2):", strong, 0xFF208030.toInt())
        block("Wenig effektiv gegen (x0.5):", weak, 0xFFB06018.toInt())
        block("Keine Wirkung gegen (x0):", none, 0xFF902020.toInt())
        y += 4f
        block("Anfaellig gegen:", defWeak, 0xFF902020.toInt())
        block("Widerstandsfaehig gegen:", defRes, 0xFF208030.toInt())

        Gfx.text(c, "Attacken vom eigenen Typ: +50% Schaden (Typ-Bonus)",
            Game.VW / 2f, g.worldH - 18f, 8.5f, 0xFFD8E0F0.toInt(), center = true)
        Gfx.text(c, "LINKS/RECHTS = Typ   B = zurueck", Game.VW / 2f, g.worldH - 6f, 9f, 0xFFD8E0F0.toInt(), center = true)
    }
}

// ======================================================================
//  Box
// ======================================================================
class BoxScene : Scene {
    private var index = 0

    override fun update(g: Game, dt: Float) {
        if (g.dialog.active) { g.dialog.update(g, dt); return }
        val box = g.state.box
        if (g.input.pressed(Btn.B)) { g.pop(); return }
        if (box.isEmpty()) return
        if (g.input.repeated(Btn.UP)) index = (index - 1 + box.size) % box.size
        if (g.input.repeated(Btn.DOWN)) index = (index + 1) % box.size
        if (g.input.pressed(Btn.A)) {
            if (g.state.party.size < 6) {
                val m = box.removeAt(index)
                g.state.party.add(m)
                index = index.coerceAtMost(max(0, box.size - 1))
                g.dialog.say(listOf("${m.name} kommt ins Team!"))
            } else {
                g.dialog.say(listOf("Dein Team ist voll (6 Monster)."))
            }
        }
    }

    override fun draw(g: Game, c: Canvas) {
        Gfx.fillRect(c, 0f, 0f, Game.VW, g.worldH, 0xFF3A5468.toInt())
        Gfx.panel(c, 5f, 5f, Game.VW - 10f, 20f)
        Gfx.text(c, "MONSTER-BOX", 12f, 19f, 11f)
        val box = g.state.box
        Gfx.panel(c, 5f, 29f, Game.VW - 10f, g.worldH - 44f)
        if (box.isEmpty()) {
            Gfx.text(c, "Die Box ist leer.", Game.VW / 2f, 70f, 10f, center = true)
        } else {
            val maxRows = ((g.worldH - 60f) / 24f).toInt().coerceAtLeast(3)
            val start = (index - maxRows / 2).coerceIn(0, max(0, box.size - maxRows))
            for (i in start until min(box.size, start + maxRows)) {
                val m = box[i]
                val y = 34f + (i - start) * 24f
                Sprites.drawMonster(c, m.speciesId, 16f, y, 22f)
                Gfx.text(c, m.name, 44f, y + 15f, 10f)
                Gfx.text(c, "Lv${m.level}", Game.VW - 16f, y + 15f, 9f, right = true)
                if (i == index) Gfx.cursor(c, 8f, y + 11f)
            }
        }
        Gfx.text(c, "A = ins Team   B = zurueck", Game.VW / 2f, g.worldH - 6f, 9f, 0xFFD8E0F0.toInt(), center = true)
        g.dialog.draw(g, c)
    }
}

// ======================================================================
//  Trainerkarte
// ======================================================================
class TrainerCardScene : Scene {
    override fun update(g: Game, dt: Float) {
        if (g.input.pressed(Btn.B) || g.input.pressed(Btn.A)) g.pop()
    }

    override fun draw(g: Game, c: Canvas) {
        Gfx.fillRect(c, 0f, 0f, Game.VW, g.worldH, 0xFF2E4A6A.toInt())
        Gfx.panel(c, 8f, 12f, Game.VW - 16f, 150f)
        Gfx.text(c, "TRAINERKARTE", 18f, 30f, 12f, Gfx.ACCENT)
        Gfx.drawActor(c, "player", 0, Game.VW - 56f, 20f, 34f, 0)
        Gfx.text(c, "Name:  ${g.state.playerName}", 18f, 52f, 10f)
        Gfx.text(c, "Geld:  ${g.state.money} Muenzen", 18f, 68f, 10f)
        val h = g.state.playTimeMs / 3600000
        val mi = (g.state.playTimeMs / 60000) % 60
        Gfx.text(c, "Zeit:  ${h}h ${mi}min", 18f, 84f, 10f)
        Gfx.text(c, "Schritte: ${g.state.steps}", 18f, 100f, 10f)
        Gfx.text(c, "Dex:   ${g.state.caught.size}/${Dex.count()} gefangen", 18f, 116f, 10f)
        Gfx.text(c, "SIEGEL", 18f, 138f, 10f, Gfx.ACCENT)
        val names = listOf("Kupfer", "Wellen", "Glut", "Frost", "Sturm")
        for (i in 0 until 5) {
            val x = 18f + i * 42f
            val has = g.state.flags.contains("siegel${i + 1}")
            Gfx.p.color = if (has) intArrayOf(
                0xFFC08040.toInt(), 0xFF4090D0.toInt(), 0xFFE06030.toInt(),
                0xFF90D8E8.toInt(), 0xFFE0C830.toInt()
            )[i] else 0xFFB0B0B8.toInt()
            c.drawCircle(x + 14f, 152f, 11f, Gfx.p)
            Gfx.text(c, names[i], x + 14f, 168f, 7.5f, if (has) Gfx.BLACK else 0xFF909098.toInt(), center = true)
        }
        val story = when {
            g.state.flags.contains("champion_besiegt") -> "Du bist Champion von Auronia!"
            g.state.flags.contains("morgana_besiegt") -> "Der Schattenorden ist besiegt. Auf zur Liga!"
            g.state.badges >= 5 -> "Alle Siegel! Das Schattental wartet."
            else -> "Sammle die fuenf Siegel der Arenen."
        }
        Gfx.panel(c, 8f, 176f, Game.VW - 16f, 34f)
        var y = 190f
        for (ln in Gfx.wrap(story, 34)) { Gfx.text(c, ln, 16f, y, 9.5f); y += 12f }
        Gfx.text(c, "B = zurueck", Game.VW / 2f, g.worldH - 8f, 9f, 0xFFD8E0F0.toInt(), center = true)
    }
}

// ======================================================================
//  Attacke ersetzen
// ======================================================================
class MoveLearnScene(
    private val m: Monster,
    private val newMoveId: String,
    private val onDone: () -> Unit
) : Scene {
    private var index = 0

    override fun update(g: Game, dt: Float) {
        if (g.dialog.active) { g.dialog.update(g, dt); return }
        if (g.input.repeated(Btn.UP)) index = (index - 1 + m.moves.size) % m.moves.size
        if (g.input.repeated(Btn.DOWN)) index = (index + 1) % m.moves.size
        if (g.input.pressed(Btn.B)) {
            g.pop()
            g.dialog.say(listOf("${m.name} hat ${Moves.get(newMoveId).name} nicht erlernt."))
            onDone()
            return
        }
        if (g.input.pressed(Btn.A)) {
            val old = m.moves[index].move.name
            m.replaceMove(index, newMoveId)
            g.pop()
            g.dialog.say(listOf("$old wurde vergessen.", "${m.name} erlernt ${Moves.get(newMoveId).name}!"))
            onDone()
        }
    }

    override fun draw(g: Game, c: Canvas) {
        Gfx.fillRect(c, 0f, 0f, Game.VW, g.worldH, 0xFF404C68.toInt())
        Gfx.panel(c, 5f, 5f, Game.VW - 10f, 30f)
        Gfx.text(c, "Welche Attacke vergessen?", 12f, 24f, 10f)
        for ((i, slot) in m.moves.withIndex()) {
            val y = 42f + i * 28f
            Gfx.panel(c, 5f, y, Game.VW - 10f, 25f, if (i == index) 0xFFF8F0D0.toInt() else Gfx.UI_BG)
            Gfx.text(c, slot.move.name, 26f, y + 12f, 10f)
            Gfx.typeBadge(c, slot.move.type, 26f, y + 14f, 6.5f)
            Gfx.text(c, "ST ${if (slot.move.power > 0) slot.move.power else "-"}  AP ${slot.pp}/${slot.maxPp}",
                Game.VW - 14f, y + 12f, 9f, right = true)
            if (i == index) Gfx.cursor(c, 14f, y + 12f)
        }
        val nm = Moves.get(newMoveId)
        Gfx.panel(c, 5f, 160f, Game.VW - 10f, 46f)
        Gfx.text(c, "NEU: ${nm.name}", 12f, 174f, 10f, Gfx.ACCENT)
        Gfx.typeBadge(c, nm.type, 12f, 178f, 6.5f)
        Gfx.text(c, "ST ${if (nm.power > 0) nm.power else "-"}  AP ${nm.pp}", Game.VW - 14f, 190f, 9f, right = true)
        for ((i, ln) in Gfx.wrap(nm.desc, 36).withIndex()) {
            Gfx.text(c, ln, 12f, 202f + i * 11f, 9f)
        }
        Gfx.text(c, "A = ersetzen   B = abbrechen", Game.VW / 2f, g.worldH - 6f, 9f, 0xFFD8E0F0.toInt(), center = true)
    }
}
