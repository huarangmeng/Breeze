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

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.hrm.breeze"
            packageVersion = "1.0.0"
        }
    }
}

tasks.matching { task -> task.name.contains("Msi", ignoreCase = true) }.configureEach {
    dependsOn(":runtime:llama:verifyDesktopLlamaBundledRuntime")
    doFirst {
        check(System.getProperty("os.name").lowercase().contains("win")) {
            "Windows MSI packaging must run on a Windows host. " +
                "The bundled Desktop llama runtime is host-specific and cannot be packaged into an MSI from ${System.getProperty("os.name")}."
        }
    }
}
