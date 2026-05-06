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

## What the app includes

- **Video feed** — watch videos from nodes you follow
- **Photo feed** — photo and image posts from the M4TR1X network
- **Stories** — ephemeral photo and video stories, stored on nodes
- **Music** — audio content from the network
- **Encrypted messaging** — end-to-end encrypted direct messages
- **Tipping** — send H8 tokens to creators directly from your phone
- **Node selector** — connect to any M4TR1X node, including your own self-hosted node

The app connects to a M4TR1X node. By default it uses public nodes from the network directory. If you run your own node ([m4tr1x-node](https://github.com/H8dboy/m4tr1x-node)) you can point the app directly at it.

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
