import org.gradle.api.tasks.Copy

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.devtools.ksp)
    alias(libs.plugins.jetbrains.kotlin.plugin.serialization)
}

android {
    namespace = "me.xdan.prism"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "me.xdan.prism"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/LICENSE.md"
            excludes += "/META-INF/NOTICE.md"
        }
    }

    // AGP 9 rejects Provider instances passed directly to the legacy
    // AndroidSourceSet API. Register the generated directory as a plain path,
    // while the task dependency below ensures it is populated before assets
    // are merged.
    sourceSets["main"].assets.directories.add(
        layout.buildDirectory.dir("generated/prismTemplateAssets").get().asFile.absolutePath
    )
}

val prepareWebTemplate by tasks.registering(Copy::class) {
    dependsOn(":webtemplate:assembleRelease")
    from(project(":webtemplate").layout.buildDirectory.dir("outputs/apk/release")) {
        include("*.apk")
    }
    into(layout.buildDirectory.dir("generated/prismTemplateAssets/generated"))
    rename { "base-release.apk" }
}

tasks.matching { it.name.matches(Regex("merge[A-Z].*Assets")) }.configureEach {
    dependsOn(prepareWebTemplate)
}

tasks.named("preBuild") {
    dependsOn(prepareWebTemplate)
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.accompanist.permissions)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.coil.compose)
    implementation(libs.converter.moshi)
    implementation(libs.moshi.kotlin)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.logging.interceptor)
    implementation(libs.material)
    implementation(libs.zip4j)
    implementation(libs.bouncycastle)
    implementation(libs.bcpkix)
    implementation(libs.apksig)
    implementation(libs.androidsvg)
}
