# Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keepclassmembernames interface * {
    @retrofit2.http.* <methods>;
}

# Gson
-keepattributes Signature
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# App models & DTOs (keep all fields for serialization/deserialization)
-keep class com.empowermom.app.feature.messageboard.model.** { *; }
-keep class com.empowermom.app.core.data.** { *; }
-keep class com.empowermom.app.core.data.remote.dto.** { *; }
-keep class com.empowermom.app.core.network.** { *; }

# Kotlin Serialization concrete DTOs — preserve serializer, Companion, all members
-keep,includedescriptorclasses class com.empowermom.app.core.data.remote.dto.**$$serializer { *; }
-keepclassmembers class com.empowermom.app.core.data.remote.dto.** {
    *** Companion;
    *** $$serializer;
    kotlinx.serialization.KSerializer serializer(...);
}

# ====================================================================
# Supabase & Ktor
# ====================================================================

# Keep all Supabase classes
-keep class io.github.jan.supabase.** { *; }

# Keep all Ktor classes
-keep class io.ktor.** { *; }

# Kotlin Serialization
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class * {
    *** Companion;
}
-keepclassmembers class * {
    *** $$serializer;
}
-keepclassmembers @kotlinx.serialization.Serializable class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable

# Ktor uses META-INF/services to load the engine — keep resource files
-adaptresourcefilenames META-INF/services/*

# Ktor OkHttp engine service loader entry
-keep class io.ktor.client.engine.okhttp.OkHttpEngineContainer
-keep class io.ktor.client.engine.cio.CIOEngineContainer

# Ktor plugin/service loader
-keep class io.ktor.client.HttpClientEngineContainer
-keep class io.ktor.serialization.kotlinx.** { *; }

# Kotlinx coroutines (Supabase uses heavily)
-keep class kotlinx.coroutines.** { *; }

# Ktor client content negotiation / serialization plugin
-keep class io.ktor.client.plugins.contentnegotiation.** { *; }
-keep class io.ktor.serialization.kotlinx.** { *; }

-dontwarn kotlinx.atomicfu.**
-dontwarn io.netty.**
-dontwarn org.slf4j.**
-dontwarn org.codehaus.mojo.**
-dontwarn reactor.blockhound.**
-dontwarn java.lang.management.**

# Suppress warnings for optional dependencies
-dontwarn org.eclipse.jetty.**
-dontwarn io.netty.channel.**
-dontwarn io.netty.handler.codec.**
-dontwarn org.reactivestreams.**
