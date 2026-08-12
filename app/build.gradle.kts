plugins {
    id("com.android.application")
}

val splitAbi = providers.gradleProperty("splitAbi")
    .orNull
    ?.toBooleanStrictOrNull()
    ?: false

android {
    namespace = "com.wayne.hyperaicrbypass"
    compileSdk = 37
    buildToolsVersion = "37.0.0"

    defaultConfig {
        applicationId = "com.wayne.hyperaicrbypass"
        minSdk = 28
        targetSdk = 37
        versionCode = 4
        versionName = "1.2.0"

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
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        buildConfig = true
    }

    packaging {
        resources {
            merges += "META-INF/xposed/*"
            excludes += setOf(
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
    compileOnly("io.github.libxposed:api:102.0.0")
    implementation("io.github.libxposed:service:102.0.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("io.github.libxposed:api:102.0.0")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
}
