// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    extra.apply {
        set("room_version", "2.6.1")
        set("compose_bom_version", "2024.12.01")
        set("hilt_version", "2.51.1")
        set("lifecycle_version", "2.7.0")
        set("navigation_version", "2.7.6")
        set("camerax_version", "1.4.0")
    }
}

plugins {
    id("com.android.application") version "8.13.0" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
    id("com.google.dagger.hilt.android") version "2.51.1" apply false
    id("com.google.devtools.ksp") version "2.0.21-1.0.26" apply false
}

