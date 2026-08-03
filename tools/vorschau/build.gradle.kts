// Eigenstaendiger Build: startet das Spiel ohne Android-Geraet, damit man
// Bildschirmfotos machen und die Darstellung pruefen kann.
// Aufruf:  cd tools/vorschau && gradle test --rerun-tasks
plugins { kotlin("jvm") version "2.0.21" }
repositories { mavenCentral() }

dependencies {
    implementation("org.json:json:20240303")
    implementation(files("shim.jar"))
    testImplementation("junit:junit:4.13.2")
}

kotlin { jvmToolchain(21) }

sourceSets {
    main {
        java.setSrcDirs(listOf<String>())
        kotlin.setSrcDirs(listOf("../../app/src/main/java"))
        kotlin.exclude("**/MainActivity.kt")
    }
    test { kotlin.setSrcDirs(listOf("src")) }
}

// Die Android-Nachbauten werden vorab zu shim.jar uebersetzt.
val shimBauen by tasks.registering(Exec::class) {
    commandLine("bash", "-c",
        "rm -rf build/shim && mkdir -p build/shim && " +
        "javac -nowarn -d build/shim \$(find shim/java -name '*.java') && " +
        "jar cf shim.jar -C build/shim .")
}
tasks.named("compileKotlin") { dependsOn(shimBauen) }

tasks.withType<Test> {
    systemProperty("java.awt.headless", "true")
    systemProperty("pokelike.bilder", layout.buildDirectory.dir("bilder").get().asFile.absolutePath)
    testLogging { events("passed", "failed"); showStandardStreams = true }
}
