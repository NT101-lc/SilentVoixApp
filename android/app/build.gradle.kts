plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Backend the app talks to. Override with -Psilentvoix.backendBaseUrl=... or in
// ~/.gradle/gradle.properties. The default reaches the host machine from the Android emulator.
val backendBaseUrl = providers.gradleProperty("silentvoix.backendBaseUrl")
    .getOrElse("http://10.0.2.2:8081")

android {
    namespace = "com.silentvoix.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.silentvoix.app"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"

        buildConfigField("String", "BACKEND_BASE_URL", "\"$backendBaseUrl\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.adaptive.navigation.suite)
    implementation(libs.androidx.compose.material.icons.core)
}
