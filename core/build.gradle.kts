plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

android {
    namespace = "app.grapheneos.seedvault.core"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments["disableAnalytics"] = "true"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
        languageVersion = "1.8"
        freeCompilerArgs += listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-Xexplicit-api=strict"
        )
    }
}

dependencies {
    val aospLibs: FileTree by rootProject.extra
    compileOnly(aospLibs)
    compileOnly(kotlin("test"))
    implementation(libs.bundles.kotlin)
    implementation(libs.bundles.coroutines)
    implementation(libs.androidx.documentfile)
    implementation(libs.androidx.core.ktx)
    implementation(libs.google.tink.android)
    implementation(fileTree("$projectDir/libs/dav4jvm").include("*.jar"))
    implementation(libs.squareup.okio)
    implementation(libs.kotlin.logging)
    implementation(libs.slf4j.api)

    testImplementation(kotlin("test"))
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.xpp3)
    testImplementation(libs.slf4j.simple)
}
