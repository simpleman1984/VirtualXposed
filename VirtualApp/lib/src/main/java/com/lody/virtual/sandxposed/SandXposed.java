package com.lody.virtual.sandxposed;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import com.lody.virtual.IFixerService;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.ipc.ServiceManagerNative;
import com.lody.virtual.helper.utils.OSUtils;
import com.lody.virtual.remote.InstalledAppInfo;
import com.swift.sandhook.SandHook;
import com.swift.sandhook.SandHookConfig;
import com.swift.sandhook.xposedcompat.XposedCompat;

import java.io.File;
import java.util.List;
import java.util.UUID;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

import static com.swift.sandhook.xposedcompat.utils.DexMakerUtils.MD5;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

public class SandXposed {

    private final static String Tag = SandXposed.class.getName();

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
            try {
                XposedCompat.loadModule(module.apkPath, module.getOdexFile().getParent(), module.libPath, XposedBridge.class.getClassLoader());
            } catch (Exception e) {
                Log.e(Tag,"加载xposed模块异常。apkPath:" + module.apkPath + ",odexFile:" + module.getOdexFile() + ",libPath:" + module.libPath);
            }
        }

        XposedCompat.context = context;
        XposedCompat.packageName = packageName;
        XposedCompat.processName = processName;
        XposedCompat.cacheDir = new File(context.getCacheDir(), MD5(processName));
        XposedCompat.classLoader = XposedCompat.getSandHookXposedClassLoader(classLoader, XposedBridge.class.getClassLoader());
        XposedCompat.isFirstApplication = true;

//        XC_LoadPackage.LoadPackageParam loadPackageParam = new XC_LoadPackage.LoadPackageParam(new XposedBridge.CopyOnWriteSortedSet());
//        try {
//            loadPackageParam.classLoader = XposedCompat.getSandHookXposedClassLoader(classLoader, XposedBridge.class.getClassLoader());
//            new RootHider().handleLoadPackage(loadPackageParam);
//        } catch (Throwable throwable) {
//            throwable.printStackTrace();
//        }

        //一个非常好的示例
//        Class clazz = XposedHelpers.findClass("cn.jiguang.ab.b", null);
//        Method m = XposedHelpers.findMethodExact(clazz, "d", Context.class);
//        m.setAccessible(true);
//
//        XposedBridge.hookMethod(m, new XC_MethodHook() {
//            @Override
//            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
//                Log.e("call -> 极光" , (String) param.args[0]);
//                param.setResult(true);
//                super.beforeHookedMethod(param);
//            }
//        });

        //https://github.com/M66B/XPrivacyLua/blob/master/app/src/main/java/eu/faircode/xlua/XLua.java
        findAndHookMethod("android.os.storage.StorageManager", classLoader, "getCacheQuotaBytes", UUID.class, new XC_MethodHook() {
            protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param)
            {
                IFixerService iFixerService = getFixerService();
                UUID uuid = (UUID) param.args[0];
                try {
                    param.setResult(iFixerService.getCacheQuotaBytes(uuid.toString()));
                } catch (RemoteException e) {
                    Log.e(Tag,"调用远程服务StorageManager,uuid:" + uuid + "发生异常~");
                    //返回一个虚假的数字，防止程序不正常
                    param.setResult(100000l);
                }
                Log.d("XposedHook","hook: android.os.storage.StorageManager -> getCacheQuotaBytes");
            }
        });
        //拦截GPS定位查询
        findAndHookMethod("android.location.Location", classLoader, "getLatitude", new XC_MethodHook() {
            protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param)
            {
                double fakeLatitude = XposedModuleProfile.fakeLatitude();
                if(fakeLatitude > 0){
                    param.setResult(fakeLatitude);
                    Log.e("XposedHook","拦截纬度数据查询：" + fakeLatitude);
                } else {
                    Log.e("XposedHook","未拦截纬度数据查询");
                }

            }
        });
        findAndHookMethod("android.location.Location", classLoader, "getLongitude", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                double fakeLongitude = XposedModuleProfile.fakeLongitude();
                if(fakeLongitude > 0){
                    param.setResult(fakeLongitude);
                    Log.e("XposedHook","拦截经度数据查询：" + fakeLongitude);
                } else {
                    Log.e("XposedHook","未拦截经度数据查询");
                }
            }
        });

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

    /**
     * 获取远程fixerService
     * @return
     */
    private static IFixerService getFixerService(){
        final IBinder binder = ServiceManagerNative.getService("FixerService");
        return IFixerService.Stub.asInterface(binder);
    }

}
