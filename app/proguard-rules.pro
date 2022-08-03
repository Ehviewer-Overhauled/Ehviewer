-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    public static void check*(...);
    public static void throw*(...);
}

-keep class com.hippo.ehviewer.ui.fragment.* extends com.hippo.ehviewer.ui.fragment.BaseFragment { }
-keep class com.hippo.ehviewer.ui.fragment.* extends com.hippo.ehviewer.ui.fragment.BasePreferenceFragment { }

-keepnames class com.hippo.ehviewer.ui.scene.* { }

-keep class com.hippo.ehviewer.dao.* { *; }

-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute
-repackageclasses
-allowaccessmodification
-overloadaggressively

-dontwarn net.sqlcipher.database.**
-dontwarn rx.**
-dontwarn org.conscrypt.ConscryptHostnameVerifier
-dontwarn org.conscrypt.Conscrypt$Version
-dontwarn org.bouncycastle.jsse.provider.BouncyCastleJsseProvider
-dontwarn org.bouncycastle.jsse.BCSSLSocket
-dontwarn org.openjsse.javax.net.ssl.SSLParameters
-dontwarn net.sqlcipher.Cursor
-dontwarn org.bouncycastle.jsse.BCSSLParameters
-dontwarn org.conscrypt.Conscrypt
-dontwarn org.openjsse.javax.net.ssl.SSLSocket
-dontwarn org.openjsse.net.ssl.OpenJSSE

-keepclasseswithmembernames class * { native <methods>;}
-keepclassmembers class com.hippo.image.Image {private <init>(long, int, int, int);}
