package com.pokelike.game

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Der komplette Spielstand. */
class GameState {

    var playerName: String = "Held"
    var mapId: String = "zuhause"
    var px: Int = 4
    var py: Int = 4
    var dir: Int = 0                    // 0 unten, 1 oben, 2 links, 3 rechts

    val party = mutableListOf<Monster>()
    val box = mutableListOf<Monster>()
    val bag = Bag()
    var money: Int = 3000

    val flags = HashSet<String>()
    val seen = HashSet<String>()
    val caught = HashSet<String>()

    var healMap: String = "heim"
    var healX: Int = 6
    var healY: Int = 10
    var repelSteps: Int = 0
    var playTimeMs: Long = 0L
    var steps: Int = 0

    val badges: Int
        get() = (1..5).count { flags.contains("siegel$it") }

    fun hasFlag(f: String) = f.isEmpty() || flags.contains(f)
    fun setFlag(f: String) { if (f.isNotEmpty()) flags.add(f) }

    fun firstAlive(): Int = party.indexOfFirst { !it.isFainted }
    fun anyAlive(): Boolean = party.any { !it.isFainted }
    fun healParty() = party.forEach { it.healFull() }

    fun addMonster(m: Monster): Boolean {
        caught.add(m.speciesId)
        seen.add(m.speciesId)
        m.originalTrainer = playerName
        return if (party.size < 6) { party.add(m); true } else { box.add(m); false }
    }

    fun startNewGame() {
        playerName = "Held"
        mapId = "zuhause"; px = 4; py = 4; dir = 0
        party.clear(); box.clear()
        bag.loadJson(JSONObject())
        bag.add("fangball", 5)
        bag.add("trank", 3)
        money = 3000
        flags.clear(); seen.clear(); caught.clear()
        healMap = "heim"; healX = 6; healY = 10
        repelSteps = 0; playTimeMs = 0; steps = 0
    }

    // ------------------------------------------------------------------
    fun toJson(): JSONObject {
        val o = JSONObject()
        o.put("v", 1)
        o.put("name", playerName)
        o.put("map", mapId); o.put("px", px); o.put("py", py); o.put("dir", dir)
        o.put("money", money)
        o.put("party", JSONArray().apply { party.forEach { put(it.toJson()) } })
        o.put("box", JSONArray().apply { box.forEach { put(it.toJson()) } })
        o.put("bag", bag.toJson())
        o.put("flags", JSONArray().apply { flags.forEach { put(it) } })
        o.put("seen", JSONArray().apply { seen.forEach { put(it) } })
        o.put("caught", JSONArray().apply { caught.forEach { put(it) } })
        o.put("healMap", healMap); o.put("healX", healX); o.put("healY", healY)
        o.put("time", playTimeMs); o.put("steps", steps)
        return o
    }

    fun loadJson(o: JSONObject) {
        playerName = o.optString("name", "Held")
        mapId = o.optString("map", "heim")
        px = o.optInt("px", 6); py = o.optInt("py", 10); dir = o.optInt("dir", 0)
        money = o.optInt("money", 3000)
        party.clear(); box.clear()
        o.optJSONArray("party")?.let { a -> for (i in 0 until a.length()) party.add(Monster.fromJson(a.getJSONObject(i))) }
        o.optJSONArray("box")?.let { a -> for (i in 0 until a.length()) box.add(Monster.fromJson(a.getJSONObject(i))) }
        o.optJSONObject("bag")?.let { bag.loadJson(it) }
        flags.clear(); seen.clear(); caught.clear()
        o.optJSONArray("flags")?.let { a -> for (i in 0 until a.length()) flags.add(a.getString(i)) }
        o.optJSONArray("seen")?.let { a -> for (i in 0 until a.length()) seen.add(a.getString(i)) }
        o.optJSONArray("caught")?.let { a -> for (i in 0 until a.length()) caught.add(a.getString(i)) }
        healMap = o.optString("healMap", "heim")
        healX = o.optInt("healX", 6); healY = o.optInt("healY", 10)
        playTimeMs = o.optLong("time", 0L); steps = o.optInt("steps", 0)
        repelSteps = 0
    }

    companion object {
        private const val FILE = "spielstand.json"

        fun saveFile(ctx: Context) = File(ctx.filesDir, FILE)

        fun hasSave(ctx: Context): Boolean = saveFile(ctx).exists()

        fun save(ctx: Context, state: GameState): Boolean = try {
            saveFile(ctx).writeText(state.toJson().toString())
            true
        } catch (e: Exception) {
            false
        }

        fun load(ctx: Context): GameState? = try {
            val f = saveFile(ctx)
            if (!f.exists()) null else {
                val s = GameState()
                s.loadJson(JSONObject(f.readText()))
                s
            }
        } catch (e: Exception) {
            null
        }
    }
}
