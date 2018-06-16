-repackageclasses

-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable

# Models
-keepclassmembernames class com.simonlebras.radiofrance.data.models.Radio { *; }

# OkHttp
-dontwarn okhttp3.internal.platform.*

# Support Library
-keep class android.support.v7.widget.SearchView {
   public <init>(android.content.Context);
   public <init>(android.content.Context, android.util.AttributeSet);
}
-keep class android.support.v7.app.MediaRouteActionProvider {
    public <init>(android.content.Context);
}

# Firestore
-keep class io.grpc.** {*;}
