package com.pokelike.game

import org.json.JSONObject

enum class ItemCategory(val deName: String) {
    BALL("Baelle"),
    HEILUNG("Heilung"),
    KAMPF("Kampf"),
    SONSTIGES("Sonstiges"),
    BASIS("Basis-Items")
}

data class Item(
    val id: String,
    val name: String,
    val category: ItemCategory,
    val price: Int,
    val desc: String,
    val ballRate: Double = 0.0,
    val healHp: Int = 0,                 // -1 = volle KP
    val healStatus: List<StatusKind> = emptyList(),
    val healAllStatus: Boolean = false,
    val revivePct: Int = 0,              // 50 = halbe KP, 100 = volle KP
    val restorePp: Boolean = false,
    val battleStat: Stat? = null,
    val battleStages: Int = 0,
    val vitaminStat: Stat? = null,
    val levelUp: Boolean = false,
    val stone: String? = null,
    val repelSteps: Int = 0,
    val escapeRope: Boolean = false,
    val keyItem: Boolean = false
) {
    val usableInBattle: Boolean
        get() = ballRate > 0.0 || healHp != 0 || healStatus.isNotEmpty() || healAllStatus ||
                revivePct > 0 || restorePp || battleStat != null
    val usableInField: Boolean
        get() = healHp != 0 || healStatus.isNotEmpty() || healAllStatus || revivePct > 0 ||
                restorePp || vitaminStat != null || levelUp || stone != null ||
                repelSteps > 0 || escapeRope
    val needsTarget: Boolean
        get() = healHp != 0 || healStatus.isNotEmpty() || healAllStatus || revivePct > 0 ||
                restorePp || vitaminStat != null || levelUp || stone != null
}

object Items {

    private val list = listOf(
        // ------------------------------- Baelle -------------------------------
        Item("fangball", "Fangball", ItemCategory.BALL, 200,
            "Ein einfaches Geraet zum Fangen wilder Monster.", ballRate = 1.0),
        Item("superball", "Superball", ItemCategory.BALL, 600,
            "Etwas besser als ein Fangball.", ballRate = 1.5),
        Item("hyperball", "Hyperball", ItemCategory.BALL, 1200,
            "Ein sehr guter Ball mit hoher Fangrate.", ballRate = 2.0),
        Item("meisterball", "Meisterball", ItemCategory.BALL, 0,
            "Faengt jedes wilde Monster ohne Fehlschlag.", ballRate = 255.0),

        // ------------------------------- Heilung -------------------------------
        Item("trank", "Trank", ItemCategory.HEILUNG, 200,
            "Fuellt 20 KP eines Monsters auf.", healHp = 20),
        Item("supertrank", "Supertrank", ItemCategory.HEILUNG, 600,
            "Fuellt 50 KP eines Monsters auf.", healHp = 50),
        Item("hypertrank", "Hypertrank", ItemCategory.HEILUNG, 1200,
            "Fuellt 120 KP eines Monsters auf.", healHp = 120),
        Item("toptrank", "Top-Trank", ItemCategory.HEILUNG, 2500,
            "Fuellt alle KP eines Monsters auf.", healHp = -1),
        Item("beleber", "Beleber", ItemCategory.HEILUNG, 1500,
            "Belebt ein Monster mit der Haelfte seiner KP wieder.", revivePct = 50),
        Item("topbeleber", "Top-Beleber", ItemCategory.HEILUNG, 4000,
            "Belebt ein Monster mit vollen KP wieder.", revivePct = 100),
        Item("gegengift", "Gegengift", ItemCategory.HEILUNG, 150,
            "Heilt eine Vergiftung.", healStatus = listOf(StatusKind.GIFT)),
        Item("brandsalbe", "Brandsalbe", ItemCategory.HEILUNG, 150,
            "Heilt Verbrennungen.", healStatus = listOf(StatusKind.BRAND)),
        Item("eisspray", "Eisspray", ItemCategory.HEILUNG, 150,
            "Taut ein eingefrorenes Monster auf.", healStatus = listOf(StatusKind.FROST)),
        Item("aufwecker", "Aufwecker", ItemCategory.HEILUNG, 150,
            "Weckt ein schlafendes Monster.", healStatus = listOf(StatusKind.SCHLAF)),
        Item("paraheiler", "Para-Heiler", ItemCategory.HEILUNG, 150,
            "Heilt eine Paralyse.", healStatus = listOf(StatusKind.PARALYSE)),
        Item("allheiler", "Allheiler", ItemCategory.HEILUNG, 400,
            "Heilt jeden Statusproblem-Zustand.", healAllStatus = true),
        Item("elixier", "Elixier", ItemCategory.HEILUNG, 900,
            "Fuellt die AP aller Attacken eines Monsters auf.", restorePp = true),

        // ------------------------------- Kampf-Items -------------------------------
        Item("xangriff", "X-Angriff", ItemCategory.KAMPF, 500,
            "Erhoeht im Kampf den Angriff.", battleStat = Stat.ATK, battleStages = 1),
        Item("xabwehr", "X-Abwehr", ItemCategory.KAMPF, 550,
            "Erhoeht im Kampf die Verteidigung.", battleStat = Stat.DEF, battleStages = 1),
        Item("xspezial", "X-Spezial", ItemCategory.KAMPF, 500,
            "Erhoeht im Kampf den Spezial-Angriff.", battleStat = Stat.SPA, battleStages = 1),
        Item("xtempo", "X-Tempo", ItemCategory.KAMPF, 350,
            "Erhoeht im Kampf die Initiative.", battleStat = Stat.SPE, battleStages = 1),

        // ------------------------------- Sonstiges -------------------------------
        Item("sonderbonbon", "Sonderbonbon", ItemCategory.SONSTIGES, 4800,
            "Erhoeht das Level eines Monsters um eins.", levelUp = true),
        Item("kpplus", "KP-Plus", ItemCategory.SONSTIGES, 9800,
            "Erhoeht die Basis-KP eines Monsters dauerhaft.", vitaminStat = Stat.HP),
        Item("kraftplus", "Kraft-Plus", ItemCategory.SONSTIGES, 9800,
            "Erhoeht den Angriff eines Monsters dauerhaft.", vitaminStat = Stat.ATK),
        Item("panzerplus", "Panzer-Plus", ItemCategory.SONSTIGES, 9800,
            "Erhoeht die Verteidigung eines Monsters dauerhaft.", vitaminStat = Stat.DEF),
        Item("geistplus", "Geist-Plus", ItemCategory.SONSTIGES, 9800,
            "Erhoeht den Spezial-Angriff dauerhaft.", vitaminStat = Stat.SPA),
        Item("nervenplus", "Nerven-Plus", ItemCategory.SONSTIGES, 9800,
            "Erhoeht die Spezial-Verteidigung dauerhaft.", vitaminStat = Stat.SPD),
        Item("tempoplus", "Tempo-Plus", ItemCategory.SONSTIGES, 9800,
            "Erhoeht die Initiative dauerhaft.", vitaminStat = Stat.SPE),
        Item("feuerstein", "Feuerstein", ItemCategory.SONSTIGES, 2100,
            "Ein gluehender Stein. Manche Monster entwickeln sich damit.", stone = "feuerstein"),
        Item("wasserstein", "Wasserstein", ItemCategory.SONSTIGES, 2100,
            "Ein blauer Stein. Manche Monster entwickeln sich damit.", stone = "wasserstein"),
        Item("blattstein", "Blattstein", ItemCategory.SONSTIGES, 2100,
            "Ein gruener Stein. Manche Monster entwickeln sich damit.", stone = "blattstein"),
        Item("donnerstein", "Donnerstein", ItemCategory.SONSTIGES, 2100,
            "Ein knisternder Stein. Manche Monster entwickeln sich damit.", stone = "donnerstein"),
        Item("schutz", "Schutz", ItemCategory.SONSTIGES, 350,
            "Haelt 100 Schritte lang schwache wilde Monster fern.", repelSteps = 100),
        Item("superschutz", "Superschutz", ItemCategory.SONSTIGES, 500,
            "Haelt 200 Schritte lang wilde Monster fern.", repelSteps = 200),
        Item("fluchtseil", "Fluchtseil", ItemCategory.SONSTIGES, 550,
            "Bringt dich sofort zur letzten Heilstation.", escapeRope = true),

        // ------------------------------- Basis-Items -------------------------------
        Item("lampe", "Grubenlampe", ItemCategory.BASIS, 0,
            "Erhellt dunkle Hoehlen.", keyItem = true),
        Item("angel", "Angel", ItemCategory.BASIS, 0,
            "Damit kann man am Wasser Monster angeln.", keyItem = true),
        Item("gipfelpass", "Gipfelpass", ItemCategory.BASIS, 0,
            "Erlaubt den Aufstieg zum Drachengipfel.", keyItem = true),
        Item("monsterradar", "Monsterradar", ItemCategory.BASIS, 0,
            "Zeigt an, wie viele Arten in der Gegend leben.", keyItem = true)
    )

    private val byId = list.associateBy { it.id }

    fun get(id: String): Item = byId[id] ?: list[0]
    fun exists(id: String) = byId.containsKey(id)
    fun all(): List<Item> = list
}

/** Der Beutel des Spielers. */
class Bag {
    private val counts = LinkedHashMap<String, Int>()

    fun add(id: String, n: Int = 1) {
        if (!Items.exists(id)) return
        counts[id] = (counts[id] ?: 0) + n
    }

    fun remove(id: String, n: Int = 1): Boolean {
        val have = counts[id] ?: 0
        if (have < n) return false
        if (have - n <= 0) counts.remove(id) else counts[id] = have - n
        return true
    }

    fun count(id: String): Int = counts[id] ?: 0
    fun has(id: String): Boolean = count(id) > 0
    fun isEmpty(): Boolean = counts.isEmpty()

    fun byCategory(cat: ItemCategory): List<Pair<Item, Int>> =
        counts.entries.map { Items.get(it.key) to it.value }
            .filter { it.first.category == cat }
            .sortedBy { it.first.name }

    fun toJson(): JSONObject {
        val o = JSONObject()
        counts.forEach { (k, v) -> o.put(k, v) }
        return o
    }

    fun loadJson(o: JSONObject) {
        counts.clear()
        val it = o.keys()
        while (it.hasNext()) {
            val k = it.next()
            if (Items.exists(k)) counts[k] = o.getInt(k)
        }
    }
}
