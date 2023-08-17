/*
 * Copyright (c) 2023 OceanBase.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.oceanbase.odc.service.datasecurity;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import com.oceanbase.odc.service.datasecurity.model.SensitiveColumnScanningTaskInfo;

import lombok.extern.slf4j.Slf4j;

/**
 * @author gaoda.xy
 * @date 2023/5/25 14:44
 */
@Slf4j
public class SensitiveColumnScanningResultCache {

    private static final Long EXPIRE_TIME_MILLS = 60 * 60 * 1000L;
    private static final Long MAX_EXECUTE_TIME_MILLS = 12 * 60 * 60 * 1000L;

    private static final Map<String, SensitiveColumnScanningTaskInfo> taskId2Info = new ConcurrentHashMap<>();

    private static volatile SensitiveColumnScanningResultCache instance;

    private SensitiveColumnScanningResultCache() {}

    public static SensitiveColumnScanningResultCache getInstance() {
        if (instance == null) {
            synchronized (SensitiveColumnScanningResultCache.class) {
                if (instance == null) {
                    instance = new SensitiveColumnScanningResultCache();
                }
            }
        }
        return instance;
    }

    public void put(String key, SensitiveColumnScanningTaskInfo value) {
        taskId2Info.put(key, value);
    }

    public SensitiveColumnScanningTaskInfo get(String key) {
        return taskId2Info.get(key);
    }

    public boolean containsKey(String key) {
        return taskId2Info.containsKey(key);
    }

    public Iterator<Entry<String, SensitiveColumnScanningTaskInfo>> getAll() {
        return taskId2Info.entrySet().iterator();
    }

    public void clearExpiredTaskInfo() {
        Iterator<Entry<String, SensitiveColumnScanningTaskInfo>> iterator = getAll();
        while (iterator.hasNext()) {
            Map.Entry<String, SensitiveColumnScanningTaskInfo> taskId2Info = iterator.next();
            String taskId = taskId2Info.getKey();
            SensitiveColumnScanningTaskInfo taskInfo = taskId2Info.getValue();
            long createTime = taskInfo.getCreateTime().getTime();
            long currentTime = System.currentTimeMillis();
            if ((currentTime - createTime > MAX_EXECUTE_TIME_MILLS) ||
                    (taskInfo.getStatus().isCompleted() && currentTime - createTime > EXPIRE_TIME_MILLS)) {
                iterator.remove();
                log.info("Sensitive column scanning task info has been expired, taskId={}", taskId);
            }
        }
    }

}
