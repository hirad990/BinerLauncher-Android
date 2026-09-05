# BinerLauncher Android

A native Android launcher foundation for Minecraft: Java Edition, built from scratch for the BinerCraft ecosystem.

## Vision

BinerLauncher-Android is intended to grow into a full Android Java Edition launcher with:

- Minecraft version management
- Isolated game instances
- Java runtime management
- Fabric / Forge support
- Offline/local profiles first, with account integrations added later
- Mod and resource-pack management
- Touch controls and controller support
- Native LWJGL/GLFW integration
- BinerCraft server integration
- Crash logs and diagnostics
- A polished Persian RTL-friendly UI

## Current status

**Phase 0 — project foundation**

The repository starts as a clean native Android project. The Minecraft runtime layer is deliberately separated from the UI so native/runtime components can be integrated incrementally instead of turning the launcher into a fragile monolith.

## Architecture

```text
app/                 Android application and UI
launcher-core/       Platform-independent launcher models/services
runtime/             Java runtime and process abstractions
minecraft/           Version metadata, libraries, assets and launch planning
native/               Future JNI/LWJGL/GLFW integration
```

## Technology

- Kotlin
- Android Gradle Plugin
- Jetpack Compose / Material 3
- Kotlin Coroutines
- AndroidX

## Roadmap

1. Native Android shell and launcher UI
2. Version metadata and local storage
3. Java runtime detection/management
4. Minecraft asset/library/version downloader
5. Launch command builder and process runner
6. Native graphics/input bridge
7. Fabric/Forge installation
8. Accounts and multiplayer integrations
9. Touch controls, controller mapping and performance profiles
10. Release builds and update infrastructure

## Licensing

BinerLauncher code is developed as an independent project. Any future integration of third-party launcher/runtime components will preserve their respective licenses and notices.
