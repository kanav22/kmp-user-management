# Kotlin serialization — keep all @Serializable classes
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.sliide.usermanager.**$$serializer { *; }
-keepclassmembers class com.sliide.usermanager.** {
    *** Companion;
}
-keepclasseswithmembers class com.sliide.usermanager.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Ktor — keep client internals used via reflection
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# SQLDelight — generated query classes
-keep class com.sliide.usermanager.data.local.db.** { *; }
-keep class app.cash.sqldelight.** { *; }
-dontwarn app.cash.sqldelight.**

# Koin — keep module declarations
-keep class org.koin.** { *; }
-dontwarn org.koin.**

# Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# Compose — keep lambdas and composable functions
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# OkHttp (Ktor Android engine)
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# Keep line numbers for crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
