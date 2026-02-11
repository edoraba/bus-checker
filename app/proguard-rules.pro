# Add project specific ProGuard rules here.

# Keep Retrofit and Gson models
-keep class com.redergo.buspullman.data.** { *; }
-keep class com.redergo.buspullman.service.UpdateManager$UpdateInfo { *; }
-keepattributes Signature
-keepattributes *Annotation*

# Retrofit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Gson
-keep class com.google.gson.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Glance widget
-keep class com.redergo.buspullman.widget.** { *; }
