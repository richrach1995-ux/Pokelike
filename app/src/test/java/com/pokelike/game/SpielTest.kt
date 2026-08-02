package com.pokelike.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Prueft Spieldaten, Kampf-Engine und Durchspielbarkeit ohne Android-Geraet.
 */
class SpielTest {

    // ------------------------------------------------------------------ Daten
    @Test
    fun artenDatenSindGueltig() {
        val moveIds = Moves.all().map { it.id }.toSet()
        val speciesIds = Dex.all().map { it.id }.toSet()
        val fehler = mutableListOf<String>()

        for (sp in Dex.all()) {
            if (sp.learnset.isEmpty()) fehler.add("${sp.id}: leeres Lernset")
            for ((lv, mv) in sp.learnset) {
                if (mv !in moveIds) fehler.add("${sp.id}: unbekannte Attacke $mv")
                if (lv !in 1..100) fehler.add("${sp.id}: Level $lv ungueltig")
            }
            sp.evolution?.let {
                if (it.toId !in speciesIds) fehler.add("${sp.id}: Entwicklung zu ${it.toId} unbekannt")
                if (it.level == 0 && it.stone == null) fehler.add("${sp.id}: Entwicklung ohne Bedingung")
                if (it.stone != null && !Items.exists(it.stone)) fehler.add("${sp.id}: Stein ${it.stone} unbekannt")
            }
            if (SpriteArt.monsterTemplates[sp.sprite] == null) fehler.add("${sp.id}: Sprite ${sp.sprite} fehlt")
            if (sp.catchRate !in 1..255) fehler.add("${sp.id}: Fangrate ungueltig")

            val m = Monster(sp.id, 50)
            if (m.maxHp < 20) fehler.add("${sp.id}: zu wenig KP")
            if (m.moves.isEmpty()) fehler.add("${sp.id}: keine Attacken auf Level 50")
            if (m.moves.size > 4) fehler.add("${sp.id}: mehr als 4 Attacken")
        }
        assertTrue(fehler.joinToString("\n"), fehler.isEmpty())
    }

    @Test
    fun dexNummernSindEindeutig() {
        val nummern = Dex.all().map { it.dexNo }
        assertEquals(nummern.size, nummern.toSet().size)
        val ids = Dex.all().map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun attackenSindPlausibel() {
        val fehler = mutableListOf<String>()
        for (m in Moves.all()) {
            if (m.cat == MoveCategory.STATUS && m.power > 0) fehler.add("${m.id}: Statusattacke mit Staerke")
            if (m.cat != MoveCategory.STATUS && m.power <= 0) fehler.add("${m.id}: Angriff ohne Staerke")
            if (m.pp <= 0) fehler.add("${m.id}: keine AP")
            if (m.acc !in 0..100) fehler.add("${m.id}: Genauigkeit ungueltig")
            if (m.minHits > m.maxHits) fehler.add("${m.id}: Trefferzahl ungueltig")
        }
        assertTrue(fehler.joinToString("\n"), fehler.isEmpty())
    }

    @Test
    fun typentabelleStimmt() {
        assertEquals(2.0, TypeChart.single(Type.FEUER, Type.PFLANZE), 0.001)
        assertEquals(0.5, TypeChart.single(Type.FEUER, Type.WASSER), 0.001)
        assertEquals(0.0, TypeChart.single(Type.NORMAL, Type.GEIST), 0.001)
        assertEquals(0.0, TypeChart.single(Type.ELEKTRO, Type.GESTEIN), 0.001)
        // Doppeltyp multipliziert sich
        assertEquals(4.0, TypeChart.multiplier(Type.EIS, listOf(Type.DRACHE, Type.PFLANZE)), 0.001)
        for (t in Type.values()) {
            assertTrue("$t hat keine Besonderheiten",
                Type.values().any { TypeChart.single(t, it) != 1.0 })
        }
    }

    @Test
    fun typBonusErhoehtSchaden() {
        // Gleicher Angreifer, gleiche Staerke: Attacke des eigenen Typs muss haerter treffen
        val angreifer = Monster("flamki", 50)
        val ziel = Monster("ratzel", 50)
        var mitBonus = 0
        var ohneBonus = 0
        repeat(400) {
            ziel.healFull()
            val b1 = Battle(mutableListOf(angreifer), mutableListOf(ziel), BattleKind.WILD)
            b1.start()
            angreifer.moves = mutableListOf(MoveSlot.of("glut"))       // Feuer = eigener Typ
            angreifer.healFull()
            b1.playerMove(0)
            mitBonus += ziel.maxHp - ziel.currentHp

            ziel.healFull()
            val b2 = Battle(mutableListOf(angreifer), mutableListOf(ziel), BattleKind.WILD)
            b2.start()
            angreifer.moves = mutableListOf(MoveSlot.of("aquaknarre")) // Wasser, gleiche Staerke
            angreifer.healFull()
            b2.playerMove(0)
            ohneBonus += ziel.maxHp - ziel.currentHp
        }
        assertTrue("Typ-Bonus wirkt nicht ($mitBonus vs $ohneBonus)", mitBonus > ohneBonus * 1.2)
    }

    // ------------------------------------------------------------------ Karten
    @Test
    fun kartenSindGueltig() {
        val fehler = mutableListOf<String>()
        val speciesIds = Dex.all().map { it.id }.toSet()
        for (map in MapData.all()) {
            if (map.rows.map { it.length }.distinct().size != 1)
                fehler.add("${map.id}: ungleiche Zeilenlaengen")
            for (w in map.warps) {
                val ziel = MapData.all().firstOrNull { it.id == w.to }
                if (ziel == null) { fehler.add("${map.id}: Warp zu ${w.to} unbekannt"); continue }
                if (map.solidAt(w.x, w.y)) fehler.add("${map.id}: Warp-Feld blockiert")
                if (ziel.solidAt(w.tx, w.ty)) fehler.add("${map.id}->${w.to}: Zielfeld blockiert")
                if (ziel.warpAt(w.tx, w.ty) != null)
                    fehler.add("${map.id}->${w.to}: Ziel ist selbst ein Warp (Endlosschleife)")
            }
            for (n in map.npcs) {
                if (n.x !in 0 until map.width || n.y !in 0 until map.height)
                    fehler.add("${map.id}/${n.id}: NPC ausserhalb der Karte")
                else if (map.solidAt(n.x, n.y) && n.kind != NpcKind.SIGN)
                    fehler.add("${map.id}/${n.id}: NPC auf blockiertem Feld")
                if (n.itemId.isNotEmpty() && !Items.exists(n.itemId))
                    fehler.add("${map.id}/${n.id}: Item ${n.itemId} unbekannt")
                if (n.givesItem.isNotEmpty() && !Items.exists(n.givesItem))
                    fehler.add("${map.id}/${n.id}: Geschenk ${n.givesItem} unbekannt")
                n.trainer?.let { t ->
                    if (t.party.isEmpty()) fehler.add("${map.id}/${n.id}: Trainer ohne Team")
                    for (tm in t.party) {
                        if (tm.speciesId !in speciesIds) fehler.add("${map.id}/${n.id}: Art ${tm.speciesId} unbekannt")
                        for (mv in tm.moves) if (Moves.all().none { it.id == mv })
                            fehler.add("${map.id}/${n.id}: Attacke $mv unbekannt")
                    }
                    if (t.rewardItem.isNotEmpty() && !Items.exists(t.rewardItem))
                        fehler.add("${map.id}/${n.id}: Belohnung unbekannt")
                }
            }
            for (s in map.shop) if (!Items.exists(s)) fehler.add("${map.id}: Shop-Item $s unbekannt")
            for (e in map.encounters + map.fishing) {
                if (e.speciesId !in speciesIds) fehler.add("${map.id}: Begegnung ${e.speciesId} unbekannt")
                if (e.min > e.max) fehler.add("${map.id}: Levelbereich ungueltig")
            }
        }
        assertTrue(fehler.joinToString("\n"), fehler.isEmpty())
    }

    // ------------------------------------------------------------------ Kampf
    @Test
    fun kaempfeLaufenStabilDurch() {
        val ids = Dex.all().map { it.id }
        val baelle = listOf("fangball", "superball", "hyperball")
        var haenger = 0
        repeat(600) { r ->
            val lvl = Random.nextInt(5, 60)
            val team = MutableList(Random.nextInt(1, 7)) {
                Monster(ids.random(), (lvl + Random.nextInt(-4, 5)).coerceIn(2, 100)).apply { healFull() }
            }
            val wild = Random.nextBoolean()
            val gegner = if (wild) mutableListOf(Monster(ids.random(), lvl).apply { healFull() })
            else MutableList(Random.nextInt(1, 5)) { Monster(ids.random(), lvl).apply { healFull() } }
            val art = if (wild) BattleKind.WILD else if (r % 5 == 0) BattleKind.BOSS else BattleKind.TRAINER

            val b = Battle(team, gegner, art, "Testgegner", 100)
            var ereignisse = b.start()
            var runden = 0
            while (!b.over && runden < 400) {
                runden++
                ereignisse.forEach { ev ->
                    when (ev) {
                        is BEvent.Evolve -> team.getOrNull(ev.monsterIndex)?.evolveInto(ev.toId)
                        is BEvent.LearnPrompt -> team.getOrNull(ev.monsterIndex)?.replaceMove(0, ev.moveId)
                        else -> {}
                    }
                }
                if (b.over) break
                if (ereignisse.any { it is BEvent.RequestSwitch }) {
                    val idx = team.indexOfFirst { !it.isFainted }
                    assertTrue("Wechsel verlangt, aber kein Monster kampffaehig", idx >= 0)
                    ereignisse = b.switchAfterFaint(idx)
                    continue
                }
                ereignisse = when (Random.nextInt(10)) {
                    0 -> b.playerRun()
                    1 -> if (wild) b.playerItem(baelle.random(), b.playerIndex)
                    else b.playerMove(Random.nextInt(b.activePlayer.moves.size))
                    2 -> b.playerItem("trank", b.playerIndex)
                    3 -> {
                        val frei = team.indices.filter { !team[it].isFainted && it != b.playerIndex }
                        if (frei.isEmpty()) b.playerMove(Random.nextInt(b.activePlayer.moves.size))
                        else b.playerSwitch(frei.random())
                    }
                    else -> b.playerMove(Random.nextInt(b.activePlayer.moves.size))
                }
                val a = b.activePlayer
                assertTrue("KP ausserhalb des Bereichs", a.currentHp in 0..a.maxHp)
                assertTrue("Ungueltiger Index", b.playerIndex in team.indices && b.foeIndex in gegner.indices)
            }
            if (runden >= 400) haenger++
        }
        assertEquals("Kaempfe ohne Ende", 0, haenger)
    }

    @Test
    fun statusUndErfahrungFunktionieren() {
        val m = Monster("flamki", 5)
        m.healFull()
        val vorher = m.level
        m.gainExp(Monster.expForLevel(20) - m.exp)
        assertTrue("Kein Levelaufstieg", m.level > vorher)
        assertTrue("Entwicklung nicht faellig", m.evolutionTarget() == "flammor")
        m.evolveInto("flammor")
        assertEquals("flammor", m.speciesId)
        assertTrue(m.currentHp in 1..m.maxHp)

        // Feuer-Monster koennen nicht verbrennen
        val ziel = Monster("flamki", 30).apply { healFull() }
        val angreifer = Monster("glutwurm", 30).apply {
            healFull(); moves = mutableListOf(MoveSlot.of("flammenwurf"))
        }
        val b = Battle(mutableListOf(angreifer), mutableListOf(ziel), BattleKind.WILD)
        b.start()
        repeat(20) { if (!b.over) b.playerMove(0) }
        assertTrue("Feuer-Monster wurde verbrannt", ziel.status != StatusKind.BRAND)
    }

    // ------------------------------------------------------------------ Story
    @Test
    fun spielIstDurchspielbar() {
        val flags = HashSet<String>()
        var siegel = 0
        var erreichbar: Set<Triple<String, Int, Int>> = emptySet()

        repeat(30) {
            erreichbar = flutFuellung(flags)
            val neu = HashSet<String>()
            for ((karte, x, y) in erreichbar) {
                val map = MapData.get(karte)
                for (n in map.npcs) {
                    if (!sichtbar(n, flags)) continue
                    if (!benachbart(x, y, n)) continue
                    n.trainer?.let { t -> if (t.badge.isNotEmpty()) neu.add(t.badge) }
                    if (n.givesItem.isNotEmpty() && n.setsFlag.isNotEmpty() && siegel >= n.requiresBadges)
                        neu.add(n.setsFlag)
                    if (n.id.startsWith("ball_")) neu.add("starter")
                }
            }
            if (!flags.addAll(neu)) return@repeat
            siegel = (1..5).count { flags.contains("siegel$it") }
        }

        val ziele = listOf("starter", "siegel1", "lampe_erhalten", "siegel2", "angel_erhalten",
            "siegel3", "siegel4", "siegel5", "morgana_besiegt", "champion_besiegt")
        for (f in ziele) assertTrue("Nicht erreichbar: $f", flags.contains(f))

        val titanox = MapData.get("gipfel").npcs.first { it.id == "titanox" }
        assertTrue("Titanox unerreichbar",
            erreichbar.any { it.first == "gipfel" && benachbart(it.second, it.third, titanox) })

        val besucht = erreichbar.map { it.first }.toSet()
        for (m in MapData.all()) assertTrue("Karte ${m.id} unerreichbar", m.id in besucht)

        for (m in MapData.all()) for (n in m.npcs) if (n.kind == NpcKind.ITEM) {
            assertTrue("Item ${n.itemId} auf ${m.id} unerreichbar",
                erreichbar.any { it.first == m.id && benachbart(it.second, it.third, n) })
        }
    }

    private fun sichtbar(n: NpcDef, flags: Set<String>): Boolean {
        if (n.requiresFlag.isNotEmpty() && !flags.contains(n.requiresFlag)) return false
        if (n.hiddenFlag.isNotEmpty() && flags.contains(n.hiddenFlag)) return false
        return true
    }

    private fun benachbart(x: Int, y: Int, n: NpcDef): Boolean =
        (x == n.x && kotlin.math.abs(y - n.y) == 1) || (y == n.y && kotlin.math.abs(x - n.x) == 1)

    private fun flutFuellung(flags: Set<String>): Set<Triple<String, Int, Int>> {
        val start = Triple("zuhause", 4, 4)
        val gesehen = HashSet<Triple<String, Int, Int>>()
        val schlange = ArrayDeque<Triple<String, Int, Int>>()
        gesehen.add(start); schlange.add(start)
        while (schlange.isNotEmpty()) {
            val (karte, x, y) = schlange.removeFirst()
            val map = MapData.get(karte)
            val warp = map.warpAt(x, y)
            if (warp != null && (warp.requiresFlag.isEmpty() || flags.contains(warp.requiresFlag))) {
                val np = Triple(warp.to, warp.tx, warp.ty)
                if (gesehen.add(np)) schlange.add(np)
                continue
            }
            for ((dx, dy) in listOf(0 to 1, 0 to -1, 1 to 0, -1 to 0)) {
                val nx = x + dx
                val ny = y + dy
                if (nx < 0 || ny < 0 || nx >= map.width || ny >= map.height) continue
                if (map.solidAt(nx, ny)) continue
                if (map.npcs.any { it.blocks && it.x == nx && it.y == ny && sichtbar(it, flags) }) continue
                val np = Triple(karte, nx, ny)
                if (gesehen.add(np)) schlange.add(np)
            }
        }
        return gesehen
    }
}
