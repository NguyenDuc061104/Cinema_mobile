# Web Cinema Mobile Android

Native Android version of the cinema booking project. It runs with the standalone Java backend in ..\mobile_java_backend.

## Target device

- Emulator: Small Phone
- API: 24
- Android: 7.0 Nougat
- ABI: x86
- App config: `minSdk 24`, `targetSdk 35`, `compileSdk 35`

## Backend URL

The Android emulator cannot call the host machine through `127.0.0.1`.
This app uses:

```text
http://10.0.2.2:5000
```

Run the Java backend first:

```powershell
cd ..\mobile_java_backend
run_backend.bat
```

## Implemented mobile screens

- Movie list from `GET /movie/movies`
- Movie detail from `GET /movie/movies/{id}`
- Showtime selection from `GET /showtime/movies/{id}/showtimes`
- Cinema info from `GET /showtime/{id}/cinema`
- Ticket types from `GET /ticket/{showtime_id}/ticket-types`
- Seat selection from `GET /seat/{showtime_id}/seats`
- Combos from `GET /combo/api/`
- Login and signup from `/auth/login`, `/auth/signup`
- Payment and ticket creation from `POST /payment`, `POST /ticket`

## Build

Open this folder in Android Studio, then run the `app` configuration.

CLI build used for verification:

```powershell
cd android_cinema_app
& "C:\Users\Windows\.gradle\wrapper\dists\gradle-9.0.0-bin\d6wjpkvcgsg3oed0qlfss3wgl\gradle-9.0.0\bin\gradle.bat" :app:assembleDebug
```

The debug APK is generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Notes

- Cleartext HTTP is enabled because the local Java backend runs over `http`.
- No external Android UI libraries are used, so the project can build with the standard Android Gradle Plugin and SDK.
## Current Android code structure

```text
app/src/main/java/com/webcinema/mobile/
  MainActivity.java          Main navigation and booking flow
  config/AppConfig.java      Backend URL/config values
  data/SessionStore.java     Login session persistence
  model/UserProfile.java     Customer profile response model
  ui/ProfileScreen.java      Customer profile UI renderer
```

Profile entry: sign in, then tap the username button in the top bar.
