# SnapCam ProGuard Rules
-keep class com.snapcam.** { *; }
-keep class * extends androidx.room.RoomDatabase
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*
