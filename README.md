# A Little App That Helps Me

An offline-first, privacy-focused Android utility app written in Kotlin. Everything runs on your device — no accounts, no tracking, no cloud.

---

## Quick Start

### Requirements

| Tool | Version |
|---|---|
| Android Studio | Ladybug (2024.2) or newer |
| Android SDK | API 35 (Android 15) |
| JDK | 17+ |
| Gradle | 8.7 (auto-downloaded via wrapper) |
| Android device / emulator | API 26+ (Android 8.0+) |

### Build & Run

```bash
# 1. Clone the repo
git clone https://github.com/yourname/alittleappthathelpsme.git
cd alittleappthathelpsme

# 2. Open in Android Studio
#    File → Open → select the folder

# 3. Let Gradle sync (first sync downloads ~500MB of dependencies)

# 4. Run on device / emulator
#    Press the green ▶ Run button in Android Studio
#    OR via command line:
./gradlew installDebug
```

**First Gradle sync** can take 3–10 minutes depending on your internet connection. Subsequent builds are fast (< 30s).

### Build release APK

```bash
./gradlew assembleRelease
# Output: app/build/outputs/apk/release/app-release-unsigned.apk
# Sign with your keystore before installing on a device
```

---

## Project Structure

```
alittleappthathelpsme/
├── app/                    Main activity, NavGraph, Dashboard, DI entry point
├── core/                   Shared theme (Material 3), common UI components
├── data/                   Room DB, all DAOs & entities, repositories, DataStore, Gson
├── feature_notes/          Notes — create/edit/delete, pin, tags, full-text search
├── feature_tasks/          Tasks & Habits — priorities, due dates, streak tracking
├── feature_utils/          Tools — unit converter, QR scanner/generator, password gen, calculator
├── feature_transfer/       AES-256-GCM encrypted P2P file transfer over Wi-Fi
├── feature_nas/            WebDAV client — browse/upload/download UGREEN NAS (or any WebDAV)
├── feature_vault/          Secure Vault — PIN-locked, AES-256 via Android Keystore
├── feature_timer/          Timers — stopwatch, countdown, Pomodoro with progress ring
├── feature_clipboard/      Clipboard manager — history, pin items, auto-clear
└── feature_webui/          Go Live — embedded Ktor web server, browser dashboard
```

### Tech Stack

| Layer | Library |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + StateFlow |
| DI | Hilt |
| Database | Room (SQLite) |
| Preferences | DataStore |
| Async | Coroutines + Flow |
| Encryption (transfers) | AES-256-GCM, SecureRandom (javax.crypto) |
| Encryption (vault) | AES-256-GCM, Android Keystore |
| QR codes | ZXing |
| NAS / WebDAV | Ktor Client (OkHttp engine) |
| Web server (Go Live) | Ktor Server (Netty engine) |
| Build | Gradle 8.7, version catalog (libs.versions.toml) |

---

## Features

### Home Dashboard
- Today's tasks + completion status
- Recent notes (mini cards)
- Quick-action chips to every module
- Stats: pending task count, note count

### Notes
- Create, edit, delete
- Pin notes to top
- Tags (comma-separated, shown as `#tag`)
- Full-text search (title + content + tags)

### Tasks & Habits
- Tasks with priority (Normal / High / Urgent) and due dates
- Filter: Today / Pending / All
- Daily habits with streak counter (reset each day)

### Tools
- **Unit Converter** — Length, Weight, Temperature, Volume, Area, Speed, Data
- **QR Scanner** — Uses device camera (requires Camera permission)
- **QR Generator** — Generate QR from any text or URL
- **Password Generator** — SecureRandom, 8–64 chars, uppercase/lowercase/numbers/symbols, strength indicator
- **Calculator** — Normal and Scientific modes, calculation history

### Encrypted File Transfer
Peer-to-peer, Wi-Fi only, zero servers.

**How it works (AES-256-GCM, Option B — end-to-end):**
1. **Sender** taps "Send a File" → picks a file
2. App generates a random 256-bit AES session key
3. App starts a TCP server on the local network (random port)
4. A **QR code is displayed** containing: the session key + sender IP + port + filename
5. **Receiver** taps "Receive (Scan QR)" → scans the QR code
6. Receiver connects over TCP and receives file chunks
7. Every chunk is **decrypted with AES-256-GCM** on the receiver's device
8. The key is discarded after the session — it never leaves the QR

No plaintext data over the network. No TLS certificates to manage. Works offline.

### NAS Browser (UGREEN NAS / WebDAV)
Connect to your NAS via WebDAV — works on your home network with no internet.

**Setup:**
1. On your UGREEN NAS, enable WebDAV in the web admin panel
   - Usually under: File Manager → Settings → WebDAV → Enable
   - Default port: `5005`, path: `/dav`
2. In the app, tap **NAS** from the dashboard
3. Enter: IP address (e.g. `192.168.1.100`), port, WebDAV path, username, password
4. Tap **Connect**

From there you can browse directories, upload files, download files, create folders, and delete items.

### Secure Vault
PIN-locked local storage for passwords, notes, and secrets.

- First launch: set a 4–12 digit PIN
- Data encrypted with **AES-256-GCM via Android Keystore** (hardware-backed on supported devices)
- Keys never leave the Keystore
- Tap the eye icon to reveal any entry; lock with the lock button

### Timers
- **Stopwatch** — start/pause/lap
- **Countdown** — set hours/minutes/seconds, circular progress indicator
- **Pomodoro** — 25 min work → 5 min break → 15 min long break (every 4 rounds)

### Clipboard Manager
- Tap the paste icon to grab the current system clipboard
- Tap + to manually add text
- Long-press or use the menu to pin, copy, or delete items
- Clear all unpinned items at once
- Optional auto-clear toggle

### Go Live — Browser Dashboard

**What it does:** starts an embedded web server on your device. Any browser on the same Wi-Fi can open the dashboard and see your notes and tasks in real time.

**How to use:**
1. Tap **Go Live** from the dashboard (or the Go Live screen)
2. App starts a local HTTP server and shows a URL like `http://192.168.1.105:52341`
3. A QR code is displayed — scan it with another device, or type the URL in a browser
4. The browser shows your notes and tasks, auto-refreshing every 30 seconds
5. Tap **Open** to open it directly in the device's browser
6. Tap **Stop** when done

**Privacy:** read-only, local network only, no data leaves the device, no internet required.

---

## Permissions

| Permission | When requested |
|---|---|
| `CAMERA` | QR Scanner, Transfer receiver (QR scan) |
| `READ_MEDIA_*` / `READ_EXTERNAL_STORAGE` | File picker for transfer / NAS upload |
| `INTERNET` | NAS (local), Go Live server (local), File transfer (local) |
| `ACCESS_WIFI_STATE` | Getting device IP for file transfer & Go Live |
| `USE_BIOMETRIC` | Optional biometric unlock for Vault (future) |

No background tracking. No location. No microphone.

---

## Privacy

- No analytics, crash reporting, or telemetry
- No user accounts
- All data stored locally (Room DB in app private storage)
- File transfers: AES-256-GCM encrypted before any network byte leaves the device
- Vault: AES-256-GCM via Android Keystore (hardware-backed)
- NAS credentials: stored only in DataStore on-device, never transmitted externally
- Go Live: read-only, local network only, user-initiated, manual stop

---

## Common Issues

**Gradle sync fails with "Failed to resolve dependency"**
→ Check your internet connection. Run `./gradlew --refresh-dependencies`.

**App won't install: "INSTALL_FAILED_VERSION_DOWNGRADE"**
→ Uninstall any existing version: `adb uninstall com.alittleapp`

**File transfer: receiver can't connect**
→ Make sure both devices are on the same Wi-Fi network (not one on Wi-Fi + one on mobile data).
→ Check that no firewall is blocking the random TCP port.

**NAS: "Cannot connect"**
→ Verify the NAS IP from your router's DHCP list.
→ Confirm WebDAV is enabled in your UGREEN NAS settings.
→ Try the NAS web interface in a browser first to confirm it's reachable.

**Go Live: URL not opening**
→ Make sure the viewing device is on the same Wi-Fi network as the Android device.
→ Some corporate/guest Wi-Fi networks block peer-to-peer connections.

---

## Roadmap

- Offline Calendar (events/reminders, no Google account)
- Document Scanner (camera → PDF)
- Voice Notes (record + store locally)
- Flashcards / Spaced Repetition
- Global tag-based search across all modules
- Receipt/expense tracker
- Plugin/module system
- Device-to-device sync (using existing encrypted transfer)

---

## License

See [LICENSE](LICENSE). See [LEGAL.md](LEGAL.md) for full legal disclaimer.
