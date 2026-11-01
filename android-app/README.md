# Playlet Mobile (Android)

An ad-free YouTube client for Android phones, part of this Playlet fork. It uses
[Invidious](https://invidious.io/) servers (an open-source YouTube front end) for feeds, search and video,
so there are no ads, no tracking and no Google account.

- **Home:** trending videos, with Trending / Music / Gaming / Movies chips
- **Search:** endless results
- **Player:** the video on top (full screen when the phone is sideways), title, channel, stats, description,
  and Up next; when a video ends, the first related one plays
- **Settings:** pick the Invidious server (Automatic switches when one fails) and the highest quality

Plain Android framework code (no AndroidX), minSdk 21 (Android 5), target 33.

## Build

No Gradle or Android SDK manager needed:

```sh
sudo apt-get install aapt dalvik-exchange zipalign apksigner   # plus a JDK
./build.sh          # -> build/playlet-mobile-<version>.apk
./run-tests.sh      # parser tests on the JVM
```

`build.sh` downloads the Android 11 framework jar once. Signing uses `$KEYSTORE` or creates
`build/debug.keystore`. Android only installs an update over an existing install when it is signed with
the same key, so keep that keystore if you want to update without uninstalling.

## Limits

- Playback uses Android's built-in player, which can't combine YouTube's separate high-quality video and
  audio streams, so videos play in the muxed qualities servers provide (usually 360p, sometimes 720p).
- Public Invidious servers go down and get blocked by YouTube from time to time; try another server in
  Settings when videos don't load.
