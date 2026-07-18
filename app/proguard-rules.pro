# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# 1. Metadata Generic & Annotations (Sangat Penting untuk ParameterizedType Error)
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes RuntimeInvisibleAnnotations, RuntimeInvisibleParameterAnnotations

# 2. Retrofit & OkHttp
-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }
-dontwarn retrofit2.**
-dontwarn okhttp3.**

# Menjaga Interface Service (Penting agar method API Laravel tetap ada)
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# 3. Gson & JSON Modeling
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class * extends com.google.gson.reflect.TypeToken { *; }

# Menjaga SerializedName agar field JSON tidak berubah namanya
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# 4. Data Models (Aplikasi Anda)
# Pastikan semua class di package data tidak di-minify
-keep class id.my.matahati.absensi.data.** { *; }

# 5. Kotlin Coroutines (Mencegah error pada suspend functions)
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class * extends kotlinx.coroutines.AbstractCoroutine { *; }

# 6. ML Kit & Firebase (Konfigurasi bawaan sebelumnya)
-keep class com.google.mlkit.** { *; }
-keep class com.google.firebase.** { *; }

# 7. Room Database
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**
