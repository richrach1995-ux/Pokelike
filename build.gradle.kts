// Root build script. Plugins are declared (but not applied) here so that every
// module can apply them without repeating the version.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}

/**
 * Convenience task that runs the full quality gate used by CI:
 * unit tests of every module plus a debug assembly.
 */
tasks.register("qualityGate") {
    group = "verification"
    description = "Runs all unit tests and assembles the debug APK."
    dependsOn(
        ":domain:test",
        ":data:testDebugUnitTest",
        ":app:testDebugUnitTest",
        ":app:assembleDebug",
    )
}
