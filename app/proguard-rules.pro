# Keep Hilt
-keep class dagger.hilt.** { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }

# Keep Room entities
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.Database class * { *; }

# Keep Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Keep ZXing
-keep class com.google.zxing.** { *; }
-keep class com.journeyapps.** { *; }

# Keep Ktor
-keep class io.ktor.** { *; }
-keep class io.netty.** { *; }

# Keep crypto classes
-keep class com.alittleapp.feature_transfer.crypto.** { *; }
-keep class javax.crypto.** { *; }

# Keep Gson serialization models
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }

# Suppress warnings for Netty and Ktor JVM internals
-dontwarn io.netty.**
-dontwarn io.ktor.**
-dontwarn org.slf4j.**
