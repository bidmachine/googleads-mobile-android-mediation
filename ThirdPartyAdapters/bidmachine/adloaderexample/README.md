# BidMachine AdLoader example (Android)

Reproduces the banner integration The Weather Channel uses on Android: an Ad Manager `AdLoader`
request with the Ad Manager banner and native ad types, no `AdManagerAdView` created by the app,
and the banner sizes supplied only through `forAdManagerAdView`. It is the Android counterpart of
the iOS example in the `googleads-mobile-ios-mediation` fork.

Unlike iOS, Android hands the sizes to Google when the loader is built, so the adapter sees a
size during signal collection: Google passes the primary size, the first one listed. The adapter
maps it to the closest of 320x50, 300x250 and 728x90, falling back to 320x50 for bidding when
nothing is close, so a 240x133 primary size becomes a 320x50 token. Google then sizes the load
request from the same primary size, whatever the bid declares.

## Choosing the sizes

The Sizes button picks what the request offers Google, and reloads. The presets cover the Weather
Channel feed list with 240x133 first, the same list led by 320x50 or 300x250, a single 320x50 or
300x250, and an anchored or inline adaptive banner at the screen width. The status line names the
active preset, and logcat tag `AdLoaderExample` prints the sizes each load offered. A launch extra
picks the preset without the UI:

```
adb shell am start -n com.weather.Weather/io.bidmachine.adloaderexample.AdLoaderBannerActivity \
  --es sizes MREC_FIRST
```

The names are the `SizeSet` entries: `WEATHER_FEED`, `BANNER_FIRST`, `MREC_FIRST`, `BANNER_ONLY`,
`MREC_ONLY`, `ANCHORED_ADAPTIVE`, `INLINE_ADAPTIVE`.

## Setup

The module lives in the adapter's Gradle project and depends on `:bidmachine`, so the adapter is
built from the sources in this repository, with Google Mobile Ads 24.9.0 and BidMachine SDK 3.5.1
as the adapter declares them.

1. Build from `ThirdPartyAdapters/bidmachine` with a JDK 17:

   ```
   ./gradlew :adloaderexample:assembleDebug -PadManagerAppId=ca-app-pub-XXXX~YYYY
   ```

   `adManagerAppId` is the publisher's Ad Manager app ID, which decides the mediation
   configuration Google applies to the unit. Without it the build uses Google's sample app ID:
   the SDK initialises, but the unit carries no BidMachine mapping.
2. `adUnitId` in `AdLoaderBannerActivity.kt` is the publisher's Android home screen unit,
   `/7646/app_android_us/thr_display/home_screen/today`; the test unit is
   `/7646/test_app_android_us/thr_display/home_screen/today`. The application ID is the
   publisher's package name so Google treats the requests as theirs.
3. Run on a device whose advertising ID is allowlisted for BidMachine test bidders. The app logs
   it at start (`GAID …`); use it for the allowlist and for the exchange's per-IFA logging
   allocations.

## Logs

The application turns on BidMachine SDK logging before Google initialises the adapter and prints
every adapter's initialisation status. Watch with:

```
adb logcat -s AdLoaderExample BidMachine Ads
```
