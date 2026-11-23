plugins {
    id("com.android.application")
}
android {
    namespace = "music.nuspective.mymidicc"
    compileSdk = 35

    defaultConfig {
        applicationId = "music.nuspective.mymidicc"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    // The original web/ directory is copied into the APK as-is, so the
    // installed app always has a working offline baseline.
    sourceSets.getByName("main").assets.srcDir("../web")
}
dependencies {
    implementation("androidx.webkit:webkit:1.12.1")
}
