package io.virtualapp.settings;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.ipc.VirtualStorageManager;
import com.lody.virtual.helper.ArtDexOptimizer;
import com.lody.virtual.os.VEnvironment;
import com.lody.virtual.remote.InstalledAppInfo;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.island.R;
import io.virtualapp.abs.ui.VActivity;
import io.virtualapp.abs.ui.VUiKit;
import io.virtualapp.glide.GlideUtils;
import io.virtualapp.utils.AppDialogUtils;

/**
 * @author weishu
 * @date 18/2/15.
 */

public class AppManageActivity extends VActivity {

    private ListView mListView;
    private List<AppManageInfo> mInstalledApps = new ArrayList<>();
    private AppManageAdapter mAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        mListView = (ListView) findViewById(R.id.list);
        mAdapter = new AppManageAdapter();
        mListView.setAdapter(mAdapter);

        mListView.setOnItemClickListener((parent, view, position, id) -> {
            AppManageInfo appManageInfo = mInstalledApps.get(position);
            showContextMenu(appManageInfo, view);
        });
        loadAsync();
    }

    private void loadAsync() {
        VUiKit.defer().when(this::loadApp).done((v) -> mAdapter.notifyDataSetChanged());
    }

    private void loadApp() {

        List<AppManageInfo> ret = new ArrayList<>();
        List<InstalledAppInfo> installedApps = VirtualCore.get().getInstalledApps(0);
        PackageManager packageManager = getPackageManager();
        for (InstalledAppInfo installedApp : installedApps) {
            int[] installedUsers = installedApp.getInstalledUsers();
            for (int installedUser : installedUsers) {
                AppManageInfo info = new AppManageInfo();
                info.userId = installedUser;
                ApplicationInfo applicationInfo = installedApp.getApplicationInfo(installedUser);
                info.name = applicationInfo.loadLabel(packageManager);
//                info.icon = applicationInfo.loadIcon(packageManager);  //Use Glide to load icon async
                info.pkgName = installedApp.packageName;
                info.path = applicationInfo.sourceDir;
                ret.add(info);
            }
        }
        mInstalledApps.clear();
        mInstalledApps.addAll(ret);
    }

    class AppManageAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return mInstalledApps.size();
        }

        @Override
        public AppManageInfo getItem(int position) {
            return mInstalledApps.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                holder = new ViewHolder(AppManageActivity.this, parent);
                convertView = holder.root;
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            AppManageInfo item = getItem(position);

            holder.label.setText(item.getName());

            if (VirtualCore.get().isOutsideInstalled(item.pkgName)) {
                GlideUtils.loadInstalledPackageIcon(getContext(), item.pkgName, holder.icon, android.R.drawable.sym_def_app_icon);
            } else {
                GlideUtils.loadPackageIconFromApkFile(getContext(), item.path, holder.icon, android.R.drawable.sym_def_app_icon);
            }

            holder.button.setOnClickListener(v -> showContextMenu(item, v));

            return convertView;
        }
    }

    private void showContextMenu(AppManageInfo appManageInfo, View anchor) {
        if (appManageInfo == null) {
            return;
        }
        PopupMenu popupMenu = new PopupMenu(this, anchor);
        popupMenu.inflate(R.menu.app_manage_menu);
        MenuItem redirectMenu = popupMenu.getMenu().findItem(R.id.action_redirect);

        try {
            final String packageName = appManageInfo.pkgName;
            final int userId = appManageInfo.userId;
            boolean virtualStorageEnable = VirtualStorageManager.get().isVirtualStorageEnable(packageName, userId);
            redirectMenu.setTitle(virtualStorageEnable ? R.string.app_manage_redirect_off : R.string.app_manage_redirect_on);
        } catch (Throwable e) {
            redirectMenu.setVisible(false);
        }

        popupMenu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.action_uninstall:
                    AppDialogUtils.showUninstallDialog(getContext(),appManageInfo.name,appManageInfo.pkgName,appManageInfo.userId, this::loadAsync);
                    break;
                case R.id.action_repair:
                    AppDialogUtils.showRepairDialog(getContext(),appManageInfo.pkgName,appManageInfo.path,appManageInfo.userId);
                    break;
                case R.id.action_redirect:
                    AppDialogUtils.showStorageRedirectDialog(getContext(),appManageInfo.pkgName,appManageInfo.userId);
                    break;
            }
            return false;
        });
        try {
            popupMenu.show();
        } catch (Throwable ignored) {
        }
    }

    static class ViewHolder {
        ImageView icon;
        TextView label;
        ImageView button;

        View root;

        ViewHolder(Context context, ViewGroup parent) {
            root = LayoutInflater.from(context).inflate(R.layout.item_app_manage, parent, false);
            icon = root.findViewById(R.id.item_app_icon);
            label = root.findViewById(R.id.item_app_name);
            button = root.findViewById(R.id.item_app_button);
        }
    }

    static class AppManageInfo {
        CharSequence name;
        int userId;
        Drawable icon;
        String pkgName;
        String path;

        CharSequence getName() {
            if (userId == 0) {
                return name;
            } else {
                return name + "[" + (userId + 1) + "]";
            }
        }
    }

}
