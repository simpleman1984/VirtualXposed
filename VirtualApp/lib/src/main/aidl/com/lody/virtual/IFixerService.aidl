package com.lody.virtual;

interface IFixerService {
    /**
     * 查询缓存文件夹，使用尺寸 ~
     **/
    long getCacheQuotaBytes(String uuid);
}
