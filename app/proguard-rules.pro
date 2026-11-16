# Keep Firestore model classes and fields for reflection-based deserialization
-keepattributes Signature,InnerClasses,EnclosingMethod
-keepclassmembers class com.example.model.** {
    *;
}
-keepnames class com.example.model.**

# Preserve line numbers for crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
