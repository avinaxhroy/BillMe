plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.billme.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.billme.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 2
        versionName = "2.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Signing config should be added by user before release
            // signingConfig = signingConfigs.getByName("release")
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-DEBUG"
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
    
    
    buildFeatures {
        compose = true
    }
    
    
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
        }
    }
    
    lint {
        disable.add("MissingPermission")
        disable.add("NewApi")
        disable.add("FullBackupContent")
        checkReleaseBuilds = false
        // Use lint.xml for more detailed warning suppression
        lintConfig = file("lint.xml")
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:${rootProject.extra["compose_bom_version"]}")
    implementation(composeBom)
    
    // Core Android
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:${rootProject.extra["lifecycle_version"]}")
    implementation("androidx.activity:activity-compose:1.8.2")
    
    // Jetpack Compose
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    
    // Navigation
    implementation("androidx.navigation:navigation-compose:${rootProject.extra["navigation_version"]}")
    
    // ViewModel & State
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:${rootProject.extra["lifecycle_version"]}")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:${rootProject.extra["lifecycle_version"]}")
    
    // Room Database
    implementation("androidx.room:room-runtime:${rootProject.extra["room_version"]}")
    implementation("androidx.room:room-ktx:${rootProject.extra["room_version"]}")
    ksp("androidx.room:room-compiler:${rootProject.extra["room_version"]}")
    
    // Dependency Injection - Hilt
    implementation("com.google.dagger:hilt-android:${rootProject.extra["hilt_version"]}")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")
    ksp("com.google.dagger:hilt-compiler:${rootProject.extra["hilt_version"]}")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    
    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")
    
    // Camera & Scanning
    implementation("androidx.camera:camera-core:${rootProject.extra["camerax_version"]}")
    implementation("androidx.camera:camera-camera2:${rootProject.extra["camerax_version"]}")
    implementation("androidx.camera:camera-lifecycle:${rootProject.extra["camerax_version"]}")
    implementation("androidx.camera:camera-view:${rootProject.extra["camerax_version"]}")
    
        // Barcode Scanning
    implementation("com.google.mlkit:barcode-scanning:17.3.0")
    
    // ML Kit Text Recognition - Primary OCR
    implementation("com.google.mlkit:text-recognition:16.0.0")
    
    // Date & Time
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0")
    
    // DataStore (preferences)
    implementation("androidx.datastore:datastore-preferences:1.1.0")

    // Compose foundation (LazyRow, foundation APIs)
    implementation("androidx.compose.foundation:foundation")
    
    // Bluetooth Printing
    implementation("com.github.DantSu:ESCPOS-ThermalPrinter-Android:3.3.0")
    
    // PDF Generation
    implementation("com.itextpdf:itext7-core:8.0.2")
    
    // Permission handling
    implementation("com.google.accompanist:accompanist-permissions:0.32.0")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.8.1")
    
    // Hilt Worker support
    implementation("androidx.hilt:hilt-work:1.1.0")
    ksp("androidx.hilt:hilt-compiler:1.1.0")
    
    // Security (for encryption)
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    
    // Image Loading (Coil)
    implementation("io.coil-kt:coil-compose:2.5.0")
    
    // Google Play Services - Authentication and Drive
    implementation("com.google.android.gms:play-services-auth:21.0.0")
    
    // Google API Client (for Drive API v3)
    implementation("com.google.api-client:google-api-client-android:2.2.0") {
        exclude(group = "org.apache.httpcomponents", module = "httpclient")
    }
    implementation("com.google.apis:google-api-services-drive:v3-rev20220815-2.0.0") {
        exclude(group = "org.apache.httpcomponents", module = "httpclient")
    }
    implementation("com.google.http-client:google-http-client-gson:1.43.3")
    implementation("com.google.http-client:google-http-client-android:1.43.3")
    
    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("androidx.room:room-testing:${rootProject.extra["room_version"]}")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("io.mockk:mockk:1.13.8")
    
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:${rootProject.extra["compose_bom_version"]}"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("com.google.dagger:hilt-android-testing:${rootProject.extra["hilt_version"]}")
    kspAndroidTest("com.google.dagger:hilt-compiler:${rootProject.extra["hilt_version"]}")
    
    debugImplementation(platform("androidx.compose:compose-bom:${rootProject.extra["compose_bom_version"]}"))
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}