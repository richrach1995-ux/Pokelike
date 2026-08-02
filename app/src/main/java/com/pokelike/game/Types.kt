package com.pokelike.game

/**
 * Die zwoelf Elementtypen des Spiels inklusive Effektivitaets-Tabelle.
 */
enum class Type(val deName: String, val color: Int) {
    NORMAL("Normal", 0xFFA8A878.toInt()),
    FEUER("Feuer", 0xFFF08030.toInt()),
    WASSER("Wasser", 0xFF6890F0.toInt()),
    PFLANZE("Pflanze", 0xFF78C850.toInt()),
    ELEKTRO("Elektro", 0xFFE8C020.toInt()),
    EIS("Eis", 0xFF88D0D8.toInt()),
    KAMPF("Kampf", 0xFFC03028.toInt()),
    GIFT("Gift", 0xFFA040A0.toInt()),
    GESTEIN("Gestein", 0xFFB8A038.toInt()),
    KAEFER("Kaefer", 0xFF98B028.toInt()),
    GEIST("Geist", 0xFF705898.toInt()),
    DRACHE("Drache", 0xFF7038F8.toInt());
}

object TypeChart {

    // attacker -> (defender -> multiplier). Alles was nicht eingetragen ist, ist 1.0.
    private val chart: Map<Type, Map<Type, Double>> = mapOf(
        Type.NORMAL to mapOf(Type.GESTEIN to 0.5, Type.GEIST to 0.0),
        Type.FEUER to mapOf(
            Type.PFLANZE to 2.0, Type.EIS to 2.0, Type.KAEFER to 2.0,
            Type.FEUER to 0.5, Type.WASSER to 0.5, Type.GESTEIN to 0.5, Type.DRACHE to 0.5
        ),
        Type.WASSER to mapOf(
            Type.FEUER to 2.0, Type.GESTEIN to 2.0,
            Type.WASSER to 0.5, Type.PFLANZE to 0.5, Type.DRACHE to 0.5
        ),
        Type.PFLANZE to mapOf(
            Type.WASSER to 2.0, Type.GESTEIN to 2.0,
            Type.FEUER to 0.5, Type.PFLANZE to 0.5, Type.GIFT to 0.5,
            Type.KAEFER to 0.5, Type.DRACHE to 0.5
        ),
        Type.ELEKTRO to mapOf(
            Type.WASSER to 2.0,
            Type.PFLANZE to 0.5, Type.ELEKTRO to 0.5, Type.DRACHE to 0.5,
            Type.GESTEIN to 0.0
        ),
        Type.EIS to mapOf(
            Type.PFLANZE to 2.0, Type.DRACHE to 2.0, Type.GESTEIN to 2.0,
            Type.FEUER to 0.5, Type.WASSER to 0.5, Type.EIS to 0.5
        ),
        Type.KAMPF to mapOf(
            Type.NORMAL to 2.0, Type.GESTEIN to 2.0, Type.EIS to 2.0,
            Type.GIFT to 0.5, Type.KAEFER to 0.5,
            Type.GEIST to 0.0
        ),
        Type.GIFT to mapOf(
            Type.PFLANZE to 2.0,
            Type.GIFT to 0.5, Type.GESTEIN to 0.5, Type.GEIST to 0.5
        ),
        Type.GESTEIN to mapOf(
            Type.FEUER to 2.0, Type.EIS to 2.0, Type.KAEFER to 2.0,
            Type.KAMPF to 0.5, Type.GIFT to 0.5
        ),
        Type.KAEFER to mapOf(
            Type.PFLANZE to 2.0, Type.GEIST to 2.0,
            Type.FEUER to 0.5, Type.KAMPF to 0.5, Type.GIFT to 0.5
        ),
        Type.GEIST to mapOf(
            Type.GEIST to 2.0, Type.GIFT to 0.5,
            Type.NORMAL to 0.0
        ),
        Type.DRACHE to mapOf(Type.DRACHE to 2.0)
    )

    fun single(attacker: Type, defender: Type): Double =
        chart[attacker]?.get(defender) ?: 1.0

    /** Gesamteffektivitaet gegen ein Monster mit einem oder zwei Typen. */
    fun multiplier(attacker: Type, defenders: List<Type>): Double {
        var m = 1.0
        for (d in defenders) m *= single(attacker, d)
        return m
    }

    fun effectivenessText(m: Double): String? = when {
        m == 0.0 -> "Es hat keine Wirkung ..."
        m >= 2.0 -> "Das ist sehr effektiv!"
        m > 0.0 && m < 1.0 -> "Das ist nicht sehr effektiv ..."
        else -> null
    }
}
