import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.pokelike.idle"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.pokelike.idle"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()

        // versionCode wird spaeter vom CI hochgezaehlt; 1 ist der Startwert.
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            // Eigene applicationId, damit Debug- und Release-Build parallel auf
            // demselben Geraet installiert sein koennen.
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            isMinifyEnabled = false

            // Steuert, ob der echte Google-Play-Billing-Client oder die lokale
            // Fake-Implementierung gebunden wird. Siehe Billing-Schritt.
            buildConfigField("boolean", "USE_FAKE_BILLING", "true")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            buildConfigField("boolean", "USE_FAKE_BILLING", "false")

            // Solange kein Upload-Keystore hinterlegt ist, wird der Release-Build
            // mit dem Debug-Key signiert, damit `assembleRelease` lokal
            // durchlaeuft. Vor der Play-Veroeffentlichung ersetzen.
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        val javaVersion = JavaVersion.toVersion(libs.versions.javaTarget.get())
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion

        // Erlaubt die Nutzung neuerer java.time-APIs bis hinunter zu minSdk 24.
        isCoreLibraryDesugaringEnabled = true
    }

    buildFeatures {
        compose = true
        // Wird fuer USE_FAKE_BILLING und BuildConfig.DEBUG gebraucht.
        buildConfig = true
    }

    packaging {
        resources {
            // Doppelte Lizenz-/Metadatendateien aus transitiven Abhaengigkeiten
            // wuerden den Merge sonst abbrechen lassen.
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
        }
    }

    testOptions {
        unitTests {
            // Erlaubt es, Android-Framework-Klassen in JVM-Tests zu benutzen,
            // ohne dass jeder Aufruf eine Exception wirft.
            isReturnDefaultValues = true
        }
    }
}

/**
 * Kotlin-Compileroptionen.
 *
 * Gehoert auf Modulebene und nicht in den `android`-Block: Die Einstellung
 * stammt vom Kotlin-Plugin, nicht von AGP. Das frueher uebliche
 * `android.kotlinOptions` ist seit AGP 8 veraltet.
 */
kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(libs.versions.javaTarget.get()))

        // Warnungen als Fehler behandeln haelt die Codebasis dauerhaft sauber.
        // Bewusst nur fuer eigene Warnungen; veraltete Aufrufe aus Bibliotheken
        // wuerden den Build sonst bei jedem Upgrade blockieren.
        allWarningsAsErrors.set(false)
    }
}

/**
 * KSP-Argumente.
 *
 * Room legt sein Datenbankschema als JSON ab. Die Dateien werden eingecheckt
 * und dienen ab dem Persistenz-Schritt als Grundlage fuer Migrationstests:
 * Ohne sie laesst sich nicht automatisiert pruefen, ob ein Update alte
 * Spielstaende noch lesen kann.
 */
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
}

dependencies {

    // --- AndroidX Basis ---------------------------------------------------
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.splashscreen)

    // --- Lifecycle --------------------------------------------------------
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    // Prozessweiter Lifecycle: Grundlage fuer Autosave beim App-Wechsel in den
    // Hintergrund und fuer die Offline-Progress-Berechnung beim Zurueckkehren.
    implementation(libs.androidx.lifecycle.process)

    // --- Compose ----------------------------------------------------------
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // --- Dependency Injection --------------------------------------------
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    // --- Persistenz -------------------------------------------------------
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.datastore.preferences)

    // --- Nebenlaeufigkeit / Serialisierung --------------------------------
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    // --- Desugaring -------------------------------------------------------
    coreLibraryDesugaring(libs.desugarJdkLibs)

    // --- Unit Tests -------------------------------------------------------
    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)

    // --- Instrumented Tests -----------------------------------------------
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.room.testing)
}
