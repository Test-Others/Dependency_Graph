import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.cyclonedx.model.Component

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.cyclonedxBom)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }
    
    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.activity.compose)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "com.example.dependency_graph"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.example.dependency_graph"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(libs.compose.uiTooling)
}

// CycloneDX SBOM configuration
tasks.cyclonedxDirectBom {
    // Project type and metadata
    projectType.set(Component.Type.APPLICATION)
    componentName.set("dependency-graph-kmp")
    componentGroup.set("com.example.dependency_graph")
    
    // Include all platform configurations
    includeConfigs.set(listOf(
        "commonMainImplementation",
        "androidReleaseRuntimeClasspath",
        "androidReleaseCompileClasspath",
        "iosArm64ApiElements",
        "iosSimulatorArm64ApiElements"
    ))
    
    // Exclude test configurations
    skipConfigs.set(listOf(
        ".*[Tt]est.*",
        ".*[Dd]ebug.*"
    ))
    
    // Schema and metadata options
    includeBomSerialNumber.set(true)
    includeLicenseText.set(false)
    includeMetadataResolution.set(true)
    
    // Output format: both JSON and XML
    jsonOutput.set(file("build/reports/cyclonedx-direct/bom.json"))
    xmlOutput.set(file("build/reports/cyclonedx-direct/bom.xml"))
}

tasks.cyclonedxBom {
    // Aggregated SBOM configuration
    projectType.set(Component.Type.APPLICATION)
    componentName.set("dependency-graph-kmp-aggregate")
    componentGroup.set("com.example.dependency_graph")
    
    includeBomSerialNumber.set(true)
    includeLicenseText.set(false)
    
    jsonOutput.set(file("${rootProject.layout.buildDirectory.get()}/reports/cyclonedx/bom.json"))
    xmlOutput.set(file("${rootProject.layout.buildDirectory.get()}/reports/cyclonedx/bom.xml"))
}

