plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-parcelize")
    alias(libs.plugins.google.ksp)
}

android {
    namespace = "io.github.galitach.mathhero"
    compileSdk = 36
    kotlin {
        jvmToolchain(21)
    }
    defaultConfig {
        applicationId = "io.github.galitach.mathhero"
        minSdk = 24
        targetSdk = 36
        versionCode = 9
        versionName = "4.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.google.ads)
    implementation(libs.google.ump)
    implementation(libs.androidx.recyclerview)
    implementation(libs.bundles.google.play.appUpdate)
    testImplementation(libs.junit)
    implementation(libs.google.play.billing)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(libs.konfetti.view)
    implementation(libs.androidx.splashscreen)
    implementation(libs.google.play.services.oss.licenses)

    // db
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
}