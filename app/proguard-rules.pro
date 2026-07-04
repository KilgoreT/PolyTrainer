# Keep line numbers for stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-keepattributes *Annotation*

# Room — keep all entities, DAOs, and database classes
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep class * extends androidx.room.RoomDatabase$Callback { *; }
-keep class **_Impl { *; }

# Room @Relation and @Embedded
-keepclassmembers class * {
    @androidx.room.Embedded *;
    @androidx.room.Relation *;
}

# Bundled SQLite driver — keep native methods and driver classes
-keep class androidx.sqlite.driver.bundled.** { *; }
-keep class androidx.sqlite.** { native <methods>; }

# Keep all DB entities in core-db-impl
-keep class me.apomazkin.core_db_impl.entity.** { *; }
-keep class me.apomazkin.core_db_impl.room.** { *; }

# Keep all API entities in core-db-api
-keep class me.apomazkin.core_db_api.entity.** { *; }

# Dagger — keep generated components and factories
-keep class **_Factory { *; }
-keep class **_MembersInjector { *; }
-keep class dagger.** { *; }
-keepclassmembers class * {
    @javax.inject.Inject <init>(...);
    @javax.inject.Inject <fields>;
}

# DataStore
-keep class androidx.datastore.** { *; }

# Kotlin serialization (Firebase Sessions)
-keepattributes InnerClasses
-keep,includedescriptorclasses class com.google.firebase.sessions.**$$serializer { *; }
-keepclassmembers class com.google.firebase.sessions.** {
    *** Companion;
}
-keepclasseswithmembers class com.google.firebase.sessions.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# country_data — Gson reflection, нет consumer rules
-keep class com.blongho.country_data.** { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Compose
-keep class androidx.compose.** { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}
