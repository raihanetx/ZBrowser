# Keep JavaScript interface methods
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keepattributes JavascriptInterface

# Only keep the specific classes that are accessed by name/reflection
-keep class com.zbrowser.app.BrowserTab { *; }
-keep class com.zbrowser.app.BrowserTab$Snapshot { *; }
