# RSG Portal

App for automatically updating Android apps outside of the Play store. RSG Portal allows you to distribute and install .apk packages on Android phones and tablets, as well as download and version data files separate to the main app. The RSG Portal will also verify all downloads and can handle courrupted installations or broken downloads.

This is used by Real Serious Games for distribution of our B2B Android apps that cannot be released on the Google Play Store but still require ongoing support and updates. The RSG Portal also updates itself, so it's possible to push out new functionality to devices remotely. Since the RSG Portal allows you to host your own server, it also supports auto-updating apps on devices that aren't usually connected to the Internet but do have a local network connection.

## Usage
There are two parts to the RSG Portal: the client which runs on Android devices and the server which uses [Node.js](https://nodejs.org/en/) and can run on a dedicated machine or a VPS in the cloud. 

## Setting up the client
Building the client requires [Android Studio](https://developer.android.com/studio/index.html). It also requires the Android SDK Platform version 23, the Android SDK build tools version 23, and the Android Support repository, all of which can be installed through the Android SDK Manager.

Open the `Client` folder in Android Studio to build it. After building in Android Studio, an APK file that can be installed to devices can be found in `Client/app/build/outputs/apk`.

By default, the app is set up to download and install an app called `com.RSG.MyApp`, which matches the sample APK file inside the `Server/test-resources` folder. To change this, simply change the value of `AppUpdater.APP_ID` to match the [bundle identifier](https://developer.android.com/studio/build/application-id.html) the app you wish to manage updates for (inside `Client/app/src/main/java/com/rsg/rsgportal/AppUpdater.java`). 

To change the name displayed in the UI for the app, change the `application_title` string value in `Client/app/src/main/res/values/strings.xml`.

## Setting up the server

The server requires a fairly recent version of Node.js due to its use of ES6 and has been tested against version 6.9 LTS. Once you have Node.js installed, copy the `Server` directory to the machine you plan to use as the server and run `npm install` to install the dependencies.

Once the dependencies have been installed you will need to set up the folder structure for the resources you want to distribute via the server. An example of how to do this is included in the `test-resources` directory. 

In order for the Portal to update itself you will need to include a `portal_manifest.json` file in the root folder of the resources directory, which includes the name of the APK for the RSG Portal (in the same directory), and the latest version of it. An example of this file can be found in the `test-resources` directory.

You will create a numbered folder for each version of your app inside `<your resources directory>/android/<your app bundle identifier>/`, as well as a `current_version.json` file with the following format:
```
{
    "CurrentVersion": 1
}
```
Where "1" corresponds to the current version of the app, which is also a folder inside the same directory.

Inside each numbered folder you should include the APK file of your app, named to match the [bundle identifier](https://developer.android.com/studio/build/application-id.html) of your app, and a `data` folder with any data files required by the app.

Once the resources folder has been cofigured, you can run the server with this command:
```
node index --resources=test-resources
``` 
Replace `test-resources` with the path to your own resources folder.
