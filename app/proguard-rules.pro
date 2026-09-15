# kotlinx.serialization: keep generated serializers for our models
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class dev.brown.wordy.** { *** Companion; }
-keepclasseswithmembers class dev.brown.wordy.** { kotlinx.serialization.KSerializer serializer(...); }
