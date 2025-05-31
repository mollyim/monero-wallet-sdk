# Monero Wallet SDK for Android

[![Build](https://github.com/mollyim/monero-wallet-sdk/actions/workflows/build.yml/badge.svg)](https://github.com/mollyim/monero-wallet-sdk/actions/workflows/build.yml)
![Maven Central](https://img.shields.io/maven-central/v/im.molly/monero-wallet-sdk)
![Snapshot](https://img.shields.io/maven-metadata/v?color=orange&label=snapshot&metadataUrl=https%3A%2F%2Fcentral.sonatype.com%2Frepository%2Fmaven-snapshots%2Fim%2Fmolly%2Fmonero-wallet-sdk%2Fmaven-metadata.xml)

A modern Kotlin library that embeds Monero's wallet2 inside a sandboxed Android Service and
exposes an idiomatic, asynchronous API for mobile apps.

## Key Features

- **Kotlin-native API**: Asynchronous by design, using `suspend` functions and `Flow`.
- **Sandboxed native code**: All C++ runs in a zero-privilege, isolated process.
- **Pluggable storage**: Bring your own persistence layer (files, DB, cloud) via the
  `StorageProvider` interface.
- **Custom HTTP stack**: Inject any networking code (plain, Tor, I2P, QUIC, …) to talk to Monero
  remote nodes.
- **Client-side load-balancing**: Automatic node selection for faster sync & fail-over.
- **Tiny library (~6.5 MB AAR)**: LTO, dead-code elimination, and static vendored deps keep the
  footprint small.
- **Jetpack-Compose demo wallet**: Full sample app following Google's official architecture
  guidelines.

## Setup

The SDK is available on Maven Central:

```kotlin
dependencies {
    implementation("im.molly:monero-wallet-sdk:<latest-version>")
}
```

Make sure `mavenCentral()` is in your repositories block.

To use snapshot versions:

```kotlin
repositories {
    maven {
        name = "Central Portal Snapshots"
        url = uri("https://central.sonatype.com/repository/maven-snapshots/")
        content {
            includeModule("im.molly", "monero-wallet-sdk")
        }
    }
}
```

```kotlin
dependencies {
    implementation("im.molly:monero-wallet-sdk:<snapshot-version>")
}
```

Replace `<latest-version>` or `<snapshot-version>` with the version you want to use.

## Demo App

A fully functional demo wallet is included in `demo/android`, implemented using Jetpack Compose
and following Android's modern app architecture best practices.

To try it out:

1. Clone the repository with submodules:
   ```sh
   git clone --recursive https://github.com/mollyim/monero-wallet-sdk
   ```
2. Open the root project directory in Android Studio (Meerkat or later).
3. Select the demo run configuration and press Run.

The demo app showcases wallet creation, sync, transaction sending, and more.

## Requirements

- Android 8.0 (API 26+)
- Kotlin **2.1.0**
- Android Gradle Plugin **8.1.0+**

## Roadmap

| Feature                         | Status          |
|---------------------------------|-----------------|
| Wallet management (create/open) | ✅ Done         |
| Balance, history, sync          | ✅ Done         |
| Send XMR                        | ✅ Done         |
| Seraphis migration support      | 🔜 Planned      |

## Acknowledgements

- Funded by the Monero Community Crowdfunding System (CCS).

## License

This project is licensed under the
[GNU General Public License v3.0](https://www.gnu.org/licenses/gpl-3.0.txt).
