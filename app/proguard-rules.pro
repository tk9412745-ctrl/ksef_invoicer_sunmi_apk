# ProGuard rules — release build minify
# JS interface musi zostac — wywolywane refleksyjnie z JavaScript
-keep public class cloud.kasaflow.ksefinvoicer.SunmiBridge {
    public *;
}
-keepclassmembers class cloud.kasaflow.ksefinvoicer.SunmiBridge {
    @android.webkit.JavascriptInterface <methods>;
}

# Sunmi AIDL stub klasy
-keep class woyou.aidlservice.jiuiv5.** { *; }

# Standard WebView
-keepattributes JavascriptInterface
-keep class * extends android.webkit.WebViewClient
-keep class * extends android.webkit.WebChromeClient
