# Google Maps Setup Instructions

The Statistics page includes a map view showing where users clicked on phishing campaigns. To enable this feature, you need to add a Google Maps API key.

## Quick Setup

### 1. Get a Google Maps API Key

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Select your Firebase project (or create a new one)
3. Enable the **Maps SDK for Android** API:
   - Go to **APIs & Services** → **Library**
   - Search for "Maps SDK for Android"
   - Click **Enable**
4. Create an API Key:
   - Go to **APIs & Services** → **Credentials**
   - Click **Create Credentials** → **API Key**
   - Copy the generated API key

### 2. Add the Key to Your Project

Open `app/src/main/AndroidManifest.xml` and replace `YOUR_API_KEY_HERE` with your actual API key:

```xml
<meta-data
    android:name="com.google.android.geo.API_KEY"
    android:value="AIzaSyC1234567890abcdefghijklmnopqrstuvw" />
```

### 3. Restrict the API Key (Recommended)

For security, restrict your API key to your app:

1. In Google Cloud Console → **Credentials**
2. Click on your API key
3. Under **Application restrictions**:
   - Select **Android apps**
   - Click **Add an item**
   - Add your package name: `com.phishing.simulation`
   - Add your SHA-1 certificate fingerprint (see below)

### 4. Get Your SHA-1 Fingerprint

Run this command in your project directory:

```bash
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

Copy the SHA-1 fingerprint and add it to your API key restrictions.

## Using the Map Feature

Once set up, the map will:
- Show red markers for each detection location
- Display campaign name when clicking markers
- Auto-zoom to show all markers
- Only display detections with valid coordinates (not 0,0)
- Hide if no valid location data exists

## Troubleshooting

**Map not showing?**
- Check that the API key is correct
- Ensure Maps SDK for Android is enabled
- Verify the meta-data tag is inside `<application>` in AndroidManifest.xml
- Check logcat for API key errors

**"This app won't run unless you update Google Play services"?**
- Update Google Play Services on your device/emulator
- Make sure you're using a device/emulator with Google Play

**Markers not appearing?**
- Detections with location (0, 0) are filtered out
- Grant location permission when users click campaigns
- Check Firebase to verify location data is being saved

## Without API Key

The app will still work perfectly without the API key - the map card will simply be hidden from the statistics page. All other features (charts, detection list, etc.) work independently.
