# ProGuard Rules untuk Android 15 & API Laravel

# 1. Menjaga Metadata Generic (WAJIB untuk ParameterizedType Error)
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes RuntimeInvisibleAnnotations, RuntimeInvisibleParameterAnnotations
-keepattributes AnnotationDefault

# 2. Retrofit 2 & Networking
# Menjaga SELURUH interface API di package data agar signature generic-nya tidak hilang
-keep interface id.my.matahati.absensi.data.** { *; }

# Menjaga method yang memiliki anotasi Retrofit secara global
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**

# WAJIB untuk suspend fun + Response<T> (R8 full mode) - kunci fix ParameterizedType error
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-keep,allowobfuscation,allowshrinking class retrofit2.Response

-dontwarn org.codehaus.mojo.animal_sniffer.AnnotationAvailable
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

# Gson TypeToken (untuk TypeToken<T> manual di kode, jika ada)
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# 3. Gson & Data Models
# Menjaga field yang dipetakan ke JSON.
# Kita tidak menggunakan 'allowobfuscation' di sini untuk menjamin metadata 'Signature' tetap utuh.
-keep class id.my.matahati.absensi.data.** {
    @com.google.gson.annotations.SerializedName <fields>;
    <init>(...);
}

# Jaga semua field data class (jaga-jaga untuk yang tidak pakai @SerializedName)
-keepclassmembers class id.my.matahati.absensi.data.** {
    <fields>;
}
-dontwarn com.google.gson.**

# 4. OkHttp 3 & Core Libraries
-dontwarn okhttp3.**
-dontwarn okio.**

# 5. Room Database
-keep @androidx.room.Entity class *
-keepclassmembers class * {
    @androidx.room.PrimaryKey <fields>;
    @androidx.room.ColumnInfo <fields>;
}

# 6. Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class * extends kotlinx.coroutines.AbstractCoroutine { *; }