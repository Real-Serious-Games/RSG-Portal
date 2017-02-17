package com.rsg.rsgportal;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.gson.JsonParseException;

import java.text.DecimalFormat;
import java.util.ArrayList;

import layout.DownloadingUpdateFragment;
import layout.InstallingUpdateFragment;
import layout.UpdateAvailableFragment;

public class UpdateAppActivity extends AppCompatActivity implements
        UpdateAvailableFragment.OnFragmentInteractionListener,
        DownloadingUpdateFragment.OnFragmentInteractionListener,
        InstallingUpdateFragment.OnFragmentInteractionListener {

    private TextView titleText;

    AppUpdater updater;

    private static final String TAG = "UpdateAppActivity";

    private int versionToInstall;

    private enum State {
        DEFAULT,
        INSTALLING_APK,
        UNINSTALLING_APK
    }

    private State currentState = State.DEFAULT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download_update);
        getSupportActionBar().setElevation(0f);

        titleText = (TextView) findViewById(R.id.title_text);

        updater = RSGPortalLauncher.Instance().Updater();

        versionToInstall = updater.getKnownRemoteAppManifest().getCurrentVersion();

        ArrayList<String> availableVersions = new ArrayList<>();
        for(int version : updater.getKnownRemoteAppManifest().getAvailableVersions()) {
            availableVersions.add(Integer.toString(version));
        }

        //TODO: show correct download size
        setFragment(UpdateAvailableFragment.newInstance(
                Integer.toString(updater.getCurrentLocalVersionNumber()),
                Integer.toString(versionToInstall),
                availableVersions,
                "50MB"), false);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (currentState == State.INSTALLING_APK) {
            // Installer finished
            alertInstallFinished();

            currentState = State.DEFAULT;
        } else if (currentState == State.UNINSTALLING_APK) {
            // Finished uninstalling APK, time to install again.
            installApp();
        }
    }

    /**
     * Show an alert notifying the user that we've finished installing.
     */
    private void alertInstallFinished() {
        new AlertDialog.Builder(UpdateAppActivity.this)
            .setTitle(getString(R.string.install_complete))
            .setMessage(getString(R.string.finished_installing_app))
            .setOnDismissListener(dialog -> this.finish())
            .setNeutralButton(getString(R.string.ok), (dialog, which) -> this.finish())
            .create()
            .show();
    }

    /**
     * Set the current fragment
     * @param fragment
     */
    private void setFragment(Fragment fragment, boolean slide) {
        // Insert the fragment by replacing any existing fragment
        FragmentManager fragmentManager = getSupportFragmentManager();

        FragmentTransaction ft = fragmentManager.beginTransaction();
        if (slide) {
            ft.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left);
        }
        ft.replace(R.id.content_frame, fragment);
        ft.commit();
    }

    @Override
    public void onDownloadClicked() {
        setFragment(DownloadingUpdateFragment.newInstance(), true);
        setTitle(getString(R.string.downloading_update));

        updater.downloadUpdates(versionToInstall,
                new GenericCallback<Integer>() {
                    @Override
                    public void success(Integer data) {

                    }

                    @Override
                    public void fail(Exception e) {
                    }
                }, new ProgressFeedbackCallback() {
                    private ProgressBar progressBar;
                    private TextView progressText;

                    private DecimalFormat progressTextFormat = new DecimalFormat("#.#");

                    @Override
                    public void success() {
                        findViewById(R.id.install_button).setEnabled(true);

                        setTitle(getString(R.string.ready_to_install));
                    }

                    @Override
                    public void fail(Exception ex) {
                        if (ex instanceof JsonParseException) {
                            showFailureDialog(getString(R.string.could_not_download_update), getString(R.string.invalid_response_from_server_202));
                        } else {
                            showFailureDialog(getString(R.string.could_not_download_update), ex.getMessage());
                        }
                        Log.e(TAG, ex.getMessage(), ex);
                    }

                    @Override
                    public void progress(float progress) {
                        // Lazily get widgets
                        if (progressBar == null) {
                            progressBar = (ProgressBar) findViewById(R.id.progress_bar);
                        }
                        if (progressText == null) {
                            progressText = (TextView) findViewById(R.id.progress_text);
                        }

                        progressBar.setIndeterminate(false);
                        progressBar.setMax(2000);
                        progressBar.setProgress(Math.round(progress * 2000f));

                        progressText.setText(progressTextFormat.format(progress * 100f) + "%");
                    }
                });
    }

    @Override
    public void onVersionSelectionChanged(int newVersion) {
        versionToInstall = updater.getKnownRemoteAppManifest().getAvailableVersions().get(newVersion);
    }

    @Override
    public void onCancelDownloadingClicked() {
        // TODO
    }

    @Override
    public void onCancelInstallingClicked() {
        // TODO
    }

    @Override
    public void onInstallClicked() {

        setFragment(InstallingUpdateFragment.newInstance(), true);
        setTitle(getString(R.string.installing_update));

        if (updater.uninstallIfNecessary(versionToInstall)) {
            currentState = State.UNINSTALLING_APK;
        } else {
            installApp();
        }

    }

    private void installApp() {
        updater.installApp(versionToInstall, new GenericProgressFeedbackCallback<Boolean>() {
            private ProgressBar progressBar;
            private TextView progressText;

            private DecimalFormat progressTextFormat = new DecimalFormat("#.#");

            @Override
            public void success(Boolean installing) {
                // If necessary, wait for the installer to finish.
                if (installing) {
                    currentState = State.INSTALLING_APK;
                } else {
                    // APK was already installed and is correct version, so finish the installer here.
                    alertInstallFinished();

                    currentState = State.DEFAULT;
                }
            }

            @Override
            public void fail(Exception ex) {
                showFailureDialog(getString(R.string.could_not_install_package), ex.getMessage());

                Log.e(TAG, ex.getMessage(), ex);

                currentState = State.DEFAULT;
            }

            @Override
            public void progress(float progress) {
                // Lazily get widgets
                if (progressBar == null) {
                    progressBar = (ProgressBar) findViewById(R.id.progress_bar);
                }
                if (progressText == null) {
                    progressText = (TextView) findViewById(R.id.progress_text);
                }

                progressBar.setIndeterminate(false);
                progressBar.setMax(2000);
                progressBar.setProgress(Math.round(progress * 2000f));

                progressText.setText(progressTextFormat.format(progress * 100f) + "%");
            }
        });
    }

    /**
     * Set the title text
     */
    private void setTitle(final String text) {
        // Fade out
        final int animationDuration = getResources().getInteger(android.R.integer.config_shortAnimTime);

        Animation anim = new AlphaAnimation(1f, 0f);
        anim.setDuration(animationDuration);
        anim.setFillAfter(true);

        // Once the animation has finished we can set the text and fade back in.
        anim.setAnimationListener(getTitleFadeOutListener(text, animationDuration));

        titleText.startAnimation(anim);
    }

    @NonNull
    private Animation.AnimationListener getTitleFadeOutListener(final String text, final int animationDuration) {
        return new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                // Set the new text
                titleText.setText(text);

                // Animate back in
                Animation anim = new AlphaAnimation(0f, 1f);
                anim.setDuration(animationDuration);
                anim.setFillAfter(true);
                titleText.startAnimation(anim);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        };
    }

    private void showFailureDialog(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setNeutralButton(getString(R.string.close), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        UpdateAppActivity.this.finish();
                    }
                })
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        UpdateAppActivity.this.finish();
                    }
                })
                .create()
                .show();
    }
}
