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
package com.oceanbase.odc.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.service.config.SystemConfigService;
import com.oceanbase.odc.service.datasecurity.SensitiveColumnScanningResultCache;

import lombok.extern.slf4j.Slf4j;

/**
 * @author: liuyizhuo.lyz
 * @date: 2024/12/25
 */
@Component
@Slf4j
public class Schedulers {

    private static final long REFRESH_CONFIG_RATE_MILLIS = 3 * 60 * 1000L;
    private static final int SHORT_VALIDATE_INTERVAL_MS = 10 * 1000;

    @Autowired
    private SystemConfigService systemConfigService;

    @Scheduled(fixedDelay = REFRESH_CONFIG_RATE_MILLIS)
    public void refreshSysConfig() {
        try {
            systemConfigService.refresh();
        } catch (Exception e) {
            log.error("refresh sys config failed", e);
        }
    }

    @Scheduled(fixedRate = SHORT_VALIDATE_INTERVAL_MS)
    public void clearExpiredTask() {
        SensitiveColumnScanningResultCache.getInstance().clearExpiredTaskInfo();
    }

}
