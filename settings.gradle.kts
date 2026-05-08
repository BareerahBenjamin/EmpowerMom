pluginManagement {
    repositories {
        // 华为云镜像（国内稳定）
        maven { url = uri("https://mirrors.huaweicloud.com/repository/maven/") }
        maven { url = uri("https://mirrors.huaweicloud.com/repository/gradle-plugin/") }
        // 阿里云镜像（兜底）
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        // 官方源（最后兜底）
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
        // 华为云镜像
        maven { url = uri("https://mirrors.huaweicloud.com/repository/maven/") }
        // 阿里云镜像（兜底）
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        // 官方源
        google()
        mavenCentral()
    }
}

rootProject.name = "EmpowerMom"
include(":app")