# Source provenance

This package was prepared for the WAYReveal application from an internal,
purpose-built Google Places UI Kit compatibility bridge. The accepted source
was the Kotlin 2.3.21 / Places 5.3.0 compatibility candidate that had already
passed isolated Android debug and release builds, R8, native tests, and live
device lifecycle checks before publication preparation.

The public tree contains only the reusable Android plugin implementation,
bounded Dart interface, tests, and package metadata. Internal proof apps,
screenshots, logs, generated build output, machine paths, credentials, and iOS
prototype files are not included.

Parts of the package structure and Dart platform-interface scaffolding began as
output from the Flutter tool's plugin template and were subsequently modified
for WAYReveal. The applicable Flutter notice is reproduced in
`THIRD_PARTY_NOTICES.md`.

No Google Places, AndroidX, or J2ObjC source code is vendored in this
repository. Those components are referenced as external build dependencies by
their published Maven coordinates.

This provenance record is not a software license for the WAYReveal-authored
package.
