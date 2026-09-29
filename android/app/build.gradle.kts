import java.net.URI
import java.security.MessageDigest

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
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

room {
    // Committed so future schema changes can be checked and migrated.
    schemaDirectory("$projectDir/schemas")
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
        // java.time (history dates) on minSdk 24.
        isCoreLibraryDesugaringEnabled = true
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        // Robolectric screenshot rendering needs the merged resources.
        unitTests.isIncludeAndroidResources = true
    }

    androidResources {
        // MediaPipe memory-maps the model, which fails on compressed assets.
        noCompress += "task"
    }
}

// Screenshot tests under src/test/.../screenshots render screens to PNG for design review. They are
// excluded from normal test runs (and CI); render them with: ./gradlew testDebugUnitTest -Pscreenshots
// Images land in app/build/outputs/roborazzi/.
tasks.withType<Test>().configureEach {
    if (providers.gradleProperty("screenshots").isPresent) {
        systemProperty("roborazzi.test.record", "true")
        filter.includeTestsMatching("com.silentvoix.app.screenshots.*")
    } else {
        exclude("**/screenshots/**")
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
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    testImplementation(libs.junit)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
