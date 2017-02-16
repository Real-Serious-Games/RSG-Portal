package com.rsg.rsgportal;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Handler;
import com.google.gson.JsonParseException;

import org.jdeferred.Deferred;
import org.jdeferred.Promise;
import org.jdeferred.impl.DeferredObject;


/**
 * Created by rorydungan on 19/01/2016.
 */
public class CheckForUpdatesActivity extends AppCompatActivity {

    /**
     * Used to control a two press process for quiting the application with the back button.
     */
    private Boolean doExit = false;

    private AppUpdater Updater;

    private boolean connectedToServer = false;

    private ProgressDialog progressSpinner;

    private IconTextView statusIcon;

    private IconTextView getStatusIcon() {
        if (statusIcon == null) {
            statusIcon = (IconTextView) findViewById(R.id.status_icon);
        }
        return statusIcon;
    }

    private ProgressDialog getProgressSpinner() {
        if (progressSpinner == null) {
            progressSpinner = new ProgressDialog(this, ProgressDialog.STYLE_SPINNER);
        }
        return progressSpinner;
    }

    /**
     * Back button system callback
     */
    @Override
    public void onBackPressed() {
        quit();
    }

    /**
     * when the activity's onResume is called (which also happens the first time the activity is fired up)
     * run the self updater with the updatePortal method. If that is up to date, validate and
     * check for updates.
     */
    @Override
    public void onResume() {
        super.onResume();

        //Show UI stating that the portal is checking for self updates
        getProgressSpinner().setTitle("Contacting server");
        getProgressSpinner().setMessage("RSG Portal is checking for updates...");
        getProgressSpinner().show();
        getProgressSpinner().setCancelable(false);

        updatePortal(new GenericCallback<Boolean>() {
            TextView serverErrorMessage = (TextView) findViewById(R.id.server_disconnection_error);

            @Override
            public void success(Boolean willUpdate) {
                //hide the error message in case it's showing.
                serverErrorMessage.setVisibility(View.GONE);
                connectedToServer = true;
                if (willUpdate) {
                    getProgressSpinner().setTitle("Update available");
                    getProgressSpinner().setMessage("Portal must be updated before using. Please quit and restart the application by pressing the 'back' button on the device.");
                    Notify.Instance().post("Update found. This will now be installed.");
                } else {
                    //portal is up to date, run check for updates
                    Notify.Instance().post("RSG Portal is up to date.");
                    validateAndCheckInstallation();
                }
            }

            @Override
            public void fail(Exception e) {
                getProgressSpinner().dismiss();
                //dismiss UI with an error message
                Notify.Instance().post("An error occurred while checking for an update to the RSG Portal application: " + e.getMessage());
                //set the icon to be the server error icon
                setStatusIconToServerError(getStatusIcon());
                serverErrorMessage.setVisibility(View.VISIBLE);
                connectedToServer = false;
            }
        });
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_check_for_updates);

        ///
        /// Code bootup and initialisaion
        ///

        //set the global application context as the context to be used by the application.
        PortalAppInfo.AppContext = getApplicationContext();

        //Set up classes and dependencies
        RSGPortalLauncher.Instance();

        Updater = RSGPortalLauncher.Updater();

        //set a view in the current context as teh parent view for the Notify SnackBar
        Notify.Instance().SetView(findViewById(R.id.application_title));


        //UI can now access the Updater from the RSGPortalLauncher

        setCurrentVersionText();

        //Set up click listener for the card
        CardView card = (CardView) findViewById(R.id.card_view);
        card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (connectedToServer) {
                    //check the status to see if this should do anything.
                    Intent intent = new Intent(CheckForUpdatesActivity.this, UpdateAppActivity.class);
                    startActivity(intent);
                    setCurrentVersionText();
                }
            }
        });


        checkPermissions();

        getProgressSpinner().setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
                    quit();
                    return true;
                }
                return false;
            }
        });
    }

    /**
     * Simply pass on the callback to the PortalUpdater. If this succeeds with the portal already
     * up to date, then validateAndCheckInstallation() will be called.
     * @param updateCompleteCallback
     */
    private void updatePortal(GenericCallback<Boolean> updateCompleteCallback) {
        Notify.Instance().post("RSG Portal is checking for updates...");
        RSGPortalLauncher.PortalUpdater().update(updateCompleteCallback);
    }

    /**
     * Validate the current installation of the app and if it's not valid, or fails to
     * validate, then mark it as invalid. Otherwise check for updates as we always did.
     */
    private void validateAndCheckInstallation() {
        //check that there is an actual version installed
        if (RSGPortalLauncher.Updater().getCurrentLocalVersionNumber() == 0) {
            checkForUpdates()
                .then(updateAvailable -> {
                    if (updateAvailable) {
                        setStatusIconToWarning(getStatusIcon());
                    } else {
                        setStatusIconToOK(getStatusIcon());
                    }
                })
                .fail(ex -> {
                    setStatusIconToNotOK(getStatusIcon());
                })
                .then(updateAvailable -> {
                    setCurrentVersionText();
                    getProgressSpinner().dismiss();
                });
        } else if (Updater != null) {
            getProgressSpinner().show();
            getProgressSpinner().setTitle("RSG Portal...");
            getProgressSpinner().setMessage("Validating installation");
            Updater.validateCurrentInstallation()
                .then(valid -> {
                    getProgressSpinner().setMessage("Checking for updates");
                    checkForUpdates()
                        .then(updateAvailable -> {
                            if (!valid) {
                                showInstallationValidationFail();
                            } else if (updateAvailable) {
                                setStatusIconToWarning(getStatusIcon());
                                setCurrentVersionText();
                            } else {
                                setStatusIconToOK(getStatusIcon());
                                setCurrentVersionText();
                            }
                        })
                        .fail(ex -> {
                            setStatusIconToNotOK(getStatusIcon());
                        })
                        .then(updateAvailable -> {
                            getProgressSpinner().dismiss();
                        });
                })
                .fail(ex -> {
                    showInstallationValidationFail();
                    getProgressSpinner().dismiss();
                });
        }
    }

    private Promise<Boolean, Exception, Void> checkForUpdates() {
        Deferred<Boolean, Exception, Void> deferred = new DeferredObject<>();

        getProgressSpinner().show();

        Updater.checkForUpdates(new GenericCallback<RemoteApplicationManifest>() {
            @Override
            public void success(RemoteApplicationManifest manifest) {
                if (manifest.getCurrentVersion() == Updater.getCurrentLocalVersionNumber()) {
                    deferred.resolve(false);

                } else {
                    deferred.resolve(true);
                }
            }

            @Override
            public void fail(Exception e) {
                if (e instanceof JsonParseException) {
                    showErrorCheckingForUpdatesDialog("Invalid response from server. \n\nError code 201");
                } else {
                    showErrorCheckingForUpdatesDialog(e.getMessage());
                }

                deferred.reject(e);
            }
        });

        return deferred.promise();
    }

    /**
     * Function for handling the process of quiting the application. This allows for
     * accidental quits, giving the user 3 seconds to confirm their intent to quit by
     * performing whatever function called this again. Most likely the back button.
     * http://stackoverflow.com/questions/20591959/android-quit-application-when-press-back-button
     */
    private void quit() {
        if (doExit) {
            finish();
            System.exit(0);
        }
        else {
            Toast.makeText(this, "Press back again to Quit", Toast.LENGTH_SHORT).show();
            doExit = true;
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    doExit = false;
                }
            }, 3 * 1000);
        }
    }

    private void setCurrentVersionText() {
        // Show currently installed version
        int currentlyInstalledVersion = RSGPortalLauncher.Updater().getCurrentLocalVersionNumber();
        if (currentlyInstalledVersion == 0) {
            setCurrentVersionText("Not installed");
        } else {
            setCurrentVersionText("Currently version: " + currentlyInstalledVersion);
        }
    }

    private void showInstallationValidationFail() {
        TextView currentVersionText = (TextView) findViewById(R.id.status_text);
        currentVersionText.setText("Invalid installation. Please uninstall.");
        setStatusIconToNotOK(getStatusIcon());
    }

    private void setCurrentVersionText(String text) {
        // Show currently installed version
        TextView currentVersionText = (TextView) findViewById(R.id.status_text);
        currentVersionText.setText(text);
    }

    private void setStatusIconToOK(IconTextView view) {
        view.setText(R.string.icon_ok);
        view.setTextColor(getResources().getColor(R.color.icon_ok));
    }

    private void setStatusIconToNotOK(IconTextView view) {
        view.setText(R.string.icon_not_ok);
        view.setTextColor(getResources().getColor(R.color.icon_error));
    }

    private void setStatusIconToWarning(IconTextView view) {
        view.setText(R.string.icon_warning);
        view.setTextColor(getResources().getColor(R.color.icon_warning));
    }

    private void setStatusIconToServerError(IconTextView view) {
        view.setText(R.string.icon_warning);
        view.setTextColor(getResources().getColor(R.color.icon_server_error));
    }

    private void showNoUpdateAvailableDialog() {
        new AlertDialog.Builder(this)
                .setTitle("No updates")
                .setMessage("No updates are available to download")
                .create()
                .show();
    }

    private void showErrorCheckingForUpdatesDialog(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Could not check for updates")
                .setMessage(message)
                .create()
                .show();
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
            }
        }
    }
}
