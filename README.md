# Custom Tab Attack POC

## Start the application

### Option 1: Use the .apk

Install `app/release/com.brokenbridge.poc.ct.apk` on your device or an emulator.

### Option 2: Build yourself

Open the project in [Android Studio](https://developer.android.com/studio) to build and run the application. 
You can either run the application on an emulator or on your Android device.

## Run the attack

After starting the application, you can specify a URL that will be opened in a hidden Custom Tab and a WebView. By clicking "Launch Custom Tab", the Custom Tab will launched and another Activity will be opened. Stay on this Activity for a few seconds to make sure that the website has finished loading in the Custom Tab. Return to the other Activity (use the back button on the upper left corner or the back gesture/button of your device). The events that are fired inside the Custom Tab and the WebView (which is used for the timing-based attack as a baseline) are now displayed.
