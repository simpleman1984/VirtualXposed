package com.lody.virtual.sandxposed;

import android.content.Context;
import android.content.SharedPreferences;

import com.lody.virtual.client.core.VirtualCore;


public class XposedModuleProfile {

    private static SharedPreferences config;

    static {
        config = VirtualCore.get().getContext().getSharedPreferences("xposed_config", Context.MODE_PRIVATE);
    }

    public static void enbaleXposed(boolean enbale) {
        config.edit().putBoolean("xposed_enable", enbale).commit();
    }

    public static boolean isXposedEnable() {
        return config.getBoolean("xposed_enable", true);
    }

    public static void enableModule(String pkg, boolean enable) {
        config.edit().putBoolean(pkg, enable).apply();
    }

    public static boolean isModuleEnable(String pkg) {
        return config.getBoolean(pkg, true);
    }

    public static void fakeLatitude(double latitude){
        config.edit().putString("fakeLatitude", latitude + "").apply();
    }

    public  static  double fakeLatitude(){
        String saveValue = config.getString("fakeLatitude", "-1");
        return Double.parseDouble(saveValue);
    }

    public static void fakeLongitude(double longitude){
        config.edit().putString("fakeLongitude", longitude + "").apply();
    }

    public  static  double fakeLongitude(){
        String saveValue =  config.getString("fakeLongitude", "-1");
        return Double.parseDouble(saveValue);
    }

}