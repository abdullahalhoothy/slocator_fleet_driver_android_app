# Keep our model classes (they are simple data classes, but be safe)
-keep class com.slocator.fleetdriver.data.** { *; }

# Compose / Kotlin metadata
-keep class kotlin.Metadata { *; }

# AndroidX preserved by default rules; nothing else needed.
