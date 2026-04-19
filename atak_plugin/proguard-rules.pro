# Keep the plugin entrypoints and services that ATAK reflects by name.
-keep class com.akitaengineering.meshtak.AkitaMeshTAKPlugin { *; }
-keep class com.akitaengineering.meshtak.services.** { *; }
-keep class com.akitaengineering.meshtak.ui.** { *; }

# Preserve ATAK API surface symbols when the official SDK jar is supplied.
-keep class com.atakmap.** { *; }

