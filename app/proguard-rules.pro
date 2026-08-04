# ---------------------------------------------------------------------------
# R8 / ProGuard Regeln
# ---------------------------------------------------------------------------

# --- Zeilennummern fuer lesbare Crash-Reports beibehalten ------------------
# Ohne diese beiden Regeln sind Crashlytics-Stacktraces im Release-Build
# praktisch nicht auswertbar.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# --- Annotationen und Generics -------------------------------------------
# Werden von Room, Hilt und kotlinx.serialization zur Laufzeit ausgewertet.
-keepattributes *Annotation*, InnerClasses, Signature, Exceptions

# --- kotlinx.serialization ------------------------------------------------
# Die generierten Serializer werden nur ueber Reflection gefunden.
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers class **$$serializer {
    *** INSTANCE;
}

# --- Enum-Werte -----------------------------------------------------------
# Savegames referenzieren Enums ueber ihren Namen. Wird der Name verkuerzt,
# laesst sich ein alter Spielstand nach einem Update nicht mehr lesen.
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    **[] $VALUES;
    public *;
}

# --- Coroutines -----------------------------------------------------------
-dontwarn kotlinx.coroutines.**
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
