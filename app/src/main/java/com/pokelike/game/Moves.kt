package com.pokelike.game

enum class Stat(val deName: String, val shortName: String) {
    HP("KP", "KP"),
    ATK("Angriff", "ANG"),
    DEF("Verteidigung", "VER"),
    SPA("Spezial-Angriff", "SP-ANG"),
    SPD("Spezial-Verteidigung", "SP-VER"),
    SPE("Initiative", "INI"),
    ACC("Genauigkeit", "GEN")
}

enum class StatusKind(val deName: String, val short: String) {
    NONE("", ""),
    GIFT("vergiftet", "GFT"),
    BRAND("verbrannt", "BRD"),
    PARALYSE("paralysiert", "PAR"),
    SCHLAF("eingeschlafen", "SLF"),
    FROST("eingefroren", "FRO")
}

enum class MoveCategory { PHYSISCH, SPEZIAL, STATUS }

enum class Target { SELF, FOE }

data class StatChange(val target: Target, val stat: Stat, val stages: Int)

data class Move(
    val id: String,
    val name: String,
    val type: Type,
    val cat: MoveCategory,
    val power: Int,
    val acc: Int,           // 0 = trifft immer
    val pp: Int,
    val priority: Int = 0,
    val status: StatusKind? = null,
    val statusChance: Int = 0,
    val statChanges: List<StatChange> = emptyList(),
    val statChance: Int = 100,
    val drainPct: Int = 0,      // Prozent des angerichteten Schadens als Heilung
    val recoilPct: Int = 0,     // Prozent des angerichteten Schadens als Rueckstoss
    val healPct: Int = 0,       // Prozent der eigenen max. KP
    val flinchChance: Int = 0,
    val minHits: Int = 1,
    val maxHits: Int = 1,
    val highCrit: Boolean = false,
    val confuseChance: Int = 0,
    val desc: String = ""
)

object Moves {

    private val list = listOf(
        // ---------------- NORMAL ----------------
        Move("tackle", "Tackle", Type.NORMAL, MoveCategory.PHYSISCH, 40, 100, 35,
            desc = "Ein einfacher Rammangriff."),
        Move("kratzer", "Kratzer", Type.NORMAL, MoveCategory.PHYSISCH, 40, 100, 35,
            desc = "Harte Krallen kratzen den Gegner."),
        Move("ruckzuck", "Ruckzuckhieb", Type.NORMAL, MoveCategory.PHYSISCH, 40, 100, 30, priority = 1,
            desc = "Greift immer zuerst an."),
        Move("doppelschlag", "Doppelschlag", Type.NORMAL, MoveCategory.PHYSISCH, 25, 100, 20,
            minHits = 2, maxHits = 2, desc = "Trifft zweimal in Folge."),
        Move("furienschlag", "Furienschlag", Type.NORMAL, MoveCategory.PHYSISCH, 18, 90, 20,
            minHits = 2, maxHits = 5, desc = "Trifft zwei- bis fuenfmal."),
        Move("schlitzer", "Schlitzer", Type.NORMAL, MoveCategory.PHYSISCH, 70, 100, 20,
            highCrit = true, desc = "Hohe Volltrefferquote."),
        Move("bodycheck", "Bodycheck", Type.NORMAL, MoveCategory.PHYSISCH, 85, 100, 15,
            desc = "Ein wuchtiger Koerperangriff."),
        Move("hyperstrahl", "Hyperstrahl", Type.NORMAL, MoveCategory.SPEZIAL, 120, 90, 5,
            desc = "Ein extrem starker Energiestrahl."),
        Move("heuler", "Heuler", Type.NORMAL, MoveCategory.STATUS, 0, 100, 40,
            statChanges = listOf(StatChange(Target.FOE, Stat.ATK, -1)),
            desc = "Senkt den Angriff des Gegners."),
        Move("haertner", "Haertner", Type.NORMAL, MoveCategory.STATUS, 0, 0, 30,
            statChanges = listOf(StatChange(Target.SELF, Stat.DEF, 1)),
            desc = "Erhoeht die eigene Verteidigung."),
        Move("agilitaet", "Agilitaet", Type.NORMAL, MoveCategory.STATUS, 0, 0, 30,
            statChanges = listOf(StatChange(Target.SELF, Stat.SPE, 2)),
            desc = "Erhoeht die Initiative stark."),
        Move("schwaechler", "Schwaechler", Type.NORMAL, MoveCategory.STATUS, 0, 100, 20,
            statChanges = listOf(StatChange(Target.FOE, Stat.DEF, -1)),
            desc = "Senkt die Verteidigung des Gegners."),
        Move("sandwirbel", "Sandwirbel", Type.NORMAL, MoveCategory.STATUS, 0, 100, 15,
            statChanges = listOf(StatChange(Target.FOE, Stat.ACC, -1)),
            desc = "Senkt die Genauigkeit des Gegners."),
        Move("erholung", "Erholung", Type.NORMAL, MoveCategory.STATUS, 0, 0, 10,
            healPct = 50, desc = "Stellt die Haelfte der eigenen KP her."),
        Move("verzweifler", "Verzweifler", Type.NORMAL, MoveCategory.PHYSISCH, 50, 100, 1,
            recoilPct = 25,
            desc = "Letzter Ausweg, wenn keine AP mehr uebrig sind. Verletzt auch einen selbst."),

        // ---------------- FEUER ----------------
        Move("glut", "Glut", Type.FEUER, MoveCategory.SPEZIAL, 40, 100, 25,
            status = StatusKind.BRAND, statusChance = 10, desc = "Kleine Flamme, kann verbrennen."),
        Move("feuerzahn", "Feuerzahn", Type.FEUER, MoveCategory.PHYSISCH, 65, 95, 15,
            status = StatusKind.BRAND, statusChance = 10, flinchChance = 10,
            desc = "Gluehende Zaehne beissen zu."),
        Move("flammenwurf", "Flammenwurf", Type.FEUER, MoveCategory.SPEZIAL, 90, 100, 15,
            status = StatusKind.BRAND, statusChance = 10, desc = "Eine breite Feuerwelle."),
        Move("flammenrad", "Flammenrad", Type.FEUER, MoveCategory.PHYSISCH, 75, 100, 15,
            status = StatusKind.BRAND, statusChance = 10, desc = "Rollt brennend in den Gegner."),
        Move("feuersturm", "Feuersturm", Type.FEUER, MoveCategory.SPEZIAL, 115, 85, 5,
            status = StatusKind.BRAND, statusChance = 20, desc = "Ein gewaltiger Feuerorkan."),
        Move("hitzeschutz", "Hitzeschild", Type.FEUER, MoveCategory.STATUS, 0, 0, 20,
            statChanges = listOf(StatChange(Target.SELF, Stat.SPD, 2)),
            desc = "Erhoeht die Spezial-Verteidigung stark."),

        // ---------------- WASSER ----------------
        Move("aquaknarre", "Aquaknarre", Type.WASSER, MoveCategory.SPEZIAL, 40, 100, 25,
            desc = "Ein harter Wasserstrahl."),
        Move("blubbstrahl", "Blubbstrahl", Type.WASSER, MoveCategory.SPEZIAL, 65, 100, 20,
            statChanges = listOf(StatChange(Target.FOE, Stat.SPE, -1)), statChance = 20,
            desc = "Blasen, die die Initiative senken koennen."),
        Move("aquahaubitze", "Aquahaubitze", Type.WASSER, MoveCategory.PHYSISCH, 85, 100, 15,
            desc = "Ein wuchtiger Wasserschlag."),
        Move("surfer", "Surfer", Type.WASSER, MoveCategory.SPEZIAL, 90, 100, 15,
            desc = "Eine grosse Welle rollt heran."),
        Move("hydropumpe", "Hydropumpe", Type.WASSER, MoveCategory.SPEZIAL, 115, 80, 5,
            desc = "Ein extrem starker Wasserstoss."),
        Move("nassschweif", "Nassschweif", Type.WASSER, MoveCategory.PHYSISCH, 60, 100, 20,
            highCrit = true, desc = "Peitscht mit nassem Schweif zu."),

        // ---------------- PFLANZE ----------------
        Move("rankenhieb", "Rankenhieb", Type.PFLANZE, MoveCategory.PHYSISCH, 45, 100, 25,
            desc = "Schlaegt mit Ranken zu."),
        Move("gigasauger", "Gigasauger", Type.PFLANZE, MoveCategory.SPEZIAL, 75, 100, 10,
            drainPct = 50, desc = "Saugt die Haelfte des Schadens als KP ab."),
        Move("kugelsaat", "Kugelsaat", Type.PFLANZE, MoveCategory.PHYSISCH, 80, 100, 15,
            desc = "Feuert harte Samen ab."),
        Move("blattgeissel", "Blattgeissel", Type.PFLANZE, MoveCategory.PHYSISCH, 90, 95, 10,
            highCrit = true, desc = "Scharfe Blaetter mit hoher Volltrefferquote."),
        Move("solarstrahl", "Solarstrahl", Type.PFLANZE, MoveCategory.SPEZIAL, 120, 90, 5,
            desc = "Gebuendeltes Sonnenlicht."),
        Move("schlafpuder", "Schlafpuder", Type.PFLANZE, MoveCategory.STATUS, 0, 75, 15,
            status = StatusKind.SCHLAF, statusChance = 100, desc = "Versetzt den Gegner in Schlaf."),
        Move("stachelspore", "Stachelspore", Type.PFLANZE, MoveCategory.STATUS, 0, 90, 20,
            status = StatusKind.PARALYSE, statusChance = 100, desc = "Paralysiert den Gegner."),
        Move("giftpuder", "Giftpuder", Type.PFLANZE, MoveCategory.STATUS, 0, 85, 20,
            status = StatusKind.GIFT, statusChance = 100, desc = "Vergiftet den Gegner."),
        Move("wachstum", "Wachstum", Type.PFLANZE, MoveCategory.STATUS, 0, 0, 20,
            statChanges = listOf(StatChange(Target.SELF, Stat.SPA, 1), StatChange(Target.SELF, Stat.ATK, 1)),
            desc = "Erhoeht Angriff und Spezial-Angriff."),

        // ---------------- ELEKTRO ----------------
        Move("donnerschock", "Donnerschock", Type.ELEKTRO, MoveCategory.SPEZIAL, 40, 100, 30,
            status = StatusKind.PARALYSE, statusChance = 10, desc = "Ein schwacher Stromstoss."),
        Move("funkenflug", "Funkenflug", Type.ELEKTRO, MoveCategory.PHYSISCH, 65, 100, 20,
            status = StatusKind.PARALYSE, statusChance = 30, desc = "Ein knisternder Rammangriff."),
        Move("donnerblitz", "Donnerblitz", Type.ELEKTRO, MoveCategory.SPEZIAL, 90, 100, 15,
            status = StatusKind.PARALYSE, statusChance = 10, desc = "Ein starker Blitzschlag."),
        Move("donner", "Donner", Type.ELEKTRO, MoveCategory.SPEZIAL, 115, 75, 5,
            status = StatusKind.PARALYSE, statusChance = 30, desc = "Ein gewaltiger Donnerschlag."),
        Move("donnerwelle", "Donnerwelle", Type.ELEKTRO, MoveCategory.STATUS, 0, 90, 20,
            status = StatusKind.PARALYSE, statusChance = 100, desc = "Paralysiert den Gegner sicher."),
        Move("ladungsstoss", "Ladungsstoss", Type.ELEKTRO, MoveCategory.PHYSISCH, 85, 95, 10,
            recoilPct = 25, desc = "Starker Stoss mit Rueckstoss."),

        // ---------------- EIS ----------------
        Move("frostatem", "Frostatem", Type.EIS, MoveCategory.SPEZIAL, 45, 100, 25,
            status = StatusKind.FROST, statusChance = 10, desc = "Eisiger Atem."),
        Move("eiszahn", "Eiszahn", Type.EIS, MoveCategory.PHYSISCH, 65, 95, 15,
            status = StatusKind.FROST, statusChance = 10, flinchChance = 10,
            desc = "Eiskalte Zaehne beissen zu."),
        Move("eishieb", "Eishieb", Type.EIS, MoveCategory.PHYSISCH, 80, 100, 15,
            status = StatusKind.FROST, statusChance = 10, desc = "Ein eiskalter Hieb."),
        Move("eisstrahl", "Eisstrahl", Type.EIS, MoveCategory.SPEZIAL, 90, 100, 10,
            status = StatusKind.FROST, statusChance = 10, desc = "Ein gebuendelter Eisstrahl."),
        Move("blizzard", "Blizzard", Type.EIS, MoveCategory.SPEZIAL, 115, 75, 5,
            status = StatusKind.FROST, statusChance = 20, desc = "Ein toedlicher Schneesturm."),
        Move("frostschleier", "Frostschleier", Type.EIS, MoveCategory.STATUS, 0, 100, 15,
            statChanges = listOf(StatChange(Target.FOE, Stat.SPE, -2)),
            desc = "Senkt die Initiative des Gegners stark."),

        // ---------------- KAMPF ----------------
        Move("karateschlag", "Karateschlag", Type.KAMPF, MoveCategory.PHYSISCH, 50, 100, 25,
            highCrit = true, desc = "Hohe Volltrefferquote."),
        Move("steigerungshieb", "Steigerungshieb", Type.KAMPF, MoveCategory.PHYSISCH, 40, 100, 25,
            statChanges = listOf(StatChange(Target.SELF, Stat.ATK, 1)),
            desc = "Erhoeht nach dem Treffer den Angriff."),
        Move("kraftkoloss", "Kraftkoloss", Type.KAMPF, MoveCategory.PHYSISCH, 80, 95, 15,
            desc = "Ein wuchtiger Ringkampf-Angriff."),
        Move("kreuzhieb", "Kreuzhieb", Type.KAMPF, MoveCategory.PHYSISCH, 95, 90, 10,
            highCrit = true, desc = "Kreuzende Hiebe, hohe Volltrefferquote."),
        Move("nahkampf", "Nahkampf", Type.KAMPF, MoveCategory.PHYSISCH, 120, 100, 5,
            statChanges = listOf(StatChange(Target.SELF, Stat.DEF, -1)),
            desc = "Sehr stark, senkt aber die eigene Verteidigung."),

        // ---------------- GIFT ----------------
        Move("saeure", "Saeure", Type.GIFT, MoveCategory.SPEZIAL, 40, 100, 30,
            statChanges = listOf(StatChange(Target.FOE, Stat.SPD, -1)), statChance = 20,
            desc = "Aetzende Saeure."),
        Move("giftstachel", "Giftstachel", Type.GIFT, MoveCategory.PHYSISCH, 50, 100, 25,
            status = StatusKind.GIFT, statusChance = 30, desc = "Ein giftiger Stich."),
        Move("matschbombe", "Matschbombe", Type.GIFT, MoveCategory.SPEZIAL, 70, 100, 15,
            status = StatusKind.GIFT, statusChance = 30, desc = "Wirft giftigen Schlamm."),
        Move("schlammbombe", "Schlammbombe", Type.GIFT, MoveCategory.SPEZIAL, 95, 90, 10,
            status = StatusKind.GIFT, statusChance = 30, desc = "Eine grosse Giftbombe."),
        Move("toxin", "Toxin", Type.GIFT, MoveCategory.STATUS, 0, 90, 10,
            status = StatusKind.GIFT, statusChance = 100, desc = "Vergiftet den Gegner sicher."),

        // ---------------- GESTEIN ----------------
        Move("steinwurf", "Steinwurf", Type.GESTEIN, MoveCategory.PHYSISCH, 50, 90, 15,
            desc = "Wirft einen Felsbrocken."),
        Move("steinhagel", "Steinhagel", Type.GESTEIN, MoveCategory.PHYSISCH, 75, 90, 10,
            flinchChance = 20, desc = "Ein Hagel aus Steinen."),
        Move("felswurf", "Felswurf", Type.GESTEIN, MoveCategory.PHYSISCH, 90, 85, 10,
            desc = "Schleudert einen grossen Felsen."),
        Move("steinkante", "Steinkante", Type.GESTEIN, MoveCategory.PHYSISCH, 100, 80, 5,
            highCrit = true, desc = "Scharfe Steine, hohe Volltrefferquote."),
        Move("erdbeben", "Erdbeben", Type.GESTEIN, MoveCategory.PHYSISCH, 100, 100, 10,
            desc = "Ein schweres Beben erschuettert alles."),
        Move("panzerschutz", "Panzerschutz", Type.GESTEIN, MoveCategory.STATUS, 0, 0, 15,
            statChanges = listOf(StatChange(Target.SELF, Stat.DEF, 2)),
            desc = "Erhoeht die Verteidigung stark."),

        // ---------------- KAEFER ----------------
        Move("fadenschuss", "Fadenschuss", Type.KAEFER, MoveCategory.STATUS, 0, 95, 40,
            statChanges = listOf(StatChange(Target.FOE, Stat.SPE, -2)),
            desc = "Fesselt den Gegner mit Faeden."),
        Move("kaeferbiss", "Kaeferbiss", Type.KAEFER, MoveCategory.PHYSISCH, 60, 100, 20,
            desc = "Ein harter Biss."),
        Move("silberhauch", "Silberhauch", Type.KAEFER, MoveCategory.SPEZIAL, 60, 100, 20,
            statChanges = listOf(StatChange(Target.SELF, Stat.SPA, 1)), statChance = 25,
            desc = "Glitzernder Wind, kann den Spezial-Angriff erhoehen."),
        Move("sichelschlag", "Sichelschlag", Type.KAEFER, MoveCategory.PHYSISCH, 85, 100, 15,
            highCrit = true, desc = "Scharfe Sicheln schlagen zu."),
        Move("kaefergebrumm", "Kaefergebrumm", Type.KAEFER, MoveCategory.SPEZIAL, 95, 100, 10,
            statChanges = listOf(StatChange(Target.FOE, Stat.SPD, -1)), statChance = 30,
            desc = "Ein zerrender Schallangriff."),

        // ---------------- GEIST ----------------
        Move("schattenstoss", "Schattenstoss", Type.GEIST, MoveCategory.PHYSISCH, 40, 100, 30, priority = 1,
            desc = "Ein Angriff aus dem Schatten, greift zuerst an."),
        Move("nachtnebel", "Nachtnebel", Type.GEIST, MoveCategory.SPEZIAL, 60, 100, 20,
            statChanges = listOf(StatChange(Target.FOE, Stat.ACC, -1)), statChance = 30,
            desc = "Dunkler Nebel truebt die Sicht."),
        Move("schattenklaue", "Schattenklaue", Type.GEIST, MoveCategory.PHYSISCH, 75, 100, 15,
            highCrit = true, desc = "Klauen aus Schatten, hohe Volltrefferquote."),
        Move("spukball", "Spukball", Type.GEIST, MoveCategory.SPEZIAL, 95, 100, 10,
            statChanges = listOf(StatChange(Target.FOE, Stat.SPD, -1)), statChance = 20,
            desc = "Ein unheimlicher Energieball."),
        Move("konfustrahl", "Konfustrahl", Type.GEIST, MoveCategory.STATUS, 0, 90, 10,
            confuseChance = 100, desc = "Verwirrt den Gegner."),
        Move("fluch", "Fluch", Type.GEIST, MoveCategory.STATUS, 0, 0, 10,
            statChanges = listOf(StatChange(Target.SELF, Stat.ATK, 1), StatChange(Target.SELF, Stat.DEF, 1)),
            desc = "Belegt sich mit dunkler Kraft."),

        // ---------------- DRACHE ----------------
        Move("drachenwut", "Drachenwut", Type.DRACHE, MoveCategory.SPEZIAL, 50, 100, 20,
            desc = "Eine Welle aus Drachenzorn."),
        Move("drachenklaue", "Drachenklaue", Type.DRACHE, MoveCategory.PHYSISCH, 80, 100, 15,
            desc = "Scharfe Drachenklauen."),
        Move("drachenpuls", "Drachenpuls", Type.DRACHE, MoveCategory.SPEZIAL, 90, 100, 10,
            desc = "Eine Schockwelle aus Drachenenergie."),
        Move("drachentanz", "Drachentanz", Type.DRACHE, MoveCategory.STATUS, 0, 0, 15,
            statChanges = listOf(StatChange(Target.SELF, Stat.ATK, 1), StatChange(Target.SELF, Stat.SPE, 1)),
            desc = "Erhoeht Angriff und Initiative."),
        Move("wutanfall", "Wutanfall", Type.DRACHE, MoveCategory.PHYSISCH, 120, 90, 5,
            recoilPct = 25, desc = "Rasender Angriff mit Rueckstoss.")
    )

    private val byId: Map<String, Move> = list.associateBy { it.id }

    fun get(id: String): Move = byId[id] ?: byId.getValue("tackle")
    fun all(): List<Move> = list
}
