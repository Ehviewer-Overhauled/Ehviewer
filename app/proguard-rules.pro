-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

-keepclassmembers class kotlin.SafePublicationLazyImpl {
    java.lang.Object _value;
}

-keep class androidx.viewpager.widget.ViewPager$LayoutParams { int position; }

-keepattributes LineNumberTable,SourceFile
-renamesourcefileattribute SourceFile

-repackageclasses
-allowaccessmodification
-overloadaggressively
