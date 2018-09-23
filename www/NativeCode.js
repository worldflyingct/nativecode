var exec = require('cordova/exec');

exports.coolMethod = function (arg0, success, error) {
    exec(success, error, 'NativeCode', 'coolMethod', [arg0]);
};

exports.duyun = {
    startaudio: function (arg0) {
        return new Promise (function (resolve, reject) {
            exec(function (res) {
                resolve (res)
            }, function (err) {
                reject (err)
            }, 'NativeCode', 'duyun.startaudio', [arg0]);
        })
    },
    stopaudio: function () {
        return new Promise (function (resolve, reject) {
            exec(function (res) {
                resolve (res)
            }, function (err) {
                reject (err)
            }, 'NativeCode', 'duyun.stopaudio', []);
        })
    }
};
