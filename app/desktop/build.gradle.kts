import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(projects.app.shared)
    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)
}

compose.desktop {
    application {
        mainClass = "com.hrm.breeze.MainKt"
        // Compose Desktop 的 checkRuntime/jpackage 需要一个带 jpackage 的 JDK。
        // Android Studio 默认的 JBR 不包含 jpackage，IDE 通过 .gradle/config.properties
        // 把 Gradle JVM 锁到了 JBR，因此这里显式根据 JAVA_HOME 指向带 jpackage 的 JDK。
        val packagingJdk: String? = providers.environmentVariable("JAVA_HOME").orNull
        if (!packagingJdk.isNullOrBlank() && file("$packagingJdk/bin/jpackage").exists()) {
            javaHome = packagingJdk
        }
        buildTypes.release.proguard {
            // 第三方 latex / markdown / codehighlight 库 release jar 自身存在引用不一致，
            // 让 ProGuard 跳过它们，避免 packageReleaseDmg 在 proguardReleaseJars 阶段失败。
            configurationFiles.from(project.file("compose-desktop.pro"))
        }
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Breeze"
            packageVersion = "1.0.0"
            modules(
                "java.net.http",
                "jdk.crypto.ec",
                "java.instrument",
                "jdk.unsupported",
            )
            macOS {
                bundleID = "com.hrm.breeze"
            }
            windows {
                menuGroup = "Breeze"
                // upgradeUuid = "固定 UUID，后续升级用"
            }
            linux {
                packageName = "breeze"
                // maintainer = "your-email@example.com"
            }
        }
    }
}

tasks.matching { task -> task.name.contains("Msi", ignoreCase = true) }.configureEach {
    dependsOn(":runtime:llama:verifyDesktopLlamaBundledRuntime")
    doFirst {
        check(System.getProperty("os.name").lowercase().contains("win")) {
            "Windows MSI packaging must run on a Windows host. " +
                    "The bundled Desktop llama runtime is host-specific and cannot be packaged into an MSI from ${
                        System.getProperty(
                            "os.name"
                        )
                    }."
        }
    }
}
