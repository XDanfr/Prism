plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "me.xdan.prism.template"
    compileSdk = 36

    defaultConfig {
        applicationId = "me.xdan.prism.template"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
