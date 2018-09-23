var exec = require('cordova/exec');

exports.coolMethod = function (arg0, success, error) {
    exec(success, error, 'NativeCode', 'coolMethod', [arg0]);
};

exports.duyun = {
    startaudio: function (arg0, success, error) {
        exec(success, error, 'NativeCode', 'duyun.startaudio', [arg0]);
    },
    stopaudio: function (arg0, success, error) {
        exec(success, error, 'NativeCode', 'duyun.stopaudio', [arg0]);
    }
};
