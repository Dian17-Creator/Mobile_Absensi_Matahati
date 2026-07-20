# ProGuard Rules untuk Android 15 & API Laravel

# 1. Menjaga Metadata Generic (WAJIB untuk ParameterizedType Error)
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes RuntimeInvisibleAnnotations, RuntimeInvisibleParameterAnnotations
-keepattributes AnnotationDefault

# 2. Retrofit 2 & Networking
# Menjaga interface agar tidak dihapus atau diganti namanya
-keep interface id.my.matahati.absensi.data.ApiService { *; }
-keepclassmembers interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**

# 3. Gson & Data Models
# Menjaga field yang dipetakan ke JSON, namun tetap mengizinkan obfuscation pada nama class
-keepclassmembers,allowobfuscation class id.my.matahati.absensi.data.** {
    @com.google.gson.annotations.SerializedName <fields>;
    <init>(...);
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
