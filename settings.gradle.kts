try {
    val pe = Class.forName("java.lang.ProcessEnvironment")
    for (fieldName in listOf("theEnvironment", "theCaseInsensitiveEnvironment")) {
        val f = pe.getDeclaredField(fieldName)
        f.isAccessible = true
        (f.get(null) as? MutableMap<String, String>)?.remove("ANDROID_PREFS_ROOT")
    }
} catch (_: Throwable) {
}

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":app")
include(":common")
include(":masterswitch")
include(":rcheevos-api")