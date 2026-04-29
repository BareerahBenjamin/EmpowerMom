pluginManagement {
    repositories {
        // 华为云镜像（已验证可访问）
        maven { url = uri("https://mirrors.huaweicloud.com/repository/maven/") }
        maven { url = uri("https://mirrors.huaweicloud.com/repository/gradle-plugin/") }
        // 兜底
        gradlePluginPortal()
        google()
        mavenCentral()
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "com.google.devtools.ksp") {
                useModule("com.google.devtools.ksp:symbol-processing-gradle-plugin:${requested.version}")
            }
        }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // 华为云镜像（已验证可访问）
        maven { url = uri("https://mirrors.huaweicloud.com/repository/maven/") }
        // 兜底
        google()
        mavenCentral()
    }
}

rootProject.name = "EmpowerMom"
include(":app")