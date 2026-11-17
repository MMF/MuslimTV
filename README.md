# Muslim TV

Android TV / Google TV app: Quran, azkar, prayer times and tools for the family living room.
Kotlin, Jetpack Compose for TV, Hilt, DataStore (Room and Media3 from Phase 2). Offline first.

## Build and run

```sh
./gradlew assembleDebug                      # debug APK
./gradlew assembleRelease                    # minified APK (signed with the debug key for local testing)
./gradlew testDebugUnitTest lintDebug        # unit tests + lint
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.mfouad.muslimtv/.MainActivity
adb shell pm clear com.mfouad.muslimtv       # reset to first-launch setup
```

Toolchain: AGP 9.2.1 (built-in Kotlin 2.2.10), Gradle 9.4.1, JDK 17 to compile, JDK 21 to run
unit tests (the Adhan library ships Java 21 bytecode). The foojay plugin downloads missing JDKs.

Use the emulator with host GPU (`emulator -avd Television_1080p -gpu host`); with GPU
emulation every frame takes about a second.

## Modules

```
app                 single activity, navigation shell (rail + NavHost), Back rules, idle → ambient
core/model          plain Kotlin models + pure logic (alert planner, tahfeez engine, khatmah, tasbeeh)
core/common         dispatchers, time ticker, network monitor
core/designsystem   colors, type, FocusCard, buttons, rows, rail, tabs, dialogs, nature scenes, focus restore
core/ui             shared strings, labels, time/date formatting, language switch, city picker
core/datastore      settings (Preferences DataStore)
core/database       Room: Quran text, notes, bookmarks, positions, downloads, tahfeez, profiles, khatmah
core/data           repositories: settings, cities, Quran, audio sources, timings, azkar, hadith, tools
core/media          playback service (Media3 session), Quran player, play cache, ambient sounds
core/downloads      download queue (WorkManager), size estimate, storage guard
feature/onboarding  first-launch setup
feature/home        dashboard, ambient screen
feature/quran       browse, reader, listening, nature mode, tahfeez, khatmah
feature/azkar       categories, zikr pager, tasbeeh, Asma ul-Husna, ruqyah
feature/prayer      today, month, adhan alarms/screen/service, reminders
feature/downloads   download manager and storage
feature/tools       Hijri calendar, qibla, hadith, live and radio, kids, Hajj and Umrah
feature/settings    settings, profiles, credits
```

See `CREDITS.md` for every bundled source and its license.
