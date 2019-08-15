package io.virtualapp.settings;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.ipc.VirtualStorageManager;
import com.lody.virtual.remote.InstalledAppInfo;
import com.lody.virtual.sandxposed.XposedModuleProfile;

import java.util.ArrayList;
import java.util.List;

import io.island.R;
import io.virtualapp.abs.ui.VActivity;
import io.virtualapp.abs.ui.VUiKit;
import io.virtualapp.glide.GlideUtils;
import io.virtualapp.utils.AppDialogUtils;

public class ModuleMannageActivity  extends VActivity {

    private ListView mListView;
    private List<ModuleMannageActivity.AppManageInfo> mInstalledApps = new ArrayList<>();
    private ModuleMannageActivity.AppManageAdapter mAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        mListView = (ListView) findViewById(R.id.list);
        mAdapter = new ModuleMannageActivity.AppManageAdapter();
        mListView.setAdapter(mAdapter);

        mListView.setOnItemClickListener((parent, view, position, id) -> {
            ModuleMannageActivity.AppManageInfo appManageInfo = mInstalledApps.get(position);
            showContextMenu(appManageInfo, view);
        });
        loadAsync();
    }

    private void loadAsync() {
        VUiKit.defer().when(this::loadApp).done((v) -> mAdapter.notifyDataSetChanged());
    }

    private void loadApp() {
        List<ModuleMannageActivity.AppManageInfo> ret = new ArrayList<>();
        List<InstalledAppInfo> installedApps = VirtualCore.get().getInstalledApps(InstalledAppInfo.FLAG_XPOSED_MODULE);
        PackageManager packageManager = getPackageManager();
        for (InstalledAppInfo installedApp : installedApps) {
            int[] installedUsers = installedApp.getInstalledUsers();
            for (int installedUser : installedUsers) {
                ModuleMannageActivity.AppManageInfo info = new ModuleMannageActivity.AppManageInfo();
                info.userId = installedUser;
                ApplicationInfo applicationInfo = installedApp.getApplicationInfo(installedUser);
                info.name = applicationInfo.loadLabel(packageManager);
//                info.icon = applicationInfo.loadIcon(packageManager);  //Use Glide to load icon async
                info.pkgName = installedApp.packageName;
                info.path = applicationInfo.sourceDir;

                if(installedApp.xposedModule != null){
                    info.xposedDesc = installedApp.xposedModule.desc;
                    info.xposedMinVersion = installedApp.xposedModule.minVersion;
                }
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
        public ModuleMannageActivity.AppManageInfo getItem(int position) {
            return mInstalledApps.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ModuleMannageActivity.ViewHolder holder;
            if (convertView == null) {
                holder = new ModuleMannageActivity.ViewHolder(ModuleMannageActivity.this, parent);
                convertView = holder.root;
                convertView.setTag(holder);
            } else {
                holder = (ModuleMannageActivity.ViewHolder) convertView.getTag();
            }

            ModuleMannageActivity.AppManageInfo item = getItem(position);

            holder.label.setText(item.getName());
            //当前package是否启用
            holder.checkBox.setChecked(XposedModuleProfile.isModuleEnable(item.pkgName));
            //设置当前package的启用状态
            holder.checkBox.setOnClickListener((v) -> XposedModuleProfile.enableModule(item.pkgName, ((CheckBox)v).isChecked()));
            holder.description_name.setText(item.xposedDesc);
            holder.version_name.setText(item.xposedMinVersion+"");

            if (VirtualCore.get().isOutsideInstalled(item.pkgName)) {
                GlideUtils.loadInstalledPackageIcon(getContext(), item.pkgName, holder.icon, android.R.drawable.sym_def_app_icon);
            } else {
                GlideUtils.loadPackageIconFromApkFile(getContext(), item.path, holder.icon, android.R.drawable.sym_def_app_icon);
            }

//            holder.button.setOnClickListener(v -> showContextMenu(item, v));

            return convertView;
        }
    }

    private void showContextMenu(ModuleMannageActivity.AppManageInfo appManageInfo, View anchor) {
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
                    AppDialogUtils.showUninstallDialog(getContext(),appManageInfo.name,appManageInfo.pkgName,appManageInfo.userId,()->loadAsync());
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
        TextView version_name;
        TextView description_name;
//        ImageView button;
        CheckBox checkBox;

        View root;

        ViewHolder(Context context, ViewGroup parent) {
            root = LayoutInflater.from(context).inflate(R.layout.item_module_manage, parent, false);
            icon = root.findViewById(R.id.item_app_icon);
            label = root.findViewById(R.id.item_app_name);
//            button = root.findViewById(R.id.item_app_button);
            checkBox = root.findViewById(R.id.item_app_checkbox);
            version_name = root.findViewById(R.id.version_name);
            description_name = root.findViewById(R.id.description);
        }
    }

    static class AppManageInfo {
        CharSequence name;
        int userId;
        Drawable icon;
        String pkgName;
        String path;
        String xposedDesc;
        int xposedMinVersion;

        CharSequence getName() {
            if (userId == 0) {
                return name;
            } else {
                return name + "[" + (userId + 1) + "]";
            }
        }
    }
}
