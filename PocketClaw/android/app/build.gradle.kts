import java.util.Properties
import java.io.FileInputStream

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.pocketclaw.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.pocketclaw.app"
        minSdk = 27
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        val hfToken: String = localProperties.getProperty("HF_TOKEN", "")
        buildConfigField("String", "HF_TOKEN", "\"$hfToken\"")

        ndk {
            abiFilters += setOf("arm64-v8a")
            debugSymbolLevel = "FULL"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            ndk { debugSymbolLevel = "NONE" }
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlin {
        compilerOptions {
            jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/LICENSE*"
            excludes += "META-INF/DEPENDENCIES*"
            excludes += "META-INF/NOTICE*"
            excludes += "META-INF/*.kotlin_module"
            excludes += "google/protobuf/*.proto"
        }
        jniLibs {
            useLegacyPackaging = true
            pickFirsts += setOf("**/libmediapipe_tasks_text_jni.so")
            pickFirsts += setOf("**/libonnxruntime.so")
            excludes += setOf("**/libdeepseek-ocr.so")
            excludes += setOf("**/libstable-diffusion.so")
        }
    }
}

configurations.all {
    resolutionStrategy {
        force("com.google.protobuf:protobuf-java:3.25.1")
        force("com.microsoft.onnxruntime:onnxruntime-android:1.24.1")
    }
    exclude(group = "com.google.protobuf", module = "protobuf-javalite")
}

dependencies {
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Room Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // LocalBroadcastManager
    implementation("androidx.localbroadcastmanager:localbroadcastmanager:1.1.0")

    // WorkManager (scheduled tasks)
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Security (EncryptedSharedPreferences)
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // AppCompat
    implementation("androidx.appcompat:appcompat:1.7.0")

    // Permissions
    implementation(libs.accompanist.permissions)

    // Image Loading
    implementation(libs.coil.compose)

    // JSON
    implementation(libs.gson)

    // Document parsing
    implementation("org.apache.commons:commons-csv:1.10.0")
    implementation("com.itextpdf:itext7-core:7.2.5")

    // Ktor
    implementation("io.ktor:ktor-client-android:2.3.6")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.6")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.6")

    // OkHttp
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // MediaPipe (LLM inference + embeddings)
    implementation("com.google.mediapipe:tasks-genai:0.10.32")
    implementation("com.google.mediapipe:tasks-vision:0.10.32")
    implementation("com.google.mediapipe:tasks-text:0.10.32")

    // Protobuf (required by MediaPipe)
    implementation("com.google.protobuf:protobuf-java:3.25.1")
    implementation("org.slf4j:slf4j-nop:2.0.9")

    // AI Edge RAG SDK
    implementation("com.google.ai.edge.localagents:localagents-rag:0.3.0")

    // Markdown
    implementation("com.github.jeziellago:compose-markdown:0.3.0")
    implementation("com.vladsch.flexmark:flexmark-all:0.64.8")

    // ONNX Runtime
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.24.1")

    // Nexa SDK (GGUF)
    implementation("ai.nexa:core:0.0.24") {
        exclude(group = "com.microsoft.onnxruntime")
    }

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
