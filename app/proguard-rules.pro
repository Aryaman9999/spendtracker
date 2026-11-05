# Add project specific ProGuard rules here.

# Keep Room Database and Entities
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keepclassmembers class * extends androidx.room.RoomDatabase { *; }

# Keep data models used in Room and Reflection
-keep class com.aryaman.spendtracker.data.local.entity.** { *; }
-keep class com.aryaman.spendtracker.data.local.dao.** { *; }
-keep class com.aryaman.spendtracker.data.model.** { *; }
-keep class com.aryaman.spendtracker.data.parser.ParsedTransaction { *; }
-keep class com.aryaman.spendtracker.data.parser.SynthesisResult { *; }
-keep class com.aryaman.spendtracker.data.parser.DetectedSlots { *; }

