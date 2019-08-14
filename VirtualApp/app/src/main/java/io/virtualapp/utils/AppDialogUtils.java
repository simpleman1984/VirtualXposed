package io.virtualapp.utils;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.text.TextUtils;
import android.widget.Toast;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.ipc.VirtualStorageManager;
import com.lody.virtual.helper.ArtDexOptimizer;
import com.lody.virtual.os.VEnvironment;

import java.io.File;
import java.io.IOException;

import io.virtualapp.R;
import io.virtualapp.abs.ui.VUiKit;
import io.virtualapp.settings.NougatPolicy;

public class AppDialogUtils {

    /**
     * 打开卸载对话框
     * @param context
     * @param name
     * @param pkgName
     * @param userId
     * @param runnable
     */
    public static void showUninstallDialog(Context context, CharSequence name, String pkgName, int userId, Runnable runnable) {
        AlertDialog alertDialog = new AlertDialog.Builder(context)
                .setTitle(com.android.launcher3.R.string.home_menu_delete_title)
                .setMessage(context.getResources().getString(com.android.launcher3.R.string.home_menu_delete_content, name))
                .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                    VirtualCore.get().uninstallPackageAsUser(pkgName, userId);
                    runnable.run();
                })
                .setNegativeButton(android.R.string.no, null)
                .create();
        try {
            alertDialog.show();
        } catch (Throwable ignored) {
        }
    }

    /**
     * 打开修复app对话框
     * @param context
     * @param pkgName
     * @param apkPath
     * @param userId
     */
    public static void showRepairDialog(Context context,String pkgName,String apkPath,int userId) {
        ProgressDialog dialog = new ProgressDialog(context);
        dialog.setTitle(context.getResources().getString(R.string.app_manage_repairing));
        try {
            dialog.setCancelable(false);
            dialog.show();
        } catch (Throwable e) {
            return;
        }

        VUiKit.defer().when(() -> {
            NougatPolicy.fullCompile(context.getApplicationContext());

            if (TextUtils.isEmpty(pkgName) || TextUtils.isEmpty(apkPath)) {
                return;
            }

            // 1. kill package
            VirtualCore.get().killApp(pkgName, userId);

            // 2. backup the odex file
            File odexFile = VEnvironment.getOdexFile(pkgName);
            if (odexFile.delete()) {
                try {
                    ArtDexOptimizer.compileDex2Oat(apkPath, odexFile.getPath());
                } catch (IOException e) {
                    throw new RuntimeException("compile failed.");
                }
            }
        }).done((v) -> {
            dismiss(dialog);
            showAppDetailDialog(context);
        }).fail((v) -> {
            dismiss(dialog);
            Toast.makeText(context, R.string.app_manage_repair_failed_tips, Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * 打开重定向对话框
     * @param context
     * @param pkgName
     * @param userId
     */
    public static void showStorageRedirectDialog(Context context,String pkgName, int userId) {
        boolean virtualStorageEnable;
        try {
            virtualStorageEnable = VirtualStorageManager.get().isVirtualStorageEnable(pkgName, userId);
        } catch (Throwable e) {
            return;
        }

        AlertDialog alertDialog = new AlertDialog.Builder(context)
                .setTitle(virtualStorageEnable ? R.string.app_manage_redirect_off : R.string.app_manage_redirect_on)
                .setMessage(context.getResources().getString(R.string.app_manage_redirect_desc))
                .setPositiveButton(virtualStorageEnable ? R.string.app_manage_redirect_off_confirm : R.string.app_manage_redirect_on_confirm,
                        (dialog, which) -> {
                            try {
                                VirtualStorageManager.get().setVirtualStorageState(pkgName, userId, !virtualStorageEnable);
                            } catch (Throwable ignored) {
                            }
                        })
                .setNegativeButton(android.R.string.no, null)
                .create();
        try {
            alertDialog.show();
        } catch (Throwable ignored) {
        }
    }

    /**
     * 当前app系统对话框
     * @param context
     */
    public static void showAppDetailDialog(Context context) {
        AlertDialog alertDialog = new AlertDialog.Builder(context)
                .setTitle(R.string.app_manage_repair_success_title)
                .setMessage(context.getResources().getString(R.string.app_manage_repair_success_content))
                .setPositiveButton(R.string.app_manage_repair_reboot_now, (dialog, which) -> {
                    String packageName = context.getPackageName();
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", packageName, null));
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    context.startActivity(intent);
                })
                .create();

        alertDialog.setCancelable(false);

        try {
            alertDialog.show();
        } catch (Throwable ignored) {
        }
    }

    private static void dismiss(Dialog dialog) {
        if (dialog == null) {
            return;
        }
        try {
            dialog.dismiss();
        } catch (Throwable ignored) {
        }
    }
}
