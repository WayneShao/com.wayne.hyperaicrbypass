plugins {
    id("com.android.application")
}

val splitAbi = providers.gradleProperty("splitAbi")
    .orNull
    ?.toBooleanStrictOrNull()
    ?: false

android {
    namespace = "com.wayne.hyperaicrbypass"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.wayne.hyperaicrbypass"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        buildConfigField(
            "String",
            "SETTINGS_AUTHORITY",
            "\"com.wayne.hyperaicrbypass.settings\""
        )

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/**",
                "kotlin/**"
            )
        }
    }

    splits {
        abi {
            isEnable = splitAbi
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
            isUniversalApk = splitAbi
        }
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation("org.luckypray:dexkit:2.2.0")
    compileOnly("de.robv.android.xposed:api:82")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
}
