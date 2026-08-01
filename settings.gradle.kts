// Runeveil — Saga of the Nine
// Root Gradle settings: repository configuration and module graph.
pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Runeveil"

// ---------------------------------------------------------------------------
// Module graph (Clean Architecture)
//
//   :app     -> Android application. Compose UI, navigation, DI wiring, audio.
//   :data    -> Android library. Room, DataStore, asset content pipeline,
//               repository implementations. Depends on :domain.
//   :domain  -> Pure Kotlin/JVM library. Entities, game rules, battle engine,
//               use cases, repository *interfaces*. Depends on nothing Android.
//
// Dependency direction is strictly  :app -> :data -> :domain  and
// :app -> :domain. :domain never points outwards.
// ---------------------------------------------------------------------------
include(":app")
include(":data")
include(":domain")
