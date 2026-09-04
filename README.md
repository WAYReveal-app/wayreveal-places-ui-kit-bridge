# WAYReveal Places UI Kit bridge

Android Flutter plugin for hosting Google Places UI Kit in a
`FragmentActivity`-backed Flutter application.

## Supported platform

- Android API 24 or newer.
- Kotlin Gradle Plugin 2.3.21 is owned by the host application.
- The plugin compiles its Kotlin sources for JVM 17.
- Native dependencies are Google Places 5.3.0, J2ObjC annotations 3.0.0,
  and AndroidX Fragment KTX 1.8.9.

The package does not declare iOS support. It does not configure credentials,
store a Google API key, or require a key to resolve or compile as a dependency.
The consuming application supplies its restricted Android client key through
the `com.wayreveal.PLACES_API_KEY` manifest metadata contract at runtime.

## Host integration

Use `WayRevealFlutterFragmentActivity` as the Android host activity when the
embedded Places surface is needed. The host app remains responsible for its
Kotlin, Android SDK, signing, environment, and credential configuration.

`PlacesUiKitEmbeddedProbe` exposes a backward-compatible selection callback and
an optional Place ID callback. Native event payloads are intentionally bounded
to loading/error state, selection state, Place ID, latitude, and longitude.
Google-rendered rich place content remains inside the Google-rendered UI.

## Identity boundary

A Google Place ID is an optional external reference. It does not replace
WAYReveal's stable provider identity.

## Development checks

```text
flutter pub get
flutter test
flutter analyze
```

This repository is distributed without a project-level software license. No
permission beyond applicable law and explicit written authorization is granted.
Source lineage and required third-party attribution are documented in
`PROVENANCE.md` and `THIRD_PARTY_NOTICES.md`.
