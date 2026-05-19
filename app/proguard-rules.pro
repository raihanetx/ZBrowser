# Keep JavaScript interface methods
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keepattributes JavascriptInterface

# Only keep the specific classes that need reflection, not the entire package
-keep class com.zbrowser.app.BrowserTab { *; }
-keep class com.zbrowser.app.BrowserViewModel$TabState { *; }

# Remove logging in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# Keep EncryptedSharedPreferences internals
-keep class androidx.security.crypto.** { *; }
