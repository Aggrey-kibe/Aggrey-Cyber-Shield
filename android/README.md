# AGGREY SHIELD — Android Application

**Brand:** AGGREY CYBER SHIELD
**Developer:** Aggrey Kibe Kwamboka — Kenya
**Package:** `com.aggreyshield.app`
**Email:** aggreykwamboka62@protonmail.com · **GitHub:** github.com/Aggrey-kibe · **Website:** aggreykibe.blogspot.com

A complete, modern Android client for the AGGREY SHIELD cybersecurity, fraud-detection and
digital-safety platform. It talks to the FastAPI backend (`/api/v1`) built in earlier phases
of this project over HTTPS, and never fabricates a scan result, security score, incident, or
alert — everything shown comes from a real backend response.

---

## Verification status — read this first

This project was generated and statically verified in a sandboxed environment **without**
network access to Google's Maven repository, Maven Central, or the Gradle distribution
service. That means:

- **I did not run `gradlew assembleDebug`, `gradlew test`, or open this project in Android
  Studio.** No Gradle build, no Kotlin compiler with the real Android/Compose/Hilt classpath,
  and no emulator were available here. I am not claiming otherwise.
- **What I did verify**, on all 172 Kotlin files (154 main, 15 unit test, 3 instrumented test):
  every file has balanced braces/parentheses; every `import com.aggreyshield.app.*` resolves to
  a real declaration; there are no duplicate class/object declarations; no `TODO`, `...` used as
  a stub, or placeholder text anywhere in the source; every `libs.xxx` reference in both
  `build.gradle.kts` files resolves to a real entry in `gradle/libs.versions.toml` and every
  `version.ref` in that catalog resolves; every resource reference (`@string`, `@drawable`,
  `@xml`, `@mipmap`, `@color`, `@style`) resolves to a real resource; every constructor call in
  the test suite matches the real class's constructor exactly; the Gradle wrapper jar was
  downloaded for real from Gradle's GitHub mirror and its SHA-256 is recorded below.
- Gradle/AGP/Kotlin/Compose/Hilt version numbers were checked against Google's and JetBrains'
  published compatibility tables (see the **Versions** section) rather than guessed.

In short: **the code is complete, internally consistent, and carefully reviewed, but it has
only been statically checked — not compiled or run.** The first thing to do is the build in
the **Build the app** section below, which will surface anything this review could not catch.

---

## What's included

- 30 screens (splash through logout confirmation) in Jetpack Compose + Material 3
- MVVM throughout: `ViewModel` + `StateFlow` UI state + a one-shot event channel for
  navigation/snackbars, Hilt for dependency injection
- Retrofit + OkHttp + kotlinx.serialization networking, with a dedicated unauthenticated
  client for login/register/refresh and a separate authenticated client with automatic
  401 → refresh → retry
- Access and refresh tokens encrypted with an AES-256-GCM key held in the Android Keystore
  (see **Security notes**)
- A clean `data` / `domain` / `presentation` architecture so backend endpoints that don't
  exist yet can be wired up later without touching the UI
- 7 JVM unit test classes and 1 instrumented Compose UI test class, using hand-written fakes
  (no Mockito/MockK dependency to go wrong)

## What is *not* included

- A running backend. Every screen expects the AGGREY SHIELD FastAPI backend's `/api/v1`
  endpoints; anything beyond `/api/v1/health` (built in Phase 1) is **REQUIRES BACKEND
  IMPLEMENTATION** — the Android/Retrofit side is complete and ready to connect the moment
  each endpoint exists (see **API status** below).
- A production keystore, a production server URL, or any real secret. You supply these.
- QR-code rendering for MFA setup (the setup key and `otpauth://` URI are shown as text —
  adding a QR image is a good follow-up, not required for the feature to work).

---

## Architecture

```
app/src/main/java/com/aggreyshield/app/
├── core/
│   ├── common/       ApiResult, AppError, Loadable, InputValidator, OneShotEvent, date/time utils
│   ├── network/      Retrofit/OkHttp setup, interceptors, error mapping, connectivity
│   ├── security/     Keystore-backed token storage, SessionManager
│   └── ui/            theme (Color/Type/Shape) and shared composables (buttons, fields, badges, states)
├── data/
│   ├── api/          Retrofit service interfaces (one per backend resource group)
│   ├── model/        DTOs + mappers to domain models (nothing here is UI-visible directly)
│   ├── local/         DataStore-backed settings, evidence-file reading
│   └── repository/    Repository implementations (DTO ↔ domain, error handling)
├── domain/
│   ├── model/         Domain models the UI actually uses (Incident, ScanResult, User, ...)
│   └── repository/    Repository interfaces (what ViewModels depend on)
├── di/                 Hilt modules (network, repositories, app-scoped coroutine scope)
├── navigation/         Routes, the NavHost, session-driven navigation, bottom nav
└── presentation/        One package per feature area (auth, dashboard, scanner, incidents,
                          alerts, threatintel, training, notifications, profile, organization,
                          settings, about, logout), each with Screen + ViewModel pairs
```

ViewModels are constructed at the `NavHost` call site (`navigation/AppNavHost.kt`) and passed
into screens as a parameter, rather than each screen calling `hiltViewModel()` internally. This
keeps screens easy to preview and unit-test without any Hilt test infrastructure — see
`LoginScreenTest.kt` for an example that renders a real screen with a hand-built ViewModel.

---

## Requirements

| Tool | Version | Notes |
|---|---|---|
| Windows | 10 or 11 | |
| JDK | 17 | Android Studio bundles a compatible JDK automatically. Only install one yourself if you plan to run `gradlew` from PowerShell **without** opening Android Studio first. |
| Android Studio | Ladybug (2024.2.1) or newer | Any newer stable release also works — AGP 8.7.x's compatibility window covers all of them. |
| Android SDK | Platform 35, Build-Tools 34.0.0+ | Installed via Android Studio's SDK Manager on first project sync. |

### Versions pinned in this project

| Component | Version | Why |
|---|---|---|
| Gradle | 8.10.2 | AGP 8.7.x requires Gradle ≥ 8.9 |
| Android Gradle Plugin | 8.7.3 | Current stable line at time of writing; supports Android Studio Ladybug and newer |
| Kotlin | 2.0.21 | Paired Kotlin release for this AGP generation |
| Compose BOM | 2024.10.01 | Pins compatible versions of all Compose artifacts together |
| Hilt | 2.52 | |
| minSdk / targetSdk / compileSdk | 26 / 35 / 35 | Android 8.0+ devices |

Gradle wrapper jar SHA-256 (for your own verification):
`2db75c40782f5e8ba1fc278a5574bab070adccb2d21ca5a6e5ed840888448046`

---

## Extract and open the project

```powershell
# From the folder where you downloaded aggrey-shield-android.zip
Expand-Archive -Path .\aggrey-shield-android.zip -DestinationPath .\aggrey-shield-android
cd .\aggrey-shield-android
```

**Open in Android Studio:**
- Easiest: open Android Studio, choose **File → Open**, and select the extracted
  `aggrey-shield-android` folder.
- From PowerShell, if Android Studio is installed at the default location:
  ```powershell
  & "C:\Program Files\Android\Android Studio\bin\studio64.exe" .
  ```
  (Adjust the path if you installed Android Studio somewhere else, or if it was installed
  per-user under `%LOCALAPPDATA%\Programs\Android Studio\bin\studio64.exe`.)

Android Studio will detect `local.properties.example`... actually it won't — **you** create
`local.properties` (see next section), and Android Studio itself writes `sdk.dir` into it
automatically the first time you open the project, alongside the `API_BASE_URL_*` lines you add.

**Sync Gradle:** Android Studio syncs automatically after opening (watch the notification bar:
"Gradle files have changed... Sync Now"). To trigger dependency resolution from PowerShell
instead of the IDE:
```powershell
.\gradlew.bat tasks
```

---

## Configure the backend connection

Copy the example files (both are git-ignored — never commit your real values):

```powershell
Copy-Item .\local.properties.example .\local.properties
Copy-Item .\keystore.properties.example .\keystore.properties   # only needed for release builds, see below
```

Open `local.properties` and set the debug backend URL. **Android Emulator vs. a physical
phone need different addresses** — this trips up almost everyone the first time:

| Where the backend runs | Where the app runs | URL to use |
|---|---|---|
| Your Windows computer (`localhost:8000`) | Android **Emulator** | `http://10.0.2.2:8000/` — `10.0.2.2` is a special alias the emulator maps to your computer's `localhost`. Using `http://localhost:8000/` here points the *emulator itself* at itself, not your computer, and will fail. |
| Your Windows computer (`localhost:8000`) | A **physical phone** on the same Wi-Fi | `http://<your-computer's-LAN-IP>:8000/` — for example `http://192.168.1.50:8000/`. Find your IP with `ipconfig` (look for "IPv4 Address" under your Wi-Fi adapter). Your phone cannot reach `10.0.2.2` or `localhost` — those mean nothing on a physical device. Also make sure your backend is started with `--host 0.0.0.0`, not `--host 127.0.0.1`, or it won't accept connections from other devices, and check Windows Firewall isn't blocking port 8000. |
| A real server (staging/production) | Either | `https://your-domain.example.com/` — must be HTTPS; release and staging builds block plain HTTP (see **Security notes**). |

```
# local.properties
API_BASE_URL_DEBUG=http://10.0.2.2:8000/
```

You can also change the server address at runtime from **Settings** inside the app (debug
builds only) — useful for switching between emulator and a physical phone without rebuilding.

---

## Build the app

All commands below are run from the extracted `aggrey-shield-android` folder in PowerShell.

**Build a debug APK:**
```powershell
.\gradlew.bat assembleDebug
```
Output: `app\build\outputs\apk\debug\app-debug.apk`

**Run the unit tests:**
```powershell
.\gradlew.bat test
```
Reports: `app\build\reports\tests\testDebugUnitTest\index.html`

**Run the instrumented UI test** (needs a running emulator or connected device):
```powershell
.\gradlew.bat connectedAndroidTest
```
Reports: `app\build\reports\androidTests\connected\index.html`

**Build everything and check for lint/compile issues without installing anything:**
```powershell
.\gradlew.bat build
```

---

## Install the debug APK on a phone

With a phone connected via USB (USB debugging enabled in Developer Options) or a running
emulator, the simplest route is letting Gradle handle `adb` for you:

```powershell
.\gradlew.bat installDebug
```

Or, if you have Android SDK platform-tools on your PATH:
```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

The debug build has application ID `com.aggreyshield.app.debug`, so it installs side-by-side
with any release build without conflicting.

---

## Build a release APK

A release build needs a signing key. **Never commit a real keystore or real passwords.**

**1. Generate a keystore** (one time; `keytool` ships with the JDK):
```powershell
keytool -genkeypair -v -keystore aggrey-shield-release.jks -alias aggreyshield -keyalg RSA -keysize 2048 -validity 10000
```
You'll be prompted for a store password, a key password, and your name/organization details.
Move the resulting `aggrey-shield-release.jks` into the project root (next to `settings.gradle.kts`).

**2. Fill in `keystore.properties`** (copied earlier from `keystore.properties.example`):
```
storeFile=aggrey-shield-release.jks
storePassword=<the password you just set>
keyAlias=aggreyshield
keyPassword=<the password you just set>
```
This file, and the `.jks` file, are listed in `.gitignore`. Keep both somewhere safe outside
Git as well — if you lose the keystore you cannot publish an update to an app already
installed from it.

**3. Set the production backend URL** in `local.properties`:
```
API_BASE_URL_RELEASE=https://api.yourdomain.example.com/
```

**4. Build:**
```powershell
.\gradlew.bat assembleRelease
```
Output: `app\build\outputs\apk\release\app-release.apk`

If `keystore.properties` does not exist, this command still succeeds, but the build script
(`app/build.gradle.kts`) deliberately leaves the release build **unsigned** rather than
falling back to a dummy or debug key — you'll get `app-release-unsigned.apk`, which Android
will refuse to install until it's properly signed. This is intentional: it stops an
accidentally-unsigned or wrongly-signed release build from looking like a normal, installable
APK.

---

## Testing

| Command | What it runs |
|---|---|
| `.\gradlew.bat test` | All JVM unit tests (`app/src/test`) — validators, error mapping, session state, and ViewModel logic, using hand-written fakes instead of a mocking framework |
| `.\gradlew.bat connectedAndroidTest` | Instrumented Compose UI tests (`app/src/androidTest`) — needs an emulator or device |
| `.\gradlew.bat testDebugUnitTest --tests "com.aggreyshield.app.core.common.InputValidatorTest"` | One test class |

Notable test files:
- `core/common/InputValidatorTest.kt` — email/password/URL validation rules
- `core/network/ErrorMapperTest.kt` — HTTP status → user-facing error mapping (401/403/404/422/429/5xx)
- `core/security/SessionManagerTest.kt` — session state transitions, the "session expired" notice
- `data/repository/AuthRepositoryImplTest.kt` — the app **never** starts a session unless the
  backend actually returned a refresh token
- `presentation/auth/LoginViewModelTest.kt` — form validation, the MFA-required flow
- `presentation/scanner/UrlScannerViewModelTest.kt` — a failed scan never fabricates a result
- `presentation/incidents/IncidentReportViewModelTest.kt` — form validation, evidence file rules
- `androidTest/presentation/auth/LoginScreenTest.kt` — renders the real `LoginScreen` and
  drives it with Compose UI test APIs

---

## API status

The backend's `/api/v1/health` endpoint (built in the earlier backend phases) is the only one
this app calls that is confirmed to exist. Every other endpoint the app calls (auth, users,
scanning, incidents, alerts, threat intel, training, notifications) is **REQUIRES BACKEND
IMPLEMENTATION** — the full request/response contract is already defined on the Android side
in `data/api/*.kt` and `data/model/*.kt`, so connecting a real endpoint later means implementing
it on the FastAPI backend to match that contract; no Android code should need to change.
`core/network/ApiEndpoints.kt` lists every path with its status.

Test the connection any time from the app's **Settings** screen ("Test connection"), which
calls the real `/api/v1/health` endpoint and shows whether the API and database are reachable.

---

## Security notes

- **Tokens are never stored in plain SharedPreferences.** `SecureTokenStorage` encrypts the
  access and refresh tokens with AES-256-GCM using a key generated inside, and never exported
  from, the Android Keystore (`KeystoreCipher.kt`). The SharedPreferences file backing it only
  ever contains ciphertext.
- **Cleartext HTTP is blocked outside of debug builds.** `network_security_config.xml` (main)
  sets `cleartextTrafficPermitted="false"`; only the debug variant's override
  (`src/debug/res/xml/network_security_config.xml`) allows plain `http://` so you can reach a
  backend running on your own computer during development. Release and staging builds require
  HTTPS.
- **TLS verification is never disabled.** There is no trust-all `X509TrustManager`, no
  `HostnameVerifier` override, and no `SSLSocketFactory` bypass anywhere in this codebase.
- **Certificate pinning** is not configured, because it needs your actual production
  certificate, which doesn't exist yet. To add it later: put an OkHttp `CertificatePinner` (or
  a `<pin-set>` block in `network_security_config.xml`) in front of your real production
  domain, then rotate pins before your certificate expires or renews.
- **Login never "succeeds" without a refresh token.** If the backend responds to `/auth/login`
  without a refresh token, the app treats it as an invalid response and does not start a
  session — see `AuthRepositoryImplTest.kt`.
- **401 handling** automatically refreshes the access token once and retries the original
  request (`TokenAuthenticator.kt`); if the refresh token itself is rejected, the session ends
  and the user is returned to Login with a "session expired" message — never silently left in
  a broken logged-in state.
- **Logging never includes tokens, passwords, or request/response bodies.** The OkHttp logging
  interceptor is set to `Level.BASIC` (method, URL, status, duration only) and is disabled
  entirely in production builds.
- **R8/ProGuard** is enabled for release builds (`isMinifyEnabled = true`), and release-only
  rules strip `Log.v`/`Log.d`/`Log.i` calls from the compiled release APK.
- **Permissions requested:** `INTERNET` (required to call the backend) and
  `ACCESS_NETWORK_STATE` (required only to detect "no internet connection" and show a clear
  message instead of a confusing network error). No contacts, SMS, camera, microphone, or
  location permission is requested anywhere, because no feature in this app needs them.
- **Evidence file uploads** go through the system document picker (`ActivityResultContracts.OpenDocument`),
  which needs no storage permission — the system grants temporary read access to exactly the
  file the user picks. Files are checked against a size limit (10 MB) and an allow-list of MIME
  types (`EvidenceRules.kt`) before upload.
- **`android:allowBackup="false"`** in the manifest, so Android's automatic backup mechanism
  never copies the app's (encrypted) local storage off the device.

---

## Troubleshooting

- **"SDK location not found"** — Android Studio writes `sdk.dir` into `local.properties`
  automatically on first sync. If you're building from PowerShell only, without ever opening
  Android Studio, add it yourself: `sdk.dir=C\:\\Users\\YOUR_NAME\\AppData\\Local\\Android\\Sdk`.
- **Gradle sync fails on first open** — this needs internet access to Google's Maven repo and
  Maven Central (`google()`, `mavenCentral()` in `settings.gradle.kts`) to download AGP,
  Kotlin, AndroidX and Hilt. Corporate proxies/firewalls sometimes block these; check your
  network settings if sync hangs or fails immediately.
- **`app-release-unsigned.apk` instead of `app-release.apk`** — `keystore.properties` is
  missing or incomplete. See **Build a release APK** above.
- **App can't reach the backend on a physical phone** — almost always the URL. Re-check the
  emulator-vs-phone table above: a phone can never use `10.0.2.2` or `localhost`; it needs your
  computer's actual LAN IP, both devices on the same Wi-Fi network, the backend started with
  `--host 0.0.0.0`, and Windows Firewall allowing inbound connections on port 8000.
- **`/api/v1/health` works but every other screen shows an error** — expected until the
  corresponding backend endpoint exists. See **API status** above.
- **"Session expired" appears right after logging in successfully** — check the backend's
  `/auth/login` response actually includes a `refresh_token` field; see `AuthRepositoryImpl.kt`.
- **Build fails with a Kotlin/Compose version mismatch after you changed a dependency** — the
  Compose BOM (`libs.androidx.compose.bom` in `gradle/libs.versions.toml`) is what keeps every
  Compose artifact's version aligned; don't pin an individual Compose library's version
  separately unless you also update the BOM.

---

## Project layout (top level)

```
aggrey-shield-android/
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/        (154 Kotlin files, resources, manifest — see Architecture above)
│       ├── debug/        network_security_config.xml override (cleartext allowed for local dev)
│       ├── test/         15 Kotlin files — JVM unit tests and fakes
│       └── androidTest/  3 Kotlin files — instrumented Compose UI test and fakes
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradlew
├── gradlew.bat
├── .gitignore
├── local.properties.example
├── keystore.properties.example
└── README.md
```
