# Android ProGuard rules

# Add project specific ProGuard rules here.
# By default, the tools ignore the Android specific classes.
# You can edit the include path to add your own rule files.
#
# For more information, see:
#   http://developer.android.com/guide/developing/tools/proguard.html
#   http://source.android.com/source/code/proguard-android.txt

# Basic ProGuard rules
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose

# Keep all Activities, Services, BroadcastReceivers
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver

# Keep all Views
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# Keep all Parcelable implementations
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep R classes
-keep class **.R$* { *; }

# Keep classes with @Keep annotation
-keep @com.android.annotations.Keep class * { *; }

# Keep Room database classes
-keep class androidx.room.** { *; }
-keep class * extends androidx.room.Database { *; }

# Keep Retrofit service classes
-keep class com.sonosequencer.alternative.network.** { *; }
-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }
-keep class com.google.gson.** { *; }

# Keep Kotlin coroutines
-keep class kotlinx.coroutines.** { *; }

# Keep Material Design components
-keep class com.google.android.material.** { *; }

# Keep AndroidX classes
-keep class androidx.** { *; }

# Keep Sonos API classes
-keep class com.sonosequencer.alternative.data.** { *; }

# Don't warn about unused code in these packages
-dontwarn com.squareup.okhttp.**
-dontwarn com.squareup.retrofit.**
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn com.google.gson.**
-dontwarn kotlinx.coroutines.**
-dontwarn androidx.room.**

# Optimizations
-optimizations !code/simplification/arithmetic
-optimizationpasses 5
-allowaccessmodification

# Keep all enum values
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep all static fields
-keepclassmembers class * {
    static <fields>;
}

# Keep all public methods
-keepclassmembers class * {
    public <methods>;
}
