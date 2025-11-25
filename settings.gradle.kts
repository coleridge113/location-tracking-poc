import java.util.Properties
import org.gradle.authentication.http.BasicAuthentication

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
        maven {
            url = uri("https://api.mapbox.com/downloads/v2/releases/maven")
        }
    }
}

val mapboxDownloadsToken: String? = run {
    val propsFile = file("local.properties")
    if (propsFile.exists()) {
        Properties().apply { load(propsFile.inputStream()) }
            .getProperty("MAPBOX_DOWNLOADS_TOKEN")
    } else {
        // fallback to gradle.properties / env if you want
        settings.extensions.extraProperties
            .properties["MAPBOX_DOWNLOADS_TOKEN"] as String?
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://api.mapbox.com/downloads/v2/releases/maven")
            credentials {
                username = "mapbox"
                password = mapboxDownloadsToken ?: ""
            }
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }
}

rootProject.name = "Location Trackign POC"
include(":app")
