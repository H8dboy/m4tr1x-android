<div align="center">

# m4tr1x-android

**M4TR1X for Android and iOS — built with Capacitor. Not on any app store.**

</div>

---

## Why no app store

Apple and Google operate the two dominant mobile app stores. Both are subject to the laws of the jurisdictions where they operate — including EU requirements for age verification, content moderation, and platform compliance. An app distributed through those stores can be removed, restricted, or required to implement identity checks at any time.

m4tr1x-android is distributed directly — as an APK for Android and a sideloaded build for iOS — because the network it connects to has no central point of compliance, and its distribution reflects the same principle.

---

## Why M4TR1X exists

The EU's Digital Services Act and related proposals — including age verification requirements and digital identity schemes — are designed to make social platforms accountable by making users identifiable. The mechanism: platforms must verify who their users are, so responsibility can be traced and enforced.

M4TR1X is built on a different architecture. There is no central server, no company, no user database. Identity in M4TR1X is a cryptographic keypair that lives on your device. There is nothing to verify against, no authority to satisfy, and no single operator that can be ordered to restrict access.

---

## What the app is

This app does not run a node, does not store your identity and does not hold your keys. It is a **thin bridge to a M4TR1X node** — the Electron desktop app or a headless [m4tr1x-node](https://github.com/H8dboy/m4tr1x-node) on your home network or behind Tor.

Your phone is the screen. Your node is the network.

```
┌──────────────┐   WiFi / Tor    ┌──────────────────────────┐
│  Phone app   │ ──────────────► │  Your M4TR1X node        │
│  (this repo) │   HTTP API      │  identity · relay · H8   │
└──────────────┘                 └──────────────────────────┘
```

What it includes:

- **Video feed** — watch videos from nodes you follow
- **Photo feed** — photo and image posts from the M4TR1X network
- **Stories** — ephemeral photo and video stories, stored on nodes
- **Music** — audio content from the network
- **Encrypted messaging** — end-to-end encrypted direct messages
- **Tipping** — send H8 tokens to creators directly from your phone
- **Node selector** — connect to any M4TR1X node, including your own self-hosted node

---

## Connecting to your node

1. Open the app — it asks for your node address.
2. Tap **CERCA NODO SULLA RETE 🔍** to auto-discover nodes on your WiFi — the app scans the common private subnets for a M4TR1X `/health` endpoint and lists every node it finds, one tap to connect. The last working subnet is remembered and scanned first.
3. Or type the address manually: `http://192.168.1.x:8080` or your `.onion` (Tor).

The choice is remembered; tap ⚡ in the top bar to switch node.

---

## Native Tor (Android)

The Android build **embeds Tor** (Guardian Project `tor-android` binary) — no
Orbot, no external apps. Enter a `.onion` node address and the app boots its
own Tor daemon (bootstrap progress shown live), then routes traffic through it:

- **GET requests — API, photos, HLS video, stories** — are transparently
  intercepted at the WebView level (`OnionWebViewClient`) and tunneled
  through Tor's SOCKS5. The web layer doesn't know Tor exists.
- **POST requests** go through the `TorBridge` Capacitor plugin (WebViews
  don't expose request bodies to native interceptors).
- **Media uploads** (photos, stories) work over Tor too: the plugin builds
  the multipart/form-data natively and streams it through the tunnel.
- DNS for `.onion` never leaves the tunnel — hostnames are resolved by Tor
  itself (SOCKS5 ATYP=domain).
- Tor runs as a child process on port 39050 (no clash with Orbot) and dies
  with the app — zero background battery drain.
- The connection dot in the top bar turns **purple 🧅** when you're on Tor.

iOS Tor (Tor.framework) is on the roadmap.

---

## Install — Android

1. Download the latest APK from [Releases](https://github.com/H8dboy/m4tr1x-android/releases/latest)
2. Verify the SHA-256 checksum against `checksums-*.txt` in the release
3. On your device: Settings → Security → Install unknown apps → allow your browser or file manager
4. Open the APK and install

**Minimum Android version:** Android 8.0 (API 26)

---

## Install — iOS

iOS does not allow direct APK installation. Options:

- **AltStore** — sideload the IPA using AltStore on a Mac or PC with your Apple ID
- **Sideloadly** — alternative sideloading tool for Windows and macOS

Instructions: [`docs/IOS_INSTALL.md`](docs/IOS_INSTALL.md)

**Minimum iOS version:** iOS 14

---

## Builds from GitHub Actions

Every push builds the apps in CI — no local toolchain needed:

| Platform | Workflow | Output |
|----------|----------|--------|
| Android | `build-android.yml` | APK artifact on every push; GitHub Release on `v*` tags |
| iOS | `build-ios.yml` | IPA via enterprise/sideload signing |

Android signing uses the repo secrets `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`; without them the APK is debug-signed (still installable). iOS uses `APPLE_CERTIFICATE_BASE64`, `APPLE_CERTIFICATE_PASSWORD`, `APPLE_PROVISIONING_PROFILE_BASE64`, `KEYCHAIN_PASSWORD`.

---

## Build from source

**Prerequisites:** Node.js 18+, Capacitor CLI, Android Studio (Android) or Xcode 14+ on macOS (iOS)

```bash
git clone https://github.com/H8dboy/m4tr1x-android.git
cd m4tr1x-android
npm install
```

**Android APK:**

```bash
npx cap sync android
cd android && ./gradlew assembleRelease
```

**iOS IPA:**

```bash
npx cap sync ios
npx cap open ios
# Archive and export from Xcode → Product → Archive
```

---

## Architecture

```
┌──────────────────────────────────────────────┐
│       Capacitor Shell (iOS / Android)        │
│                                              │
│  ┌──────────────────────────────────────┐   │
│  │        Web layer (HTML/JS/CSS)       │   │
│  │  Feed │ Stories │ Video │ Music      │   │
│  │  Messages │ Tipping │ Settings       │   │
│  └──────────────────┬───────────────────┘   │
│                     │                       │
│  ┌──────────────────▼───────────────────┐   │
│  │       Capacitor Native Bridge        │   │
│  │  Camera │ Storage │ Notifications    │   │
│  │  Tor routing │ Crypto (ML-DSA-65)    │   │
│  └──────────────────────────────────────┘   │
└──────────────────────────────────────────────┘
          │ WebSocket / HTTPS
          ▼
┌──────────────────────┐
│     m4tr1x-node      │
│  (self-hosted or     │
│   public network)    │
└──────────────────────┘
```

---

## Security

- Account identity: **ML-DSA-65 keypair** (NIST FIPS-204) — same keypair as the desktop app
- Private key encrypted at rest with **AES-256-GCM**
- Network traffic routed over **Tor by default**
- No account recovery — losing your password means losing account access
- No phone number, no email, no identity registered with any platform

Your account is the same across mobile and desktop. Import it from the desktop app via Settings → Import Account.

---

## Roadmap

| Version | Status | Notes |
|---------|--------|-------|
| v0.1.0 | In development | Core feed, video, photo, messaging |
| v0.2.0 | Planned | Stories, music, tipping |
| v0.3.0 | Planned | Push notifications, account import/export |
| v1.0.0 | Planned | Feature parity with desktop, public release |

Aligned with the M4TR1X desktop app v2.4 Public Beta target.

---

## Contributing

Early build. Most useful areas: video player, Tor integration on mobile, account import/export flow. Read [CONTRIBUTING.md](CONTRIBUTING.md) before opening a PR. Security issues: [SECURITY.md](SECURITY.md).

---

## License

MIT — see [LICENSE](LICENSE).

Part of the [M4TR1X project](https://github.com/H8dboy/m4tr1x-electron) — built by [@H8dboy](https://github.com/H8dboy) — Brescia, Italy
