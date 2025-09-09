# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# --- General Best Practices ---
-dontobfuscate
-keepattributes Signature,InnerClasses,EnclosingMethod
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.android.ump.** { *; }
-keep class nl.dionsegijn.konfetti.** { *; }

# --- Keep Application Entry Points ---
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public void set*(...);
}

# --- Keep Kotlin Specifics ---
-keep class kotlin.coroutines.jvm.internal.DebugMetadataKt
-dontwarn kotlin.Unit
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}
-keepclassmembers class kotlin.coroutines.jvm.internal.BaseContinuationImpl {
    private java.lang.Object[] getSpilledStack();
    private java.lang.String getSpilledStack(int, int);
}

# --- Parcelize ---
# This is the most critical rule for Parcelable data classes.
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}
-keepnames class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# --- Keep ViewModels and their factories ---
-keep class androidx.lifecycle.ViewModel { *; }
-keep class androidx.lifecycle.ViewModelProvider$Factory { *; }
-keep class * extends androidx.lifecycle.ViewModel
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# --- Data classes (used for state and models) ---
# Keep the constructor and all public methods of data classes.
-keepclassmembers public final class **.data.** {
    public <init>(...);
    public final <methods>;
}