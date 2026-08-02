# Consumers of :data must keep the Room and serialization metadata.
-keep class com.runeveil.saga.data.database.*Entity { *; }
-keepclasseswithmembers class com.runeveil.saga.data.content.** {
    kotlinx.serialization.KSerializer serializer(...);
}
