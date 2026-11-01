# Proguard rules for edith AI

# Preserve JavaScript Interfaces used in WebView
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Preserve org.json classes
-keep class org.json.** { *; }

# Preserve OkHttp internals
-dontwarn okhttp3.**
-dontwarn okio.**

# Preserve data models
-keep class com.edith.assistant.data.** { *; }
-keep class com.edith.assistant.voice.** { *; }
-keep class com.edith.assistant.forge.** { *; }
-keep class com.edith.assistant.agent.** { *; }
-keep class com.edith.assistant.license.** { *; }
