'use strict';

describe('integration', function() {
    const chai = require('chai');
    const expect = chai.expect;
    chai.use(require('chai-as-promised'));
    chai.use(require('chai-things'));
    
    const request = require('request-promise');
    
    const crypto = require('crypto');

    const startServer = require('../index.js');
    let app;

    beforeEach(function () {
        return startServer('test-resources')
            .then(server => app = server);     
    });

    afterEach(function () {  
        return new Promise((resolve, reject) => {
            app.close(() => resolve());
        });
    });

    it('app manifest has AvailableVersions that match folders under app', function() {
        return expect(request('http://localhost:3000/v1/android/com.RSG.MyApp/manifest')
            .then(manifest => JSON.parse(manifest).AvailableVersions))
            .to.eventually.deep.equal(['1']);
    });
    
    it('app manifest has correct CurrentVersion', function() {
        return expect(request('http://localhost:3000/v1/android/com.RSG.MyApp/manifest')
            .then(manifest => JSON.parse(manifest).CurrentVersion))
            .to.eventually.equal(1);
    });
    
    it('version manifest includes correct apk md5', function() {
        return request('http://localhost:3000/v1/android/com.RSG.MyApp/1/com.RSG.MyApp.apk', { encoding: null })
            .then(apkData => crypto.createHash('md5').update(apkData).digest('hex'))
            .then(hash => {
                return expect(request('http://localhost:3000/v1/android/com.RSG.MyApp/1/manifest')
                    .then(manifest => JSON.parse(manifest).apkmd5))
                    .to.eventually.equal(hash);
            });
    });
    
    it('version manifest includes correct apk size', function() {
        return request('http://localhost:3000/v1/android/com.RSG.MyApp/1/com.RSG.MyApp.apk', { encoding: null })
            .then(apkData => apkData.length)
            .then(size => {
                return expect(request('http://localhost:3000/v1/android/com.RSG.MyApp/1/manifest')
                    .then(manifest => JSON.parse(manifest).apkSize))
                    .to.eventually.equal(size);
            });
    });
    
    it('version manifest includes data file', function() {
        return request('http://localhost:3000/v1/android/com.RSG.MyApp/1/manifest')
            .then(manifest => {
                const files = JSON.parse(manifest).files;
                
                return expect(files).to.contain.an.item.with.property('source', 'Blah.txt');
            });
    });
    
    it('version manifest includes apk version code', function() {
        return request('http://localhost:3000/v1/android/com.RSG.MyApp/1/manifest')
            .then(manifest => {
                const version = JSON.parse(manifest).apkVersionCode;
                
                return expect(version).to.equal(1);
            });
    });
});