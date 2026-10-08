plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt") // Agrega esta línea
}

android {
    namespace = "com.tuempresa.inventario"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.tuempresa.inventario"
        minSdk = 26
        targetSdk = 35
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
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation ("com.google.code.gson:gson:2.10.1")
    implementation("org.apache.poi:poi:5.2.3") // o la versión más reciente
    implementation("org.apache.poi:poi-ooxml:5.2.3")
    implementation("androidx.recyclerview:recyclerview:1.3.2") // o la versión más reciente
    implementation("androidx.recyclerview:recyclerview-selection:1.1.0")
    implementation("androidx.room:room-runtime:2.6.1") // o la versión más reciente
    annotationProcessor("androidx.room:room-compiler:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    // Para Kotlin KAPT (Kotlin Annotation Processing Tool)
    kapt("androidx.room:room-compiler:2.6.1") // o la versión más reciente
    implementation ("net.sourceforge.jexcelapi:jxl:2.6.12")
    implementation(libs.play.code.scanner)
}