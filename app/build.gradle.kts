plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.reconocimientofacial"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.reconocimientofacial"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {


    // Retrofit para peticiones HTTP
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // OkHttp para el cliente y logs de red
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")

    //Se han añadido las siguientes dependencias
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    // librería principal para realizar peticiones HTTP a servidores.

    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    // Es un complemento para Retrofit. Traduce los textos en formato JSON que te devolverá
    // la API y convertirlos automáticamente en variables de Kotlin.

    implementation("androidx.core:core-ktx:1.12.0")
    // Paquete de utilidades kotlin, extensiones.

    implementation("androidx.appcompat:appcompat:1.6.1")
    // Asegura que componentes visuales se vean y funcionen en dispositivos más antiguos.

    implementation("com.google.android.material:material:1.11.0")
    // Proporciona el catálogo oficial de diseño visual de Google (Material Design).

    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    // Permite posicionar el espacio para la foto y los botones de manera flexible,
    // anclándolos unos a otros para que la interfaz se adapte perfectamente ya sea en
    // un celular pequeño o en uno muy grande.

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.exifinterface)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}