package com.lody.virtual.server;

import android.content.Context;
import android.os.storage.StorageManager;

import com.lody.virtual.IFixerService;

import java.util.UUID;

/**
 * 部分服务，通过反向代理的方式调用有问题；
 * 通过该Service逐个进行远程调用，通过Hook的方式，进行fixed
 */
public class FixerService extends IFixerService.Stub{

    private Context context;

    public FixerService(Context context){
        this.context = context;
    }

    @Override
    public long getCacheQuotaBytes(String uuid) {
        try{
            StorageManager storageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
            long size = storageManager.getCacheQuotaBytes(UUID.fromString(uuid));
            return size;
        }catch (Exception e){
            e.printStackTrace();
            return  0L;
        }
    }

}
