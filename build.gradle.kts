// Root-Buildscript.
//
// Hier werden ausschliesslich Plugins deklariert (ohne sie anzuwenden),
// damit die Submodule sie ueber `alias(libs.plugins.*)` einbinden koennen,
// ohne die Version erneut zu nennen.

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}
