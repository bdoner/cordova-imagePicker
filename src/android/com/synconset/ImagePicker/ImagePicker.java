/**
 * An Image Picker Plugin for Cordova/PhoneGap.
 */
package com.synconset;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

public class BzImage {
    public BzImage(String fileName, Integer position, Integer rotation) {
        setFileName(fileName);
        setPosition(position);
        setRotation(rotation);
    }

    private String _fileName;
    public getFileName() {
        return _fileName;
    }

    public setFileName(String value) {
        _fileName = value
    }

    private Integer _position;
    public getPosition() {
        return _position;
    }

    public setPosition(Integer value) {
        _position = value;
    }

    private Integer _orientation;
    public getOrientation() {
        return _orientation;
    }

    public setOrientation(Integer value) {
        _orientation = value;
    }

    public static BzImage getByName(ArrayList<BzImage> array, String name) {
        for (Integer i = 0; i < array.size(); i++) {
            if (array[i].getFileName().equals(name)) {
                return array[i];
            }
        }
        return null;
    }
}

public class ImagePicker extends CordovaPlugin {
    public static String TAG = "ImagePicker";

    private CallbackContext callbackContext;
    private JSONObject params;

    public boolean execute(String action, final JSONArray args, final CallbackContext callbackContext) throws JSONException {
        this.callbackContext = callbackContext;
        this.params = args.getJSONObject(0);
        if (action.equals("getPictures")) {
            Intent intent = new Intent(cordova.getActivity(), MultiImageChooserActivity.class);
            int max = -1;
            int desiredWidth = 0;
            int desiredHeight = 0;
            int quality = 100;
            if (this.params.has("maximumImagesCount")) {
                max = this.params.getInt("maximumImagesCount");
            }
            if (this.params.has("width")) {
                desiredWidth = this.params.getInt("width");
            }
            if (this.params.has("height")) {
                desiredWidth = this.params.getInt("height");
            }
            if (this.params.has("quality")) {
                quality = this.params.getInt("quality");
            }
            intent.putExtra("MAX_IMAGES", max);
            intent.putExtra("WIDTH", desiredWidth);
            intent.putExtra("HEIGHT", desiredHeight);
            intent.putExtra("QUALITY", quality);
            if (this.cordova != null) {
                this.cordova.startActivityForResult((CordovaPlugin) this, intent, 0);
            }
        }
        return true;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK && data != null) {
            ArrayList<BzImage> fileNames = data.getStringArrayListExtra("MULTIPLEFILENAMES");
            JSONArray res = new JSONArray(fileNames);
            this.callbackContext.success(res);
        } else if (resultCode == Activity.RESULT_CANCELED && data != null) {
            String error = data.getStringExtra("ERRORMESSAGE");
            this.callbackContext.error(error);
        } else if (resultCode == Activity.RESULT_CANCELED) {
            JSONArray res = new JSONArray();
            this.callbackContext.success(res);
        } else {
            this.callbackContext.error("No images selected");
        }
    }
}