# =====================================================================
# Zazili 2.0 — reglas de R8 para la entrega del hackathon.
#
# R8 puede encoger y ofuscar libremente; estas reglas solo protegen lo
# que se resuelve por reflexión, por manifiesto o por JNI en runtime.
# No eliminan comportamiento: cualquier clase que la app necesita para
# arrancar, guardar datos o hablar con Gemini queda intacta.
# =====================================================================

# Metadatos que Kotlin, Compose, Room y las corrutinas consultan en runtime.
-keepattributes Signature,InnerClasses,EnclosingMethod,*Annotation*
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ---------------------------------------------------------------------
# Android: componentes que el sistema instancia por nombre.
# ---------------------------------------------------------------------
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference

-keepclassmembers enum * { *; }

-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ---------------------------------------------------------------------
# Gemini AI SDK (com.google.ai.client.generativeai 0.9.0).
# Serializa y refleja los tipos del protocolo; conservar nombres y
# miembros evita fallos al construir peticiones o leer respuestas.
# ---------------------------------------------------------------------
-keep class com.google.ai.client.generativeai.** { *; }
-keep class com.google.ai.client.generativeai.type.** { *; }
-dontwarn com.google.ai.client.generativeai.**

# ---------------------------------------------------------------------
# Kotlin + corrutinas: los dispatchers y continuations se buscan por
# nombre de clase; los volátiles no pueden ofuscarse sin romper el
# modelo de memoria de CoroutineDispatcher.
# ---------------------------------------------------------------------
-keep class kotlin.Metadata { *; }
-keepclassmembers class **$WhenMappings { <fields>; }
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }
-keepclassmembers class kotlin.coroutines.SafeContinuation { volatile <fields>; }
-dontwarn kotlinx.coroutines.**

# ---------------------------------------------------------------------
# DataStore (Preferences): el delegate de Preferences guarda las llaves
# en un Value<T> que R8 podría agresivamente optimizar entre módulos.
# ---------------------------------------------------------------------
-keep class androidx.datastore.** { *; }
-keepclassmembers class androidx.datastore.preferences.core.** { *; }
-keepnames class androidx.datastore.preferences.core.Preferences$Key
-dontwarn androidx.datastore.**

# ---------------------------------------------------------------------
# Room: el código generado por KSP se instancia por reflexión a través
# de la clase Database_Impl y expone columnas por nombre.
# ---------------------------------------------------------------------
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase { *; }
-dontwarn androidx.room.paging.**

# ---------------------------------------------------------------------
# WorkManager + Glance: el widget de racha se reengancha tras reiniciar
# y WorkManager instancia los workers por nombre de clase.
# ---------------------------------------------------------------------
-keep class * extends androidx.work.ListenableWorker { *; }
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver { *; }
-keep class * extends androidx.glance.appwidget.GlanceAppWidget { *; }

# ---------------------------------------------------------------------
# ONNX Runtime: el binding JNI busca las clases por nombre binario.
# Renombrarlas rompería la síntesis de voz local (modelo_voz.onnx).
# ---------------------------------------------------------------------
-keep class ai.onnxruntime.** { *; }
-dontwarn ai.onnxruntime.**

# ---------------------------------------------------------------------
# Media3/ExoPlayer y OkHttp exponen extensiones opcionales que pueden
# no estar en el classpath; silenciar solo advertencias de empaquetado.
# ---------------------------------------------------------------------
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn com.google.protobuf.**
