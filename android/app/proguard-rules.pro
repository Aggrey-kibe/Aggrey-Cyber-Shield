# AGGREY SHIELD release (R8) rules.
# Retrofit, OkHttp, Hilt and kotlinx.serialization ship their own consumer rules.
# These extra rules only add safety nets and strip verbose logging from release builds.

# Keep generic signatures and annotations Retrofit needs for reflection.
-keepattributes Signature, InnerClasses, EnclosingMethod, RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations, AnnotationDefault

# kotlinx.serialization: keep generated serializers for our DTOs.
-keepclassmembers class com.aggreyshield.app.data.model.** {
    *** Companion;
}
-keepclasseswithmembers class com.aggreyshield.app.data.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers class com.aggreyshield.app.core.network.** {
    *** Companion;
}
-keepclasseswithmembers class com.aggreyshield.app.core.network.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# OkHttp optional dependencies that are not present on Android.
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Remove debug/verbose/info logging from release builds.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
