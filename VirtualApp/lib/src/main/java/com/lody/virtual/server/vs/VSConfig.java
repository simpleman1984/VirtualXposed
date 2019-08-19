package com.lody.virtual.server.vs;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * @author Lody
 *
 * Config of virtual storage.
 *
 */
public class VSConfig implements Parcelable {

    /**
     * 是否开启sd卡，路径重定向；
     * 修改默认为开启~
     */
    public boolean enable;

    /**
     * 虚拟sdk路径
     */
    public String vsPath;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte(this.enable ? (byte) 1 : (byte) 0);
        dest.writeString(this.vsPath);
    }

    public VSConfig() {
    }

    protected VSConfig(Parcel in) {
        this.enable = in.readByte() != 0;
        this.vsPath = in.readString();
    }

    public static final Parcelable.Creator<VSConfig> CREATOR = new Parcelable.Creator<VSConfig>() {
        @Override
        public VSConfig createFromParcel(Parcel source) {
            return new VSConfig(source);
        }

        @Override
        public VSConfig[] newArray(int size) {
            return new VSConfig[size];
        }
    };
}
