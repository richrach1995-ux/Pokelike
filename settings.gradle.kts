// Zentrale Projekt- und Repository-Konfiguration.
// Repositories werden ausschliesslich hier deklariert (PREFER_SETTINGS),
// damit einzelne Module keine abweichenden Quellen einschleusen koennen.

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

rootProject.name = "Pokelike"

include(":app")
