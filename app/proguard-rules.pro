# Runeveil release rules.

# Content DTOs are constructed by kotlinx.serialization via generated
# serializers; keep them and their serializers intact.
-keepclassmembers class com.runeveil.saga.data.content.** {
    *** Companion;
}
-keepclasseswithmembers class com.runeveil.saga.data.content.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.runeveil.saga.data.database.**Json { *; }

# Sealed hierarchies are matched by @SerialName, not by class name.
-keepattributes *Annotation*, InnerClasses, Signature, RuntimeVisible*Annotations

# Room entities are reflected over by the generated DAOs.
-keep class com.runeveil.saga.data.database.*Entity { *; }
-keep class com.runeveil.saga.data.database.EmbeddedStats { *; }

# Enum names are persisted in the save database and in the content JSON;
# renaming them would corrupt saves.
-keepclassmembers enum com.runeveil.saga.domain.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
