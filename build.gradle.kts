// AGP 9 gomulu Kotlin kullanir (varsayilan KGP 2.3.10); compose/serialization
// eklentileriyle derleyici surumu catismasin diye KGP'yi acikca 2.3.21'e sabitliyoruz.
// Resmi yontem: https://developer.android.com/build/releases/agp-9-0-0-release-notes
buildscript {
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.21")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.chaquopy) apply false
}
