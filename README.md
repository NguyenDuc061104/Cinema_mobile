# Web Cinema Mobile Android

Native Android version of the cinema booking project.

This app connects to the standalone Java backend located in:

```text
..\mobile_java_backend
```

## Target Device

| Config | Value |
|---|---|
| Emulator | Small Phone |
| API | 24 |
| Android | 7.0 Nougat |
| ABI | x86 |
| minSdk | 24 |
| targetSdk | 35 |
| compileSdk | 35 |

## Backend URL

Android Emulator cannot access the host machine using `127.0.0.1`.

Use:

```text
http://10.0.2.2:5000
```

## Run Backend First

```powershell
cd ..\mobile_java_backend
run_backend.bat
```

## Implemented Features

- Movie list
- Movie detail
- Showtime selection
- Cinema information
- Ticket type selection
- Seat selection
- Combo selection
- Login
- Signup
- Payment
- Ticket creation
- User profile screen

## API Endpoints

### Movies

```http
GET /movie/movies
GET /movie/movies/{id}
```

### Showtimes

```http
GET /showtime/movies/{id}/showtimes
GET /showtime/{id}/cinema
```

### Tickets

```http
GET /ticket/{showtime_id}/ticket-types
POST /ticket
```

### Seats

```http
GET /seat/{showtime_id}/seats
```

### Combos

```http
GET /combo/api/
```

### Authentication

```http
POST /auth/login
POST /auth/signup
```

### Payment

```http
POST /payment
```

## Android Code Structure

```text
app/src/main/java/com/webcinema/mobile/

├── MainActivity.java
├── config/
│   └── AppConfig.java
├── data/
│   └── SessionStore.java
├── model/
│   └── UserProfile.java
└── ui/
    └── ProfileScreen.java
```

## Build With Android Studio

1. Open project in Android Studio
2. Wait for Gradle Sync
3. Select `app`
4. Run emulator
5. Click Run

## CLI Build

```powershell
cd android_cinema_app

gradlew :app:assembleDebug
```

## APK Output

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Notes

- Cleartext HTTP is enabled for local backend development
- Backend must be running before opening the app
- Native Android Java UI only
- No external UI libraries used

## Troubleshooting

### Gradle Build Running Too Long

Try:

```text
File → Invalidate Caches / Restart
```

Or:

```powershell
gradlew clean
gradlew build
```

### Emulator Cannot Connect To Backend

Correct:

```text
http://10.0.2.2:5000
```

Wrong:

```text
http://127.0.0.1:5000
```

## Future Improvements

- Booking history
- QR ticket
- Dark mode
- Firebase integration
- Push notifications
- JWT authentication
- Material Design UI
- Jetpack Compose migration
