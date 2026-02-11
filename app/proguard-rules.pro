# Add project specific ProGuard rules here.

# Keep data models and API services
-keep class com.redergo.buspullman.data.** { *; }
-keep class com.redergo.buspullman.service.UpdateManager$UpdateInfo { *; }
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes InnerClasses,EnclosingMethod

# Retrofit: keep service interfaces and their generic return types
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
# Keep Retrofit service interfaces (needed for suspend function return types)
-keep interface com.redergo.buspullman.data.BusApiService { *; }
-keep interface com.redergo.buspullman.service.GitHubApiService { *; }

# Kotlin coroutines + Retrofit suspend functions
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Gson: keep generic type info for deserialization
-keep class com.google.gson.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Glance widget
-keep class com.redergo.buspullman.widget.** { *; }
