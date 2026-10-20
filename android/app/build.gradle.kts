import java.util.Properties

plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
    // The Flutter Gradle Plugin must be applied after the Android and Kotlin Gradle plugins.
    id("dev.flutter.flutter-gradle-plugin")
}

// Release signing credentials come from keystore.properties (git-ignored, at the
// repository root) and fall back to environment variables so CI can inject them.
val keystoreProperties = Properties().apply {
    val file = rootProject.file("../keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun signingValue(propertyName: String, envName: String, default: String = ""): String =
    keystoreProperties.getProperty(propertyName) ?: System.getenv(envName) ?: default

android {
    namespace = "com.multisarv.societysarv"
    compileSdk = flutter.compileSdkVersion
    ndkVersion = flutter.ndkVersion

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    defaultConfig {
        // Same id as the Kotlin app, so this build installs as an update over it.
        applicationId = "com.multisarv.societysarv"
        minSdk = maxOf(flutter.minSdkVersion, 24)
        targetSdk = flutter.targetSdkVersion
        versionCode = flutter.versionCode
        versionName = flutter.versionName
    }

    signingConfigs {
        create("release") {
            val keystorePath = signingValue("storeFile", "KEYSTORE_PATH", "societysarv-release-key.jks")
            storeFile = rootProject.file("../$keystorePath")
            storePassword = signingValue("storePassword", "STORE_PASSWORD")
            keyAlias = signingValue("keyAlias", "KEY_ALIAS", "societysarv")
            keyPassword = signingValue("keyPassword", "KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            // Falls back to the debug key when no release keystore is present,
            // so `flutter run --release` still works on a developer machine.
            signingConfig = if (rootProject.file("../${signingValue("storeFile", "KEYSTORE_PATH", "societysarv-release-key.jks")}").exists()) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

flutter {
    source = "../.."
}
