pluginManagement {
    repositories {
        google()
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

rootProject.name = "alittleappthathelpsme"
include(":app")
include(":core")
include(":data")
include(":feature_notes")
include(":feature_tasks")
include(":feature_utils")
include(":feature_transfer")
include(":feature_nas")
include(":feature_vault")
include(":feature_timer")
include(":feature_clipboard")
include(":feature_webui")
