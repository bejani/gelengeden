pluginManagement {
    repositories {
        // Prefer mirrors: Google Maven is often blocked (e.g. IR)
        maven(url = "https://maven.aliyun.com/repository/google")
        maven(url = "https://maven.aliyun.com/repository/public")
        maven(url = "https://maven.aliyun.com/repository/gradle-plugin")
        maven(url = "https://repo.maven.apache.org/maven2/")
        maven(url = "https://dl.google.com/dl/android/maven2/")
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven(url = "https://maven.aliyun.com/repository/google")
        maven(url = "https://maven.aliyun.com/repository/public")
        maven(url = "https://repo.maven.apache.org/maven2/")
        maven(url = "https://dl.google.com/dl/android/maven2/")
        google()
        mavenCentral()
    }
}

rootProject.name = "gelengeden"
include(":app")
