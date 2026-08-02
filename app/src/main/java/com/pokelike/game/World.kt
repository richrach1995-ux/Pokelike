package com.pokelike.game

/**
 * Kachel-Eigenschaften. Die Karten werden als Zeichenraster geschrieben.
 *

 *  .  Weg          ,  kurzes Gras   g  hohes Gras (Begegnungen)
 *  #  Baum         ~  Wasser        ^  Felswand
 *  _  Sand         F  Blume         G  Zaun
 *  B  Hauswand     R  Dach          D  Tuer      W  Fenster
 *  S  Schild       H  Heilmaschine  T  Theke
 *  C  Hoehlenboden X  Felsblock     I  Eis       L  Lava
 *  o  Innenboden   w  Innenwand     b  Regal     =  Teppich
 *  A  Arenaboden   M  Statue        P  Steinweg  V  Vulkangestein
 */
object Tiles {

    private const val SOLIDS = "#~^BRWGSHTXLMwbV"

    fun solid(ch: Char): Boolean = SOLIDS.indexOf(ch) >= 0
    fun isTallGrass(ch: Char): Boolean = ch == 'g'
    fun isIce(ch: Char): Boolean = ch == 'I'
}

data class Warp(val x: Int, val y: Int, val to: String, val tx: Int, val ty: Int, val requiresFlag: String = "")

data class Enc(val speciesId: String, val min: Int, val max: Int, val weight: Int = 10)

data class TrainerMon(val speciesId: String, val level: Int, val moves: List<String> = emptyList())

data class TrainerDef(
    val id: String,
    val name: String,
    val party: List<TrainerMon>,
    val money: Int,
    val winLine: String = "Du bist wirklich stark!",
    val boss: Boolean = false,
    val badge: String = "",
    val rewardItem: String = "",
    val rewardCount: Int = 1
) {
    fun buildParty(): MutableList<Monster> {
        val list = mutableListOf<Monster>()
        for (t in party) {
            val m = Monster(t.speciesId, t.level)
            if (t.moves.isNotEmpty()) {
                m.moves = t.moves.take(4).map { MoveSlot.of(it) }.toMutableList()
            }
            m.healFull()
            list.add(m)
        }
        return list
    }
}

enum class NpcKind { TALK, SIGN, TRAINER, HEALER, ITEM }

class NpcDef(
    val id: String,
    val x: Int,
    val y: Int,
    val dir: Int = 0,
    val style: String = "npc_m",
    val kind: NpcKind = NpcKind.TALK,
    val lines: List<String> = emptyList(),
    val afterLines: List<String> = emptyList(),
    val trainer: TrainerDef? = null,
    val sight: Int = 0,
    val itemId: String = "",
    val itemCount: Int = 1,
    val requiresFlag: String = "",
    val hiddenFlag: String = "",
    val setsFlag: String = "",
    val givesItem: String = "",
    val givesItemCount: Int = 1,
    val blocks: Boolean = true,
    val afterFlag: String = "",
    val requiresBadges: Int = 0
)

class GameMap(
    val id: String,
    val name: String,
    rowsRaw: List<String>,
    val warps: List<Warp> = emptyList(),
    val npcs: List<NpcDef> = emptyList(),
    val encounters: List<Enc> = emptyList(),
    val encounterRate: Int = 12,
    val fishing: List<Enc> = emptyList(),
    val dark: Boolean = false,
    val indoor: Boolean = false,
    val theme: String = "wiese",
    val shop: List<String> = emptyList()
) {
    val rows: List<String>
    val width: Int
    val height: Int

    init {
        val w = rowsRaw.maxOfOrNull { it.length } ?: 1
        rows = rowsRaw.map { it.padEnd(w, if (indoor) 'w' else '#') }
        width = w
        height = rows.size
    }

    fun tile(x: Int, y: Int): Char {
        if (y < 0 || y >= height || x < 0 || x >= width) return if (indoor) 'w' else '#'
        return rows[y][x]
    }

    fun solidAt(x: Int, y: Int): Boolean = Tiles.solid(tile(x, y))

    fun warpAt(x: Int, y: Int): Warp? = warps.firstOrNull { it.x == x && it.y == y }

    fun randomEncounter(): Monster? {
        if (encounters.isEmpty()) return null
        val total = encounters.sumOf { it.weight }
        var roll = (0 until total).random()
        for (e in encounters) {
            roll -= e.weight
            if (roll < 0) {
                val lvl = if (e.max > e.min) (e.min..e.max).random() else e.min
                return Monster.wild(e.speciesId, lvl)
            }
        }
        return null
    }

    fun randomFish(): Monster? {
        if (fishing.isEmpty()) return null
        val total = fishing.sumOf { it.weight }
        var roll = (0 until total).random()
        for (e in fishing) {
            roll -= e.weight
            if (roll < 0) {
                val lvl = if (e.max > e.min) (e.min..e.max).random() else e.min
                return Monster.wild(e.speciesId, lvl)
            }
        }
        return null
    }
}
