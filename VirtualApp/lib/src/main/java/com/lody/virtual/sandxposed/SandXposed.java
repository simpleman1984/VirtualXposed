package com.lody.virtual.sandxposed;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.helper.utils.OSUtils;
import com.lody.virtual.remote.InstalledAppInfo;
import com.swift.sandhook.SandHook;
import com.swift.sandhook.SandHookConfig;
import com.swift.sandhook.xposedcompat.XposedCompat;

import java.io.File;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

import static com.swift.sandhook.xposedcompat.utils.DexMakerUtils.MD5;

public class SandXposed {

    public static void init() {
        SandHookConfig.DEBUG = true;
        SandHookConfig.SDK_INT = OSUtils.getInstance().isAndroidQ() ? 29 : Build.VERSION.SDK_INT;
        SandHookConfig.compiler = SandHookConfig.SDK_INT < Build.VERSION_CODES.O;
        SandHook.passApiCheck();
    }

    public static void injectXposedModule(Context context, String packageName, String processName) {

        if (BlackList.canNotInject(packageName, processName))
            return;

        //读取已经安装的xposed模块列表，并注入对应的代码~
        List<InstalledAppInfo> appInfos = VirtualCore.get().getInstalledApps(InstalledAppInfo.FLAG_XPOSED_MODULE | InstalledAppInfo.FLAG_ENABLED_XPOSED_MODULE);
        ClassLoader classLoader = context.getClassLoader();

        for (InstalledAppInfo module:appInfos) {
            if (TextUtils.equals(packageName, module.packageName)) {
                Log.d("injectXposedModule", "injectSelf : " + processName);
            }
            XposedCompat.loadModule(module.apkPath, module.getOdexFile().getParent(), module.libPath, XposedBridge.class.getClassLoader());
        }

        XposedCompat.context = context;
        XposedCompat.packageName = packageName;
        XposedCompat.processName = processName;
        XposedCompat.cacheDir = new File(context.getCacheDir(), MD5(processName));
        XposedCompat.classLoader = XposedCompat.getSandHookXposedClassLoader(classLoader, XposedBridge.class.getClassLoader());
        XposedCompat.isFirstApplication = true;

        //一个非常好的示例
        //https://github.com/M66B/XPrivacyLua/blob/master/app/src/main/java/eu/faircode/xlua/XLua.java
        //do hook
        XposedHelpers.findAndHookMethod(Activity.class, "onResume", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                Log.e("XposedCompat", "________________________________________________beforeHookedMethod: " + param.method.getName());
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                Log.e("XposedCompat", "________________________________________________afterHookedMethod: " + param.method.getName());
            }
        });

        SandHookHelper.initHookPolicy();
        EnvironmentSetup.init(context, packageName, processName);

        try {
            XposedCompat.callXposedModuleInit();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }



}
