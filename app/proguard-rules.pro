# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

#### Nav
-keepnames class androidx.navigation.fragment.NavHostFragment
-keepnames class * extends android.os.Parcelable
-keepnames class * extends java.io.Serializable

#### OkHttp, Retrofit and Moshi
-dontwarn okhttp3.**
-dontwarn retrofit2.Platform$Java8
-dontwarn okio.**
-dontwarn javax.annotation.**
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-keepclasseswithmembers class * {
    @com.squareup.moshi.* <methods>;
}
-keep @com.squareup.moshi.JsonQualifier interface *
-dontwarn org.jetbrains.annotations.**
-keep class kotlin.Metadata { *; }
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

-keepclassmembers class * {
    @com.squareup.moshi.FromJson <methods>;
    @com.squareup.moshi.ToJson <methods>;
}

-keepnames @kotlin.Metadata class com.hoc.comicapp.data.remote.**
-keepnames @kotlin.Metadata class com.hoc.comicapp.data.remote.**$*
-keepnames @kotlin.Metadata class com.hoc.comicapp.data.local.entities.ComicEntity$Author
-keepnames @kotlin.Metadata class com.hoc.comicapp.data.local.entities.ComicEntity$Category

-keep class com.hoc.comicapp.data.remote.** { *; }
-keep class com.hoc.comicapp.data.remote.**$* { *; }
-keep class com.hoc.comicapp.data.local.entities.ComicEntity$Author { *; }
-keep class com.hoc.comicapp.data.local.entities.ComicEntity$Category { *; }

-keepclassmembers class com.hoc.comicapp.data.remote.** { *; }
-keepclassmembers class com.hoc.comicapp.data.remote.**$* { *; }
-keepclassmembers class com.hoc.comicapp.data.local.entities.ComicEntity$Author { *; }
-keepclassmembers class com.hoc.comicapp.data.local.entities.ComicEntity$Category { *; }