-repackageclasses
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable

# Models
-keepclassmembernames class com.simonlebras.radiofrance.models.** { *; }

# Dagger
-dontwarn com.google.errorprone.annotations.**

# Retrofit
-dontnote retrofit2.Platform
-dontwarn retrofit2.Platform$Java8
-keepattributes Signature
-keepattributes Exceptions

# Okio
-dontwarn okio.**

# Support Library
-keep class android.support.v7.app.MediaRouteActionProvider { *; }
-keep class android.support.v7.widget.SearchView { *; }

# Timber
-assumenosideeffects class timber.log.Timber { *; }
