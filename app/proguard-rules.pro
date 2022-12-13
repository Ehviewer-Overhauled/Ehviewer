-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    public static void check*(...);
    public static void throw*(...);
}

-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

-keep,allowoptimization class com.hippo.ehviewer.ui.fragment.* extends com.hippo.ehviewer.ui.fragment.BaseFragment { }
-keep,allowoptimization class com.hippo.ehviewer.ui.fragment.* extends com.hippo.ehviewer.ui.fragment.BasePreferenceFragment { }

-keep class androidx.viewpager.widget.ViewPager$LayoutParams { int position; }

-renamesourcefileattribute
-repackageclasses
-allowaccessmodification
-mergeinterfacesaggressively
-overloadaggressively
