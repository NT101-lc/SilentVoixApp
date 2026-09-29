import java.net.URI
import java.security.MessageDigest

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Backend the app talks to. Override with -Psilentvoix.backendBaseUrl=... or in
// ~/.gradle/gradle.properties. The default reaches the host machine from the Android emulator.
val backendBaseUrl = providers.gradleProperty("silentvoix.backendBaseUrl")
    .getOrElse("http://10.0.2.2:8081")

// MediaPipe's pretrained gesture recognizer (hand landmarks + 7 canned gestures). Downloaded at
// build time into generated assets rather than committed; pinned to model version 1 and verified.
abstract class DownloadGestureModel : DefaultTask() {
    @get:Input abstract val url: Property<String>
    @get:Input abstract val sha256: Property<String>
    @get:OutputDirectory abstract val outputDir: DirectoryProperty

    @TaskAction
    fun download() {
        val target = outputDir.file("gesture_recognizer.task").get().asFile
        val bytes = URI(url.get()).toURL().openStream().use { it.readBytes() }
        val actual = MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it) }
        check(actual == sha256.get()) { "Gesture model checksum mismatch: $actual" }
        target.writeBytes(bytes)
    }
}

val downloadGestureModel = tasks.register<DownloadGestureModel>("downloadGestureModel") {
    url = "https://storage.googleapis.com/mediapipe-models/gesture_recognizer/gesture_recognizer/float16/1/gesture_recognizer.task"
    sha256 = "97952348cf6a6a4915c2ea1496b4b37ebabc50cbbf80571435643c455f2b0482"
    outputDir = layout.buildDirectory.dir("generated/models")
}

androidComponents {
    onVariants { variant ->
        variant.sources.assets?.addGeneratedSourceDirectory(downloadGestureModel, DownloadGestureModel::outputDir)
    }
}

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

    androidResources {
        // MediaPipe memory-maps the model, which fails on compressed assets.
        noCompress += "task"
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.adaptive.navigation.suite)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.mediapipe.tasks.vision)

    testImplementation(libs.junit)
}
