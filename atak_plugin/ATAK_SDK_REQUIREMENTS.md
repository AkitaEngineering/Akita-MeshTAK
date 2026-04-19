# ATAK SDK Contract

## Purpose

This project intentionally does not ship the proprietary ATAK SDK jar.

## Release Requirement

- Release APK builds require the official ATAK SDK jar.
- Provide its absolute or relative path with `AKITA_ATAK_SDK_JAR` or `-PakitaAtakSdkJar=/path/to/atak-sdk.jar`.
- The expected contract version is recorded in `../version.properties` as `ATAK_SDK_VERSION`.

## CI and Debug Builds

- If the jar is absent, the Gradle build automatically falls back to compile-time ATAK stubs for debug and JVM tests.
- You can force the stub path with `-PakitaUseAtakStub=true`.
- Stub builds are only for local development, CI, and unit testing. They are not valid release artifacts.

## Why This Exists

- It keeps the repository buildable in clean environments.
- It prevents release automation from silently shipping an APK built against placeholders or missing ATAK dependencies.
- It formalizes the separation between reproducible open-source CI and proprietary release packaging.
