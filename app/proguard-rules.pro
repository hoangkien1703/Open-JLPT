# Keep kotlinx.serialization metadata for the question bank models.
-keepattributes *Annotation*, InnerClasses
-keep,includedescriptorclasses class com.openjlpt.core.model.**$$serializer { *; }
-keepclassmembers class com.openjlpt.core.model.** { *** Companion; }
