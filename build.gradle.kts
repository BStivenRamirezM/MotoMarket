plugins {
    alias(libs.plugins.android.application) apply false
    // Fija el compilador de Kotlin 2.4 para el Kotlin integrado de AGP 9 (Firebase BOM 35 lo requiere).
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.google.services) apply false
}
