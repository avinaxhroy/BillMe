# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep data classes and entities
-keep class com.billme.app.data.** { *; }
-keep class com.billme.app.core.** { *; }
-keep class com.billme.app.presentation.** { *; }
-keep class com.billme.app.domain.** { *; }

# Room Database
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-keep @androidx.room.Query class *
-dontwarn androidx.room.paging.**
-keepclassmembers class * implements androidx.room.RoomDatabase {
    public *** *(...);
}

# Hilt - CRITICAL: Prevents duplicate ViewModel key crashes in release builds
-keep class dagger.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-dontwarn com.google.errorprone.annotations.**
-keep class dagger.hilt.** { *; }

# Keep all Hilt ViewModels with their names intact
-keepnames @dagger.hilt.android.lifecycle.HiltViewModel class * extends androidx.lifecycle.ViewModel
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }
-keepclassmembers class * {
    @dagger.hilt.android.lifecycle.HiltViewModel <init>(...);
}

# Keep all classes annotated with @Inject
-keepclasseswithmembers class * {
    @javax.inject.Inject <init>(...);
}
-keepclasseswithmembers class * {
    @javax.inject.Inject <fields>;
}

# Keep Hilt generated components
-keep class * extends dagger.hilt.internal.GeneratedComponent
-keep class **_HiltModules { *; }
-keep class **_HiltModules$** { *; }
-keep class **_Factory { *; }
-keep class **_MembersInjector { *; }

# Keep ViewModelFactory classes
-keep class dagger.hilt.android.internal.lifecycle.** { *; }
-keep class androidx.hilt.navigation.** { *; }

# Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep Serializers
-keep,includedescriptorclasses class com.billme.app.**$$serializer { *; }
-keepclassmembers class com.billme.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.billme.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Retrofit & OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keepattributes Signature
-keepattributes Exceptions
-keep class retrofit2.** { *; }

# iText PDF and BouncyCastle
-dontwarn com.itextpdf.bouncycastle.BouncyCastleFactory
-dontwarn com.itextpdf.bouncycastlefips.BouncyCastleFipsFactory
-keep class com.itextpdf.** { *; }
-dontwarn com.itextpdf.**

# SLF4J
-dontwarn org.slf4j.impl.StaticLoggerBinder
-dontwarn org.slf4j.**

# ML Kit & OCR
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**
-dontwarn com.google.mlkit.**

# Google Drive API - CRITICAL: Prevents "key error" in release builds
-keep class com.google.api.client.** { *; }
-keep class com.google.api.services.drive.** { *; }
-dontwarn com.google.api.client.**
-keepattributes Signature,RuntimeVisibleAnnotations,AnnotationDefault
-keepclassmembers class * {
  @com.google.api.client.util.Key <fields>;
}
# Keep Google HTTP Client
-keep class com.google.api.client.http.** { *; }
-keep class com.google.api.client.json.** { *; }
-keep class com.google.api.client.googleapis.** { *; }

# Apache HTTP Client - excluded from dependencies but referenced
-dontwarn org.apache.http.**
-dontwarn org.apache.commons.**
-dontwarn javax.naming.**
-dontwarn org.ietf.jgss.**

# Tesseract
-keep class com.googlecode.tesseract.** { *; }
-dontwarn com.googlecode.tesseract.**

# Prevent R8 from removing @Composable functions
-keep @androidx.compose.runtime.Composable class * { *; }
-keep class androidx.compose.** { *; }
-keepclassmembers class androidx.compose.** {
    public <methods>;
}
-keep interface androidx.compose.** { *; }
-keep class androidx.compose.ui.** { *; }

# AndroidX
-keep class androidx.** { *; }
-keep interface androidx.** { *; }
-keep class androidx.appcompat.** { *; }
-keep class androidx.fragment.** { *; }
-keep class androidx.lifecycle.** { *; }

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep View constructors
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public void set*(...);
}

# Keep Activity and Fragment
-keep class * extends android.app.Activity
-keep class * extends android.app.Fragment
-keep class * extends androidx.fragment.app.Fragment

# Preserve line number information for debugging stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
