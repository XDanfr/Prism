# Prism Component Definitions

This document provides a summary of the core classes, managers, and data models that form the architecture of the Prism WebAPK generator.

## Compiler & Binary Tools

### [BinaryCompilerEngine](file:///home/dan/AndroidStudioProjects/Prism/app/src/main/java/me/xdan/prism/compiler/BinaryCompilerEngine.kt)
The orchestrator of the APK modification pipeline. It coordinates the extraction of the base APK, triggers the binary editors for manifest and resource patching, invokes the icon pipeline, and finally performs zipalign and signing operations.

### [BinaryXmlEditor](file:///home/dan/AndroidStudioProjects/Prism/app/src/main/java/me/xdan/prism/compiler/BinaryXmlEditor.kt)
A specialized tool for patching Android's binary XML (AXML) format. Its primary responsibility is to modify the string pool of `AndroidManifest.xml` to update the package name and other configuration strings without requiring a full decompile/recompile cycle.

### [ResTableEditor](file:///home/dan/AndroidStudioProjects/Prism/app/src/main/java/me/xdan/prism/compiler/ResTableEditor.kt)
Manages the `resources.arsc` binary file. It ensures that the package name defined in the resource table matches the new package name set in the manifest, maintaining resource resolution integrity.

### [ZipAligner](file:///home/dan/AndroidStudioProjects/Prism/app/src/main/java/me/xdan/prism/compiler/ZipAligner.kt)
A custom implementation of the `zipalign` tool. It processes the unaligned APK and ensures that all uncompressed data (such as `resources.arsc` and PNG files) starts on a 4-byte boundary, which is a requirement for efficient memory-mapped access by the Android OS.

### [ApkSignerHelper](file:///home/dan/AndroidStudioProjects/Prism/app/src/main/java/me/xdan/prism/compiler/ApkSignerHelper.kt)
Handles the cryptographic signing of the generated APK. It uses BouncyCastle to generate a self-signed RSA key pair and certificate, then applies v1, v2, and v3 signatures using the `apksig` library to ensure the APK is installable on all supported Android versions.

### [IconPipeline](file:///home/dan/AndroidStudioProjects/Prism/app/src/main/java/me/xdan/prism/compiler/IconPipeline.kt)
A multi-stage image processing engine that generates a complete set of Android icons. It can fetch favicons from URLs, render SVGs, or process PNGs to create legacy mipmaps, adaptive foreground/background layers, and monochromatic versions for themed icons (Android 13+).

---

## Application Logic & UI

### [InstallManager](file:///home/dan/AndroidStudioProjects/Prism/app/src/main/java/me/xdan/prism/util/InstallManager.kt)
A utility class responsible for handing off the final APK to the system installer. It uses `FileProvider` to generate secure Uris and triggers `Intent.ACTION_VIEW` with the appropriate MIME type.

### [CreateAppViewModel](file:///home/dan/AndroidStudioProjects/Prism/app/src/main/java/me/xdan/prism/ui/CreateAppViewModel.kt)
The core ViewModel for the app creation flow. It manages the `CompilationProgress` state, handles the conversion of UI inputs into compiler configurations, and maintains the background coroutine that runs the compilation engine.

### [Routes & Navigation](file:///home/dan/AndroidStudioProjects/Prism/app/src/main/java/me/xdan/prism/navigation/Routes.kt)
Prism uses **Jetpack Navigation 3** for type-safe routing.
- `Dashboard`: The entry screen for user input.
- `Progress`: A state-aware screen that displays the current compilation step.
- `Result`: Shows the outcome (success/failure) and provides actions to install the APK or retry.

---

## Data Models

### [AppConfig](file:///home/dan/AndroidStudioProjects/Prism/app/src/main/java/me/xdan/prism/model/AppConfig.kt)
The primary state holder for user-defined app parameters, including the target URL, app name, icon source, accent color, and navigation mode.

### [NavigationMode](file:///home/dan/AndroidStudioProjects/Prism/app/src/main/java/me/xdan/prism/model/NavigationMode.kt)
An enum defining how the generated WebAPK will display the target website: either via a dedicated `WebView` or using `Chrome Custom Tabs`.
