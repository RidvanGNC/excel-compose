import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.mavenPublish)
}

group = "io.github.ridvangnc"
version = "0.4.1"

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()

    coordinates(group.toString(), "excel-compose", version.toString())

    pom {
        name.set("excel-compose")
        description.set("An Excel-style dense data grid for Compose Multiplatform (Android + Desktop).")
        url.set("https://github.com/RidvanGNC/excel-compose")
        licenses {
            license {
                name.set("MIT")
                url.set("https://github.com/RidvanGNC/excel-compose/blob/main/LICENSE")
            }
        }
        developers {
            developer {
                id.set("RidvanGNC")
                name.set("Ridvan GNC")
                url.set("https://github.com/RidvanGNC")
            }
        }
        scm {
            url.set("https://github.com/RidvanGNC/excel-compose")
            connection.set("scm:git:https://github.com/RidvanGNC/excel-compose.git")
            developerConnection.set("scm:git:ssh://git@github.com/RidvanGNC/excel-compose.git")
        }
    }
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    jvm("desktop") {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
        }
    }
}

android {
    namespace = "excelcompose.grid"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
