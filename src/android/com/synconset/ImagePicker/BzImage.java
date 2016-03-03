package com.synconset;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.Integer;
import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

public class BzImage {
    public BzImage(String fileName, Integer position, Integer rotation) {
        setFileName(fileName);
        setPosition(position);
        setOrientation(rotation);
    }

    private String _fileName;
    public String getFileName() {
        return _fileName;
    }

    public void setFileName(String value) {
        _fileName = value;
    }

    private Integer _position;
    public Integer getPosition() {
        return _position;
    }

    public void setPosition(Integer value) {
        _position = value;
    }

    private Integer _orientation;
    public Integer getOrientation() {
        return _orientation;
    }

    public void setOrientation(Integer value) {
        _orientation = value;
    }

    public String toJSON() {
        try {
            JSONObject obj = new JSONObject();
            obj.put("fileName", getFileName());
            obj.put("orientation", getOrientation());
            obj.put("position", getPosition());
            return obj.toString();
        }
        catch (JSONException) {
            return "{}";
        }
    }

    public static BzImage getByName(ArrayList<BzImage> array, String name) {
        for (Integer i = 0; i < array.size(); i++) {
            if (array.get(i).getFileName().equals(name)) {
                return array.get(i);
            }
        }
        return null;
    }
}