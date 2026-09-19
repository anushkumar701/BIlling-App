# Proguard rules for Fruit Billing app
-keepclassmembers class com.fruitbilling.app.data.model.** { *; }
-keep class com.fruitbilling.app.data.model.** { *; }
-keep class com.fruitbilling.app.data.db.** { *; }
-keep class com.fruitbilling.app.data.backup.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Google Sign-In & Play Services Auth
-keep class com.google.android.gms.auth.api.signin.** { *; }
-keep class com.google.android.gms.common.** { *; }
-dontwarn com.google.android.gms.**
