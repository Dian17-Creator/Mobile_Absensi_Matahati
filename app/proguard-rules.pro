# ProGuard Rules untuk Android 15 & API Laravel

# 1. Menjaga Metadata Generic (WAJIB untuk ParameterizedType Error)
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes RuntimeInvisibleAnnotations, RuntimeInvisibleParameterAnnotations
-keepattributes AnnotationDefault

# 2. Retrofit 2
-keep class retrofit2.** { *; }
-keep class id.my.matahati.absensi.data.** { *; }
-keep interface id.my.matahati.absensi.data.** { *; }
-dontwarn retrofit2.**
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# 3. Gson
-keep class com.google.gson.** { *; }
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# 4. OkHttp 3
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# 5. Menjaga Data Models Aplikasi
# Ini sangat penting: semua class model API Anda harus tetap utuh
-keep public class id.my.matahati.absensi.data.** { *; }
-keepclassmembers class id.my.matahati.absensi.data.** { *; }

# 6. Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class * extends kotlinx.coroutines.AbstractCoroutine { *; }
