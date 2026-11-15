# The JNI functions in libjetlink.so are found by name.
-keep class io.zoompilot.jetlink.server.Native { *; }
-keep class io.zoompilot.jetlink.server.NnapiInventory { *; }
# kotlinx.serialization keeps what its plugin generates.
-keepattributes *Annotation*, InnerClasses
