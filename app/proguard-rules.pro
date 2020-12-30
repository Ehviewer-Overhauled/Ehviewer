# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in E:\Android\android-sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

-keepattributes SourceFile,LineNumberTable
-keep class com.hippo.a7zip.* { *; }
-keep class com.hippo.ehviewer.ui.fragment.* extends com.hippo.ehviewer.ui.fragment.BaseFragment { }
-keep class com.hippo.ehviewer.ui.fragment.* extends com.hippo.ehviewer.ui.fragment.BasePreferenceFragment { }
-keepnames class com.hippo.ehviewer.ui.scene.* { }
-keep class com.hippo.image.* { *; }
-keepclassmembers class * extends org.greenrobot.greendao.AbstractDao {
public static java.lang.String TABLENAME;
}
-keep class **$Properties { *; }
-dontwarn net.sqlcipher.database.**
-dontwarn rx.**
-dontwarn org.conscrypt.ConscryptHostnameVerifier