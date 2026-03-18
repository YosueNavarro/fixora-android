plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.tynsolutions.gestionaveriasmovil"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.tynsolutions.gestionaveriasmovil"
        minSdk = 23
        targetSdk = 36
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation("androidx.fragment:fragment-ktx:1.8.9")

    // --- RED Y COMUNICACIÓN API (Retrofit + OkHttp) ---
    // Retrofit: Cliente HTTP principal y type-safe para Android
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    // Convertidor Gson: Transforma automáticamente los JSON del servidor a Data Classes de Kotlin
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // OkHttp: Cliente subyacente. Fundamental para configurar tiempos de espera y seguridad
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    // Interceptor de OkHttp: Útil para ver las peticiones/respuestas en el Logcat durante el desarrollo
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
}