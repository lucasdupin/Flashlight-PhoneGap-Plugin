package nl.xservices.plugins;

import android.content.pm.FeatureInfo;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Build;
import android.util.Log;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import java.io.IOException;

public class Flashlight extends CordovaPlugin {

  private static final String ACTION_AVAILABLE = "available";
  private static final String ACTION_SWITCH_ON = "switchOn";
  private static final String ACTION_SWITCH_OFF = "switchOff";

  private static Boolean capable;
  private Camera mCamera;

  @Override
  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    Log.d("Flashlight", "Plugin Called: " + action);
    try {
      if (action.equals(ACTION_SWITCH_ON)) {
        // When switching on immediately after checking for isAvailable,
        // the release method may still be running, so wait a bit.
        if (mCamera == null) {
          mCamera = Camera.open();
          try {
            //mCamera.setPreviewTexture(new SurfaceTexture(0));
            mCamera.setPreviewDisplay(null);
          } catch (IOException e) {
            e.printStackTrace();
          }
          mCamera.startPreview();
        }
        toggleTorch(true, callbackContext);
        return true;
      } else if (action.equals(ACTION_SWITCH_OFF)) {
        toggleTorch(false, callbackContext);
        boolean soft;
        try {
          soft = args.length() > 0 && args.getBoolean(0);
        } catch (JSONException ex) {
          soft = false;
        }
        if (!soft) {
          releaseCamera();
        }
        return true;
      } else if (action.equals(ACTION_AVAILABLE)) {
        if (capable == null) {
          mCamera = Camera.open();
          capable = isCapable();
          releaseCamera();
        }
        callbackContext.success(capable ? 1 : 0);
        return true;
      } else {
        callbackContext.error("flashlight." + action + " is not a supported function.");
        return false;
      }
    } catch (Exception e) {
      callbackContext.error(e.getMessage());
      return false;
    }
  }

  private boolean isCapable() {
    final PackageManager packageManager = this.cordova.getActivity().getPackageManager();
    for (final FeatureInfo feature : packageManager.getSystemAvailableFeatures()) {
      if (PackageManager.FEATURE_CAMERA_FLASH.equalsIgnoreCase(feature.name)) {
        return true;
      }
    }
    return false;
  }

  private void toggleTorch(boolean switchOn, CallbackContext callbackContext) {
    final Camera.Parameters mParameters = mCamera.getParameters();
    if (isCapable()) {
      if (switchOn) {
        mParameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
        mCamera.setParameters(mParameters);
      } else {
        if (mParameters.getFlashMode().equals(Camera.Parameters.FLASH_MODE_TORCH)) {
          mParameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
          mCamera.setParameters(mParameters);
        }
      }
      callbackContext.success();
    } else {
      callbackContext.error("Device is not capable of using the flashlight. Please test with flashlight.available()");
    }
  }

  private void releaseCamera() {
    // we need to release the camera, so other apps can use it
    mCamera.setPreviewCallback(null);
    mCamera.stopPreview();
    mCamera.release();
    mCamera = null;
  }
}

