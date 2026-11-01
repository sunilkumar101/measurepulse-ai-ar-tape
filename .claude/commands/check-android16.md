---
description: Audit the project for Android 16 (API 36) + 16KB page size compliance
---

Verify Android 16 (API Level 36) compliance and fix anything missing:

- [ ] `compileSdk = 36`
- [ ] `targetSdk = 36`
- [ ] `ndkVersion = "28.2.13676358"`
- [ ] `packaging { jniLibs { useLegacyPackaging = false } }` (16KB page size alignment)
- [ ] `enableEdgeToEdge()` called in MainActivity, with proper WindowInsets handling (safeDrawing / systemBars padding on every screen)
- [ ] `android:enableOnBackInvokedCallback="true"` on `<application>` in AndroidManifest.xml
- [ ] `<meta-data android:name="com.google.ar.core" android:value="optional" />` and `<uses-feature android:name="android.hardware.camera.ar" android:required="false" />`

Report each item as PASS/FIXED with the file and line.

$ARGUMENTS
