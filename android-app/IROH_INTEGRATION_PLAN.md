# Iroh P2P Integration Plan for Android

This document outlines the plan for integrating real Iroh-based P2P file transfer functionality into the AltSendme Android app.

## Current Status

The Android app currently uses a **mock `SendmeLibrary`** that simulates P2P transfers. The UI and user experience are complete, but actual peer-to-peer networking is not yet functional.

**Location of mock implementation:** `app/src/main/java/com/altsendme/app/sendme/SendmeLibrary.kt`

## Decision: Wait for Iroh 1.0 (Option C)

**Preferred approach:** Wait for the Iroh team's 1.0 release (planned for 2H 2025) which will include improved mobile/FFI support.

**Rationale:**
- The Iroh team has acknowledged that current FFI bindings don't meet their "P2P that just works" promise
- FFI releases are currently paused while they improve the experience
- 1.0 will have official, well-supported mobile bindings
- Reduces maintenance burden of custom UniFFI setup

## Integration Options Research

### Option A: UniFFI Bindings (DIY Approach)

Build custom Kotlin bindings using Mozilla's UniFFI.

**Complexity:** Medium-High (~7-11 days effort)

**Steps required:**
1. Install Android NDK and configure `ANDROID_NDK_HOME`
2. Add Rust targets:
   ```bash
   rustup target add aarch64-linux-android    # ARM64
   rustup target add armv7-linux-androideabi  # ARM32
   rustup target add x86_64-linux-android     # x86_64 emulator
   ```
3. Configure cargo for Android linkers in `~/.cargo/config.toml`
4. Create UniFFI interface definition (`sendme/src/uniffi.udl`)
5. Add `uniffi` crate to sendme dependencies
6. Generate Kotlin bindings with `uniffi-bindgen`
7. Build `.so` libraries for each architecture
8. Add JNA dependency to Android app
9. Replace mock `SendmeLibrary` with generated bindings

**Pros:**
- Full control over the integration
- Can be done immediately

**Cons:**
- Significant setup effort
- Maintenance burden for cross-compilation
- May break with Iroh updates

### Option B: Use iroh-ffi Directly

Use the existing [iroh-ffi](https://github.com/n0-computer/iroh-ffi) repository maintained by the Iroh team.

**Complexity:** Medium

**Pros:**
- Pre-built Kotlin bindings exist
- Maintained by the Iroh team

**Cons:**
- Currently "tier 2" support (lighter review, slower responses)
- FFI releases paused until improved experience
- May not include sendme-specific blob transfer functionality
- Would need to adapt for sendme's specific use case

### Option C: Wait for Iroh 1.0 (SELECTED)

Wait for the official Iroh 1.0 release with improved mobile support.

**Timeline:** 2H 2025 (per Iroh team roadmap)

**Pros:**
- Better out-of-box mobile experience
- Official support and documentation
- Reduced maintenance burden
- "P2P that just works" promise

**Cons:**
- Timeline uncertainty
- App remains simulation-only until then

## Implementation Checklist (When Iroh 1.0 Releases)

When Iroh 1.0 is available, follow these steps:

- [ ] Review Iroh 1.0 release notes and mobile documentation
- [ ] Check for official Kotlin/Android bindings in iroh-ffi
- [ ] Evaluate if sendme library needs adaptation or if iroh-blobs can be used directly
- [ ] Set up Rust cross-compilation for Android targets
- [ ] Build native libraries for arm64-v8a, armeabi-v7a, x86_64
- [ ] Place `.so` files in `app/src/main/jniLibs/{arch}/`
- [ ] Replace `SendmeLibrary.kt` mock with real Iroh bindings
- [ ] Update `SendmeTypes.kt` if interface changes
- [ ] Handle Android-specific concerns:
  - [ ] Async runtime (Tokio) initialization
  - [ ] Background execution (WorkManager or foreground service)
  - [ ] Network permissions for local discovery
  - [ ] Battery optimization exemptions
- [ ] Test NAT traversal on mobile networks (LTE, 5G)
- [ ] Test relay fallback behavior
- [ ] Update `TransferService.kt` for real transfer notifications

## Files to Modify

```
android-app/app/src/main/
├── java/com/altsendme/app/
│   └── sendme/
│       ├── SendmeLibrary.kt    # Replace mock with real bindings
│       └── SendmeTypes.kt      # Update types if needed
├── jniLibs/                    # NEW: Native libraries
│   ├── arm64-v8a/
│   │   └── libsendme.so
│   ├── armeabi-v7a/
│   │   └── libsendme.so
│   └── x86_64/
│       └── libsendme.so
└── ...

android-app/app/build.gradle.kts  # Add JNA dependency
```

## Dependencies to Add

```kotlin
// In app/build.gradle.kts
dependencies {
    // JNA for native library loading (if using UniFFI)
    implementation("net.java.dev.jna:jna:5.13.0@aar")

    // Or use official Iroh Android SDK when available
    // implementation("computer.iroh:iroh-android:1.0.0")
}
```

## Key Technical Challenges

1. **Async Runtime:** Tokio (used by Iroh) needs special initialization on Android
2. **Background Execution:** Android aggressively kills background processes; use WorkManager or foreground service
3. **Network Permissions:** May need `CHANGE_WIFI_MULTICAST_STATE` for local peer discovery
4. **NAT Traversal:** Mobile carriers often have stricter NAT; expect more relay usage
5. **Battery:** P2P connections can drain battery; implement efficient connection management

## Resources

- [Iroh GitHub](https://github.com/n0-computer/iroh)
- [iroh-ffi Repository](https://github.com/n0-computer/iroh-ffi)
- [Iroh FFI Updates Blog Post](https://www.iroh.computer/blog/ffi-updates)
- [Iroh Mobile Discussion](https://github.com/n0-computer/iroh/discussions/517)
- [UniFFI User Guide](https://mozilla.github.io/uniffi-rs/)
- [Iroh Discord](https://discord.gg/iroh) - For community support

## Contact

For questions about this integration plan, refer to the original implementation discussion or contact the AltSendme maintainers.

---

*Last updated: January 2025*
*Iroh version at time of writing: ~0.28.x (pre-1.0)*
