package com.mrspark.cordova.plugin;

import com.google.android.gms.tasks.Task;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.install.model.InstallStatus;
import com.google.android.play.core.install.InstallState;
import com.google.android.play.core.install.InstallStateUpdatedListener;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;

import android.content.Context;
import com.google.android.material.snackbar.Snackbar;

import android.util.Log;
import android.widget.FrameLayout;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

public class UpdatePlugin extends CordovaPlugin {
    public int REQUEST_CODE = 7;
    private static String IN_APP_UPDATE_TYPE = "FLEXIBLE";
    private static Integer DAYS_FOR_FLEXIBLE_UPDATE = 0;
    private static Integer DAYS_FOR_IMMEDIATE_UPDATE = 0;
    private static AppUpdateManager appUpdateManager;
    private final String TAG = "UpdatePlugin";

    public void onStateUpdate(final InstallState state) {
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            // After the update is downloaded, show a notification
            // and request user confirmation to restart the app.
            popupSnackbarForCompleteUpdate();
        }
    }

    public void checkForUpdate(final int updateType, final AppUpdateInfo appUpdateInfo) {
        Log.d(TAG, "startUpdateFlowForResult");
        if (updateType == 0) {
            IN_APP_UPDATE_TYPE = "FLEXIBLE";
            InstallStateUpdatedListener listener = this::onStateUpdate;
            appUpdateManager.registerListener(listener);
        } else {
            IN_APP_UPDATE_TYPE = "IMMEDIATE";
        }
        try {
            appUpdateManager.startUpdateFlowForResult(appUpdateInfo, updateType, cordova.getActivity(),
                    REQUEST_CODE);
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    /* Displays the snackbar notification and call to action. */
    private void popupSnackbarForCompleteUpdate() {
        final Snackbar snackbar = Snackbar.make((FrameLayout) webView.getView().getParent(), "An update has just been downloaded.",
                Snackbar.LENGTH_INDEFINITE);
        snackbar.setAction("RESTART", view -> appUpdateManager.completeUpdate());
        snackbar.show();
    }

    private JSONObject getAndroidArgs(JSONArray args) {
        try {
            final JSONObject argument = args.getJSONObject(0);
            return argument.getJSONObject("ANDROID");
        } catch (final JSONException e) {
            e.printStackTrace();
            return new JSONObject();
        }
    }

    @Override
    public boolean execute(final String action, final JSONArray args, final CallbackContext callbackContext) {
        if (!action.equals("update")) {
            callbackContext.error("\"" + action + "\" is not a recognized action.");
            return false;
        }

        final Context context = this.cordova.getContext();
        appUpdateManager = AppUpdateManagerFactory.create(context);
        final Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();
        final JSONObject androidArgs = getAndroidArgs(args);
        try {
            final String type = androidArgs.getString("type");
            switch (type) {
                case "MIXED":
                    DAYS_FOR_FLEXIBLE_UPDATE = Integer.parseInt(androidArgs.getString("flexibleUpdateStalenessDays"));
                    DAYS_FOR_IMMEDIATE_UPDATE = Integer.parseInt(androidArgs.getString("immediateUpdateStalenessDays"));
                    break;
                case "FLEXIBLE":
                    DAYS_FOR_FLEXIBLE_UPDATE = Integer.parseInt(androidArgs.getString("stallDays"));
                    DAYS_FOR_IMMEDIATE_UPDATE = 999999999;
                    break;
                case "IMMEDIATE":
                    DAYS_FOR_FLEXIBLE_UPDATE = 999999999;
                    DAYS_FOR_IMMEDIATE_UPDATE = Integer.parseInt(androidArgs.getString("stallDays"));
                    break;
            }
            appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
                boolean updateAvailability = appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE;
                Log.d(TAG, "updateAvailability: " + updateAvailability);
                if (updateAvailability) {
                    int stalenessDays = 0;
                    if (appUpdateInfo.clientVersionStalenessDays() != null) {
                        stalenessDays = appUpdateInfo.clientVersionStalenessDays();
                    }
                    Log.d(TAG, "stalenessDays: " + appUpdateInfo.clientVersionStalenessDays());
                    if (stalenessDays >= DAYS_FOR_IMMEDIATE_UPDATE && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                        checkForUpdate(AppUpdateType.IMMEDIATE, appUpdateInfo);
                    } else if (stalenessDays >= DAYS_FOR_FLEXIBLE_UPDATE && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                        checkForUpdate(AppUpdateType.FLEXIBLE, appUpdateInfo);
                    } else {
                        Log.d(TAG, "Not proceed update now.");
                    }
                }
                PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, updateAvailability);
                callbackContext.sendPluginResult(pluginResult);
            });
        } catch (final Exception e) {
            e.printStackTrace();
            callbackContext.error(e.getMessage());
        }
        return true;
    }

    @Override
    public void onResume(final boolean multitasking) {
        super.onResume(multitasking);
        appUpdateManager
                .getAppUpdateInfo()
                .addOnSuccessListener(
                        appUpdateInfo -> {
                            if (IN_APP_UPDATE_TYPE.equals("FLEXIBLE")
                                    && appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                                popupSnackbarForCompleteUpdate();
                            }

                            if (appUpdateInfo
                                    .updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                                // If an in-app update is already running, resume the update.
                                try {
                                    checkForUpdate(AppUpdateType.IMMEDIATE, appUpdateInfo);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });
    }
}