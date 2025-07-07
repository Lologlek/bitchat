# Preserve Kotlin metadata for reflection
-keep class kotlin.Metadata { *; }

# Keep Jetpack Compose runtime
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Keep app classes
-keep class bitchat.** { *; }
