plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.TNTSMobileApp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.TNTSMobileApp"
        minSdk = 24
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Use Firebase BOM to manage versions
    implementation(platform("com.google.firebase:firebase-bom:32.2.3"))
    implementation("com.google.firebase:firebase-auth") // Use BOM version
    implementation("com.google.firebase:firebase-analytics") // Use BOM version
    implementation("com.google.firebase:firebase-firestore-ktx") // Use BOM version
    implementation("com.firebaseui:firebase-ui-auth:8.0.2") // Ensure this is compatible with BOM

    // Remove the alpha version if not necessary
    // If you need it, ensure it's compatible with the BOM
    // implementation("com.google.firebase:firebase-dataconnect:16.0.0-alpha05")

    // Google Play Services Auth
    implementation("com.google.android.gms:play-services-auth:20.5.0") // Ensure this is compatible

    // Image loading library
    implementation("com.github.bumptech.glide:glide:4.12.0")

    // Activity KTX
    implementation("androidx.activity:activity-ktx:1.8.2")

    // Kotlin script runtime (if needed)
    implementation(kotlin("script-runtime"))

    // Testing dependencies
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    implementation ("androidx.recyclerview:recyclerview:1.3.1")
    implementation ("com.facebook.shimmer:shimmer:0.5.0")
    implementation ("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.0")
    implementation ("androidx.lifecycle:lifecycle-livedata-ktx:2.6.0")


}
