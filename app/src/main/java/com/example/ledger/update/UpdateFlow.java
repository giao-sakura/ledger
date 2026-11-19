package com.example.ledger.update;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.example.ledger.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;

public final class UpdateFlow {
    private final AppCompatActivity activity;
    private final TextView entry;
    private File pendingApk;
    private boolean busy;
    private boolean cancelDownload;

    public UpdateFlow(AppCompatActivity activity, TextView entry) {
        this.activity = activity;
        this.entry = entry;
    }

    public void attach(View trigger) {
        showIdle();
        trigger.setOnClickListener(v -> check());
    }

    public void onResume() {
        if (pendingApk == null || !canInstall()) {
            return;
        }
        File apk = pendingApk;
        pendingApk = null;
        install(apk);
    }

    private void check() {
        if (busy) {
            return;
        }
        busy = true;
        entry.setText(R.string.update_checking);
        String local = currentVersion();
        UpdateChecker.check(result -> {
            if (activity.isDestroyed()) {
                return;
            }
            busy = false;
            showIdle();
            if (result.kind == UpdateChecker.Result.Kind.FAILED) {
                snack(R.string.update_failed);
                return;
            }
            boolean newer;
            try {
                newer = result.kind == UpdateChecker.Result.Kind.UPDATE
                        && UpdateChecker.compareVersions(local, result.versionName) < 0;
            } catch (IllegalArgumentException ignored) {
                snack(R.string.update_failed);
                return;
            }
            if (!newer) {
                snack(R.string.update_latest);
                return;
            }
            showOffer(result);
        });
    }

    private void showOffer(UpdateChecker.Result result) {
        String notes = result.notes.isEmpty()
                ? activity.getString(R.string.update_notes_empty)
                : result.notes;
        boolean hasApk = result.apkUrl != null && !result.apkUrl.isEmpty();
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity)
                .setTitle(activity.getString(R.string.update_available, result.versionName))
                .setMessage(hasApk ? notes : activity.getString(R.string.update_no_apk))
                .setNegativeButton(R.string.update_later, null);
        if (hasApk) {
            builder.setPositiveButton(R.string.update_now, (dialog, which) -> download(result.apkUrl));
        } else if (result.pageUrl != null && !result.pageUrl.isEmpty()) {
            builder.setPositiveButton(R.string.update_open_release, (dialog, which) -> openPage(result.pageUrl));
        }
        builder.show();
    }

    private void download(String url) {
        busy = true;
        cancelDownload = false;
        AlertDialog dialog = new MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.update_now)
                .setMessage(R.string.update_downloading_unknown)
                .setNegativeButton(R.string.cancel, (d, which) -> cancelDownload = true)
                .setCancelable(false)
                .create();
        dialog.show();
        File destination = new File(new File(activity.getCacheDir(), "updates"), "ledger.apk");
        UpdateChecker.download(url, destination, new UpdateChecker.Progress() {
            @Override
            public void onProgress(int percent) {
                activity.runOnUiThread(() -> {
                    if (dialog.isShowing()) {
                        dialog.setMessage(percent < 0
                                ? activity.getString(R.string.update_downloading_unknown)
                                : activity.getString(R.string.update_downloading, percent));
                    }
                });
            }

            @Override
            public boolean cancelled() {
                return cancelDownload || activity.isDestroyed();
            }
        }, success -> {
            busy = false;
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
            if (activity.isDestroyed() || cancelDownload) {
                return;
            }
            if (!success) {
                snack(R.string.update_download_failed);
                return;
            }
            installOrAsk(destination);
        });
    }

    private void installOrAsk(File apk) {
        if (!canInstall()) {
            pendingApk = apk;
            Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
            intent.setData(Uri.parse("package:" + activity.getPackageName()));
            activity.startActivity(intent);
            return;
        }
        install(apk);
    }

    private void install(File apk) {
        Uri uri = FileProvider.getUriForFile(activity, activity.getPackageName() + ".fileprovider", apk);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "application/vnd.android.package-archive");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            activity.startActivity(intent);
        } catch (RuntimeException ignored) {
            snack(R.string.update_download_failed);
        }
    }

    private void openPage(String page) {
        try {
            activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(page)));
        } catch (RuntimeException ignored) {
            snack(R.string.update_failed);
        }
    }

    private boolean canInstall() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.O
                || activity.getPackageManager().canRequestPackageInstalls();
    }

    private void showIdle() {
        entry.setText(activity.getString(R.string.update_version, currentVersion()));
    }

    private String currentVersion() {
        try {
            PackageInfo info = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0);
            if (info.versionName != null && !info.versionName.isEmpty()) {
                return info.versionName;
            }
        } catch (PackageManager.NameNotFoundException ignored) {
            // Fall through to the manifest version.
        }
        return "1.0";
    }

    private void snack(int message) {
        Snackbar.make(entry, message, Snackbar.LENGTH_LONG).show();
    }
}
