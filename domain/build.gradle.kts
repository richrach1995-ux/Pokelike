// :domain — pure Kotlin/JVM library.
//
// Contains the entire rule set of the game: entities, value objects, the
// deterministic battle engine, progression/breeding/capture mathematics,
// repository interfaces and use cases. It has **no** Android dependency which
// makes every rule unit-testable on the JVM in milliseconds.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        freeCompilerArgs.addAll("-Xjvm-default=all")
    }
}

dependencies {
    implementation(libs.coroutines.core)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.coroutines.test)
}

tasks.withType<Test>().configureEach {
    useJUnit()
    testLogging {
        events("passed", "skipped", "failed")
    }
}
