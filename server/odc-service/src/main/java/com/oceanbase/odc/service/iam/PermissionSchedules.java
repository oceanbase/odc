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
package com.oceanbase.odc.service.iam;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.metadb.iam.PermissionRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * @author gaoda.xy
 * @date 2024/1/3 11:22
 */
@Slf4j
@Component
public class PermissionSchedules {

    @Autowired
    private PermissionRepository permissionRepository;

    @Value("${odc.iam.permission.expired-retention-time-seconds:7776000}")
    private Long expiredRetentionTimeSeconds;

    @Scheduled(initialDelay = 10 * 1000L,
            fixedDelayString = "${odc.iam.permission.expired-clean-interval-millis:180000}")
    public void clearExpiredPermission() {
        Date expiredTime = new Date(System.currentTimeMillis() - expiredRetentionTimeSeconds * 1000);
        int count = permissionRepository.deleteByExpireTimeBefore(expiredTime);
        log.info("Clear expired permission, count: {}, expired time: {}", count, expiredTime);
    }

}
