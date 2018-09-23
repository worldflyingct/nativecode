package cn.worldflying.cordovaplugin;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.unisec.talkback.Talkback;

/**
 * This class echoes a string called from JavaScript.
 */
public class NativeCode extends CordovaPlugin {

    private Talkback talkback = null;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("coolMethod")) {
            String message = args.getString(0);
            this.coolMethod(message, callbackContext);
            return true;
        } else if (action.equals("duyun.startaudio")) {
            if (talkback == null) {
                String address = args.getString(0);
                try {
                    talkback = new Talkback(address);
                } catch(Exception e) {
                    e.printStackTrace();
                }
                if (talkback.openAudioDevice()) {
                    JSONObject obj = new JSONObject();
                    obj.put("errcode", 0);
                    obj.put("errmsg", "start success");
                    callbackContext.success(obj);
                } else {
                    JSONObject obj = new JSONObject();
                    obj.put("errcode", -1);
                    obj.put("errmsg", "start fail");
                    callbackContext.error(obj);
                }
            } else {
                JSONObject obj = new JSONObject();
                obj.put("errcode", -2);
                obj.put("errmsg", "Already start");
                callbackContext.error(obj);
            }
            return true;
        } else if (action.equals("duyun.stopaudio")) {
            if (talkback != null) {
                talkback.closeAudioOutput ();
                talkback = null;
                JSONObject obj = new JSONObject();
                obj.put("errcode", 0);
                obj.put("errmsg", "success");
                callbackContext.success(obj);
            } else {
                JSONObject obj = new JSONObject();
                obj.put("errcode", -3);
                obj.put("errmsg", "Already stop");
                callbackContext.error(obj);
            }
            return true;
        }
        return false;
    }

    private void coolMethod(String message, CallbackContext callbackContext) {
        if (message != null && message.length() > 0) {
            callbackContext.success(message);
        } else {
            callbackContext.error("Expected one non-empty string argument.");
        }
    }
}
