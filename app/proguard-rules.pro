-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

# https://issuetracker.google.com/issues/190382641
-keepclassmembers class kotlin.SafePublicationLazyImpl {
    java.lang.Object _value;
}

-keep class androidx.viewpager.widget.ViewPager$LayoutParams { int position; }

-keep class com.hippo.ehviewer.client.parser.Torrent { *; }
-keep class com.hippo.ehviewer.client.parser.TorrentResult { *; }

-keepattributes LineNumberTable,SourceFile
-renamesourcefileattribute SourceFile

-repackageclasses
-allowaccessmodification
-overloadaggressively
