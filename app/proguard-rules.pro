# Gson uses generic type information stored in a class file when working with fields. Proguard
# removes such information by default, so configure it to keep all of it.
-keepattributes Signature

# For using GSON @Expose annotation
-keepattributes *Annotation*

# Gson specific classes
-dontwarn sun.misc.**
#-keep class com.google.gson.stream.** { *; }

# Application classes that will be serialized/deserialized over Gson
-keep class org.turbo.beaconmqtt.beacon.** { <fields>; }
-keep class org.turbo.beaconmqtt.preferencesConverter.** { <fields>; }

-keepclassmembers,allowobfuscation class org.turbo.beaconmqtt.beaconFactory.BeaconFactory {
  public org.turbo.beaconmqtt.beacon.IBeacon getIBeaconById(java.lang.String);
  public org.turbo.beaconmqtt.beacon.WifiBeacon getWifiBeaconById(java.lang.String);
  public void reloadBeacons();
}
-keepclassmembers,allowobfuscation class org.turbo.beaconmqtt.preferencesConverter.PreferencesConverter {
  public static boolean isCurrentPreferencesRevision(android.content.SharedPreferences);
}

# Prevent proguard from stripping interface information from TypeAdapter, TypeAdapterFactory,
# JsonSerializer, JsonDeserializer instances (so they can be used in @JsonAdapter)
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Prevent R8 from leaving Data object members always null
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

