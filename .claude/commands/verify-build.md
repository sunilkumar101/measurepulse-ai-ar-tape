---
description: Build the debug APK and fix any compile/resource/manifest errors
---

Run `./gradlew assembleDebug` (on Windows: `.\gradlew.bat assembleDebug`) from the project root and fix any Kotlin compilation, resource, or manifest errors immediately in the affected files only.

Rules:
- Read the first error in the Gradle output, open only the file it names, fix it, then rebuild. Repeat until `BUILD SUCCESSFUL`.
- Do not refactor unrelated files or change dependency versions unless the error requires it.
- Report which files were changed and the final build result.

$ARGUMENTS
