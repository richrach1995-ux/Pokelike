package com.pokelike.game

import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.max
import kotlin.random.Random

class MoveSlot(val moveId: String, var pp: Int, val maxPp: Int) {
    val move: Move get() = Moves.get(moveId)
    fun toJson(): JSONObject = JSONObject().put("id", moveId).put("pp", pp).put("max", maxPp)

    companion object {
        fun of(moveId: String): MoveSlot {
            val m = Moves.get(moveId)
            return MoveSlot(moveId, m.pp, m.pp)
        }

        fun fromJson(o: JSONObject) = MoveSlot(o.getString("id"), o.getInt("pp"), o.getInt("max"))
    }
}

class Monster(
    var speciesId: String,
    var level: Int,
    var nickname: String? = null
) {
    val species: Species get() = Dex.get(speciesId)

    var exp: Int = expForLevel(level)
    var ivs: IntArray = IntArray(6) { Random.nextInt(0, 16) }
    var bonus: IntArray = IntArray(6)   // dauerhafte Boni durch Vitamine
    var moves: MutableList<MoveSlot> = mutableListOf()
    var status: StatusKind = StatusKind.NONE
    var sleepTurns: Int = 0
    var currentHp: Int = 1
    var originalTrainer: String = ""

    val name: String get() = nickname ?: species.name

    init {
        moves = defaultMovesFor(level)
        currentHp = maxHp
    }

    // ---------------------------------------------------------------- Werte
    val maxHp: Int
        get() = (2 * species.hp + ivs[0]) * level / 100 + level + 10 + bonus[0]

    fun stat(s: Stat): Int = when (s) {
        Stat.HP -> maxHp
        Stat.ATK -> calc(species.atk, ivs[1], bonus[1])
        Stat.DEF -> calc(species.def, ivs[2], bonus[2])
        Stat.SPA -> calc(species.spa, ivs[3], bonus[3])
        Stat.SPD -> calc(species.spd, ivs[4], bonus[4])
        Stat.SPE -> calc(species.spe, ivs[5], bonus[5])
        Stat.ACC -> 100
    }

    private fun calc(base: Int, iv: Int, bon: Int) = (2 * base + iv) * level / 100 + 5 + bon

    val isFainted: Boolean get() = currentHp <= 0
    val hpRatio: Float get() = if (maxHp <= 0) 0f else currentHp.toFloat() / maxHp

    // ---------------------------------------------------------------- Attacken
    private fun defaultMovesFor(lv: Int): MutableList<MoveSlot> {
        val learned = species.learnset.filter { it.first <= lv }.map { it.second }.distinct()
        val take = learned.takeLast(4)
        val res = take.map { MoveSlot.of(it) }.toMutableList()
        if (res.isEmpty()) res.add(MoveSlot.of("tackle"))
        return res
    }

    fun knowsMove(id: String) = moves.any { it.moveId == id }

    fun learnMove(id: String): Boolean {
        if (knowsMove(id)) return false
        if (moves.size >= 4) return false
        moves.add(MoveSlot.of(id))
        return true
    }

    fun replaceMove(index: Int, id: String) {
        if (index in moves.indices) moves[index] = MoveSlot.of(id)
    }

    /** Attacken, die genau auf diesem Level gelernt werden. */
    fun movesLearnedAt(lv: Int): List<String> =
        species.learnset.filter { it.first == lv }.map { it.second }.filter { !knowsMove(it) }

    // ---------------------------------------------------------------- Erfahrung
    fun expToNext(): Int = max(0, expForLevel(level + 1) - exp)
    fun expInLevel(): Int = exp - expForLevel(level)
    fun expLevelSpan(): Int = max(1, expForLevel(level + 1) - expForLevel(level))

    /** Gibt die erreichten Level zurueck. */
    fun gainExp(amount: Int): List<Int> {
        val levels = mutableListOf<Int>()
        if (level >= MAX_LEVEL) return levels
        exp += amount
        while (level < MAX_LEVEL && exp >= expForLevel(level + 1)) {
            level++
            currentHp += (2 * species.hp + ivs[0]) / 100 + 1   // kleiner KP-Zuwachs
            currentHp = currentHp.coerceAtMost(maxHp)
            levels.add(level)
        }
        if (level >= MAX_LEVEL) exp = expForLevel(MAX_LEVEL)
        return levels
    }

    // ---------------------------------------------------------------- Pflege
    fun healFull() {
        currentHp = maxHp
        status = StatusKind.NONE
        sleepTurns = 0
        moves.forEach { it.pp = it.maxPp }
    }

    fun heal(amount: Int): Int {
        val before = currentHp
        currentHp = (currentHp + amount).coerceIn(0, maxHp)
        return currentHp - before
    }

    fun damage(amount: Int): Int {
        val before = currentHp
        currentHp = (currentHp - amount).coerceAtLeast(0)
        return before - currentHp
    }

    /** Prueft Entwicklung. Gibt die Ziel-Art zurueck oder null. */
    fun evolutionTarget(stone: String? = null): String? {
        val e = species.evolution ?: return null
        return when {
            stone != null && e.stone == stone -> e.toId
            stone == null && e.stone == null && e.level > 0 && level >= e.level -> e.toId
            else -> null
        }
    }

    fun evolveInto(newId: String) {
        val hpDiff = maxHp - currentHp
        speciesId = newId
        currentHp = (maxHp - hpDiff).coerceIn(1, maxHp)
        // Neue Sofort-Attacken der Entwicklung lernen
        for ((lv, mv) in species.learnset) {
            if (lv <= level && !knowsMove(mv) && moves.size < 4) moves.add(MoveSlot.of(mv))
        }
    }

    // ---------------------------------------------------------------- Speichern
    fun toJson(): JSONObject {
        val o = JSONObject()
        o.put("sp", speciesId)
        o.put("lv", level)
        o.put("exp", exp)
        nickname?.let { o.put("nick", it) }
        o.put("hp", currentHp)
        o.put("st", status.name)
        o.put("sl", sleepTurns)
        o.put("ot", originalTrainer)
        o.put("ivs", JSONArray().apply { ivs.forEach { put(it) } })
        o.put("bon", JSONArray().apply { bonus.forEach { put(it) } })
        o.put("mv", JSONArray().apply { moves.forEach { put(it.toJson()) } })
        return o
    }

    companion object {
        const val MAX_LEVEL = 100

        fun expForLevel(lv: Int): Int {
            val l = lv.coerceIn(1, MAX_LEVEL)
            return l * l * l
        }

        fun wild(speciesId: String, level: Int): Monster = Monster(speciesId, level)

        fun fromJson(o: JSONObject): Monster {
            val nick = if (o.has("nick")) o.getString("nick") else null
            val m = Monster(o.getString("sp"), o.getInt("lv"), nick)
            m.exp = o.optInt("exp", expForLevel(m.level))
            m.status = runCatching { StatusKind.valueOf(o.optString("st", "NONE")) }.getOrDefault(StatusKind.NONE)
            m.sleepTurns = o.optInt("sl", 0)
            m.originalTrainer = o.optString("ot", "")
            o.optJSONArray("ivs")?.let { a -> for (i in 0 until minOf(6, a.length())) m.ivs[i] = a.getInt(i) }
            o.optJSONArray("bon")?.let { a -> for (i in 0 until minOf(6, a.length())) m.bonus[i] = a.getInt(i) }
            o.optJSONArray("mv")?.let { a ->
                val list = mutableListOf<MoveSlot>()
                for (i in 0 until a.length()) list.add(MoveSlot.fromJson(a.getJSONObject(i)))
                if (list.isNotEmpty()) m.moves = list
            }
            m.currentHp = o.optInt("hp", m.maxHp).coerceIn(0, m.maxHp)
            return m
        }
    }
}
