---
description: Install on the connected phone, drive the target screen with adb, screenshot it, and fix bugs
---

Install via `./gradlew installDebug` on my connected phone, clear logcat, open the target screen, test interactive buttons via uiautomator dump & adb shell input, capture a real phone screenshot via `adb exec-out screencap -p > build/live_screen.png`, visually inspect build/live_screen.png for text contrast/visibility and `adb logcat -d *:E` for errors, and fix any bug immediately.

Steps:
1. `adb devices` — confirm exactly one device is connected (stop and tell me if none).
2. `./gradlew installDebug`
3. `adb logcat -c`
4. `adb shell am start -n com.measurepulse.ar/.MainActivity` (plus any route extra for the target screen).
5. `adb shell uiautomator dump /sdcard/ui.xml && adb pull /sdcard/ui.xml build/ui.xml` — find button bounds, then `adb shell input tap X Y` on each interactive element.
6. `adb exec-out screencap -p > build/live_screen.png` — open it with the Read tool and check text contrast/visibility (no light text on lime, nothing clipped behind system bars).
7. `adb logcat -d *:E` — look for crashes/exceptions from com.measurepulse.ar.
8. Fix any bug found, rebuild, and repeat from step 2 until clean.

Helpers: `python -I tools/adb_ui.py dump` lists on-screen text with bounds, `tap "<text>"` taps it, `shot build/x.png` screenshots. Build with `JAVA_HOME` set to Android Studio's JBR and `MSYS_NO_PATHCONV=1` in Git Bash.

Note: on Windows use `adb exec-out screencap -p > build/live_screen.png` from Git Bash (PowerShell 5 corrupts binary redirects).

Target screen: $ARGUMENTS
