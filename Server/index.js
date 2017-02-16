'use strict';

//
// Example usage:
//
// node index.js --resources=my-resources-directory
//
//
// Example URLs:
//
// http://localhost:3000/v1/android/com.RSG.SomeApp/manifest
// http://localhost:3000/v1/android/com.RSG.SomeApp/latest
// http://localhost:3000/v1/android/com.RSG.SomeApp/data/manifest
// http://localhost:3000/v1/android/com.RSG.SomeApp/data/Blah.txt
// http://localhost:3000/v1/android/com.RSG.SomeApp/data/sub-dir/Dark%20theme%20forever.txt

const startServer = function(resourceDirectory) {
    
    return new Promise((resolve, reject) => {
            
        const conf = require('confucious');

        const Enumerable = require('linq');

        conf.pushArgv();

        const resourcesConfig = conf.get('resources');
        if (resourcesConfig && !resourceDirectory) {
            resourceDirectory = resourcesConfig;
        }
        
        if (!resourceDirectory) {
            throw new Error('--resources command line option not specified.')
        }

        const fs = require('fs');
        if (!fs.existsSync(resourceDirectory)) {
            throw new Error('Resources directory not found: ' + resourceDirectory);    
        }
        const fsp = require('fs-promise');
        const path = require('path');
        const md5File = require('md5-file');
        const apkReader = require('node-apk-parser');

        const express = require('express');
        const app = express();

        console.log('Serving resources from: ' + resourceDirectory);

        app.use('/v1', express.static(resourceDirectory));

        // Get all directories in a given directory
        // based on code from http://stackoverflow.com/questions/18112204/get-all-directories-within-directory-nodejs/18112359#18112359
        // Modified to work in strict mode and return a promise
        const getDirs = function(rootDir) {
            return fsp.readdir(rootDir)
                .then(files => {
                    return Enumerable.from(files)
                        .aggregate(Promise.resolve([]), (prevPromise, file) => {
                            return prevPromise.then(versionDirectories => {
                                const filePath = path.join(rootDir, file);
                                return fsp.stat(filePath)
                                    .then(stat => {
                                        if (stat.isDirectory()) {
                                            return versionDirectories.concat([file]);
                                        }
                                        return versionDirectories;
                                    });
                            });
                        });
                });
        }

        // Get the MD5 sum of the given file. Returns a promise
        const md5 = function(file) {
            return new Promise((resolve, reject) => {
                md5File(file, (error, sum) => {
                    if (error) {
                        reject(error);
                        return;
                    }
                    
                    resolve(sum);
                })
            });
        }

        // Generate a manifest with a given app ID and version. Returns a promise
        const generateManifest = function(appName, version) {
            const manifest = {
                destination: appName,
                files: []
            };
            
            const apkLocation = path.join(resourceDirectory, 'android', appName, version, appName + '.apk');
            const dataDirLocation = path.join(resourceDirectory, 'android', appName, version, 'data');
            
            // Read version info from apk file
            try {
                const reader = apkReader.readFile(apkLocation);
                const apkManifest = reader.readManifestSync();
                manifest.apkVersionCode = apkManifest.versionCode;
            }
            catch (err) {
                return Promise.reject(err);
            }
            
            return fsp.stat(apkLocation)
                .then(stats => {
                    manifest.apkSize = stats['size'];
                    
                    return md5(apkLocation);
                })
                .then(md5 => {
                    manifest.apkmd5 = md5;
                    
                    return fsp.readdir(dataDirLocation);
                })
                .then(dataFiles => {            
                    return Enumerable.from(dataFiles)
                        .aggregate(Promise.resolve([]), (prevPromise, dataFile) => {;
                            return prevPromise.then(files => {
                                const filePath = path.join(dataDirLocation, dataFile);
                                return Promise.all([
                                        fsp.stat(filePath),
                                        md5(filePath)
                                    ])
                                    .then(results => {
                                        const stats = results[0];
                                        const md5 = results[1];
                                        const file = {
                                            source: dataFile,
                                            size: stats['size'],
                                            md5: md5,
                                        };
                                        return files.concat([file]);
                                    });
                            });
                        });
                })
                .then(fileDefinitions => {
                    manifest.files = fileDefinitions
                    return manifest;
                });
        }
        
        // Set up the routes to get the manifest for every version of a given app
        const setUpRoutesForApp = function(appName) {
            return getDirs(path.join(resourceDirectory, 'android', appName))
                .then(versions => {
                    // First, set up route to get list of versions.
                    app.get('/v1/android/' + appName + '/manifest', (req, res) => {
                        
                        const currentVersionInfoPath = path.join(resourceDirectory, 'android', appName, 'current_version.json');
                        fs.readFile(currentVersionInfoPath, (err, data) => {
                            if (err) throw err;
                            
                            const manifest = JSON.parse(data);
                            manifest.AvailableVersions = [];
                            
                            versions.forEach(version => {
                                manifest.AvailableVersions.push(version);
                            });
                            
                            res.status(200).send(manifest);
                        })
                    });
                    
                    // Then set up routes for all versions of the app. 
                    versions.forEach(version => {
                        const route = '/v1/android/' + appName + '/' + version + '/manifest';
                        //console.log("Setting up route: " + route);
                        app.get(route, (req, res) => {
                            generateManifest(appName, version)
                                .then(manifest => res.send(manifest))
                                .catch(ex => {
                                    res.status(500).send();
                                    console.error(ex && ex.stack || ex);
                                });
                        });
                    });
                });
        }
    
        getDirs(path.join(resourceDirectory, 'android'))
            .then(appDirectories => {
                return Enumerable.from(appDirectories)
                    .aggregate(Promise.resolve([]), (prevPromise, appName) => {
                        return prevPromise.then(() => setUpRoutesForApp(appName));
                    });
            })
            .then(() => {
                const server = app.listen(conf.get('port') || 3000, function () {
                    const host = server.address().address;
                    const port = server.address().port;

                    console.log('RSG Portal server listening at http://%s:%s', host, port);
                    
                    resolve(server);
                });  
            });
    });
}

// http://stackoverflow.com/a/6398335
if (require.main === module) {     
    startServer()
        .catch(ex => console.error(ex && ex.stack || ex));
}
else {
    module.exports = startServer;
}

