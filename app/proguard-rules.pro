# Count & Play — R8/ProGuard rules.
#
# This app uses no reflection, serialization, or JNI. It relies only on the
# Android framework (TextToSpeech) and Jetpack Compose, both of which bundle
# their own consumer keep rules. So no custom rules are required today.
#
# Add app-specific -keep rules here if you ever introduce reflection,
# Gson/Moshi models, or native (JNI) code.
