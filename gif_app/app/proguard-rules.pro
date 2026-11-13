# 保留 Retrofit / Gson / OkHttp 相关类，避免混淆导致反射失败
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keep class com.gifapp.car.data.** { *; }