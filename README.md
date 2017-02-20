# RSG Portal

App for automatically updating Android apps outside of the Play store. RSG Portal allows you to distribute and install .apk packages on Android phones and tablets, as well as download and manage data files separate to the main app. The RSG Portal will also verify all downloads and can handle corrupted installations or broken downloads.

This is used by Real Serious Games for distribution of our B2B Android apps that cannot be released on the Google Play Store but still require ongoing support and updates. The RSG Portal also updates itself, so it's possible to push out new functionality to devices remotely. Since the RSG Portal allows you to host your own server, it also supports auto-updating apps on devices that aren't usually connected to the Internet but do have a local network connection.

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->


- [Usage](#usage)
- [Setting up the server](#setting-up-the-server)
  - [Initial setup](#initial-setup)
  - [Rolling out updates](#rolling-out-updates)
- [Setting up the client](#setting-up-the-client)
- [Managing downloads and extra data files](#managing-downloads-and-extra-data-files)
- [Error codes](#error-codes)
- [Contributing](#contributing)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

## Usage
There are two parts to the RSG Portal: the client which runs on Android devices and the server which uses [Node.js](https://nodejs.org/en/) and can run on a dedicated machine or in the cloud. 

## Setting up the server

### Initial setup

The server requires Node.js version 4.2.6 LTS or later. Once you have Node.js installed, copy the `Server` directory to the machine you plan to use as the server and run `npm install` to install its dependencies.

Once the dependencies have been installed you will need to set up the folder structure for the resources you want to distribute via the server. An example of how to do this is included in the `test-resources` directory:
```
test-resources
 ├─portal_manifest.json
 ├─Portal.apk
 └─android
    └─com.RSG.MyApp
       ├─current_version.json
       └─1
         ├─com.RSG.MyApp.apk
         └─data
            └─Blah.txt
```

In order for the Portal to update itself you will need to include a `portal_manifest.json` file in the root folder of the resources directory, which includes the name of the APK for the RSG Portal (in the same directory), and the latest version of it. An example of this file can be found in the `test-resources` directory.

You will create a numbered folder for each version of your app inside `<your resources directory>/android/<your app bundle identifier>/`, as well as a `current_version.json` file with the following format:
```
{
    "CurrentVersion": <version number>
}
```
Where "version number" is an integer corresponding to the current version of the app, which is also a folder inside the same directory.

Inside each numbered folder you should include the APK file of your app, named to match the [bundle identifier](https://developer.android.com/studio/build/application-id.html) of your app, and a `data` folder with any data files required by the app.

Once the resources folder has been configured, you can run the server with this command:
```
node index --resources=test-resources
``` 
Replace `test-resources` with the path to your own resources folder.

>Note that the RSG Portal currently doesn't support any kind of access restriction on downloads, so running it on a server that's open to the Internet will allow anyone to download any version of the app.

### Rolling out updates
Updates can be rolled out by simply creating a new numbered folder inside your app's resources directory on the server. For example, if we wanted to push out a new version of our previous example app, the folder structure could change to look like this:
```
test-resources
 ├─portal_manifest.json
 ├─Portal.apk
 └─android
    └─com.RSG.MyApp
       ├─current_version.json
       ├─1
       │ ├─com.RSG.MyApp.apk
       │ └─data
       │    └─Blah.txt
       └─2
         ├─com.RSG.MyApp.apk
         └─data
            └─Blah.txt
```

To activate this version we will also need to update `current_version.json` to list version 2 as the current version: 
```
{
    "CurrentVersion": 2
}
```

This allows you to upload the new files to your server before activating the new version and pushing out an update to the app. New versions can be added without the need to restart the server, as the server reads `current_version.json` each time a client requests the latest version of the app.

Note that when you roll out an update, you don't need to duplicate all files in the previous version if they were not modified. For example, if the APK file changed but `Blah.txt` stayed the same between versions, it could be omitted from version 2 and the app will still automatically get the version of the file that's in version 1 because that's the latest version.

## Setting up the client
The client will run on any device running Android 4.4 (API level 19) or higher, and has been tested with up to Android 7.1.1.

Building the client requires [Android Studio](https://developer.android.com/studio/index.html). It also requires the Android SDK Platform version 23, the Android SDK build tools version 23.0.2, and the Android Support repository, all of which can be installed through the Android SDK Manager.

Open the `Client` folder in Android Studio to build it. After building in Android Studio, an APK file that can be installed to devices can be found in `Client/app/build/outputs/apk`.

By default, the app is set up to download and install an app called `com.RSG.MyApp`, which matches the sample APK file inside the `Server/test-resources` folder. To change this, simply change the value of `AppUpdater.APP_ID` to match the [bundle identifier](https://developer.android.com/studio/build/application-id.html) the app you wish to manage updates for inside `Client/app/src/main/java/com/rsg/rsgportal/AppUpdater.java`. 

To change the name displayed in the UI for the app, change the `application_title` string value in `Client/app/src/main/res/values/strings.xml`.

Finally, in order to make the app talk to your server, you will need to set the IP or hostname of the server for it to talk to. This is stored in `PortalAppInfo.RemoteServerAddress` inside `Client/app/src/main/java/com/rsg/rsgportal/PortalAppInfo.java`. Note that this needs to include the port the server is running on, which by default is port 3000.


## Managing downloads and extra data files

The RSG Portal has the ability to download external data files separate to the main APK. These are stored in the [external storage directory](https://developer.android.com/guide/topics/data/data-storage.html#filesExternal) inside a subfolder with a name matching the app bundle identifier. Usually this will map to something like `/storage/emulated/0/com.RSG.MyApp`, replacing "com.RSG.MyApp" with the bundle identifier of your app, however the location of the external storage directory isn't guaranteed to be the same on different devices so the safest way to get to this directory from the app you are distributing via RSG Portal is as follows: 
```
File dataDirectory = new File(Environment.getExternalStorageState(), Activity.getPackageName());
```

All downloads from the RSG Portal are saved in the Downloads directory before APKs are installed and data files are copied to their correct locations, and the manifest files saved here are used to verify the integrity of installed data files by checking their MD5 sums. This allows you to roll back versions or reinstall without needing to re-download any data from the server, and since the MD5s are computed on the server before files are sent, also protects against corrupted downloads due to the app or server crashing. 

>In its current state, this verification is only intended to protect against broken downloads. This doesn't protect against man-in-the-middle attacks or provide a particularly cryptographically secure method of verifying the files though, as RSG Portal currently does not support downloading via HTTPS or the ability to embed a private key to verify against in the Portal app. 

## Error codes
Some errors in the client will show an error message with a numerical error code. These codes do not correspond to HTTP status codes, but instead follow a scheme detailed in full in the `Error codes.txt` file.

Error codes pertain to different areas depending on their range: 
 - 0 - 100 indicate a problem with local data (for example, not enough space to download, invalid permission to write to external storage directory)
 - 100 - 200 indicate a connection issue such as not being able to reach the server
 - 200 - 300 indicate a server-side error such as the requested resource not being found on the server or an internal server error

## Contributing

Feel free to submit pull requests if you have any functionality you would like to add. Both the client and the server have suites of tests so pull requests must have test coverage in order to be accepted.

To run the unit tests for the client, load up the project in Android Studio and select the "rsgportal in app" JUnit run configuration.

For the server, you will need [Mocha](https://mochajs.org/), which can be installed via `npm install -g mocha`. To run the tests, make sure that you are in the `Server` directory and run `mocha`, which should find the tests located in `test/integration.test.js` automatically. 