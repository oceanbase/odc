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
package com.oceanbase.odc.service.lab;

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.metadb.connection.ConnectionEntity;
import com.oceanbase.odc.service.config.SystemConfigService;
import com.oceanbase.odc.service.config.model.Configuration;
import com.oceanbase.odc.service.connection.ConnectionSessionHistoryService;
import com.oceanbase.odc.service.session.SessionLimitService;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2022/1/5 下午8:41
 * @Description: [Lab 模块定时调度任务]
 */

@Slf4j
@Component
@ConditionalOnProperty(value = "odc.lab.enabled", havingValue = "true")
public class LabSchedules {
    @Autowired
    private ResourceService resourceService;

    @Autowired
    private ConnectionSessionHistoryService connectionSessionHistoryService;

    @Autowired
    private SystemConfigService systemConfigService;

    @Autowired
    private LabDataSourceFactory labDataSourceFactory;

    private String lastDataSourceConfig;

    @Autowired
    private SessionLimitService sessionLimitService;

    @Scheduled(fixedDelayString = "${odc.lab.schedule.fix-delay-millis:7200000}")
    public void revokeLabResource() {
        List<ConnectionEntity> entities = connectionSessionHistoryService.listInactiveConnections(null);
        if (CollectionUtils.isEmpty(entities)) {
            return;
        }
        log.info("try to revoke lab resource, connectionHistoryEntity count={}", entities.size());
        for (ConnectionEntity entity : entities) {
            try {
                log.info("start revoke resource, connectionId={}", entity.getId());
                resourceService.revokeResource(entity.getId());
                log.info("revoke resource successfully, connectionId={}", entity.getId());
            } catch (Exception ex) {
                log.warn("revoke resource failed, connectionId={}", entity.getId(), ex);
            }
        }
    }

    @Scheduled(fixedDelayString = "${odc.lab.schedule.update-session-permission-delay-millis:1000}")
    public void updateSessionPermissionTask() {
        sessionLimitService.revokeInactiveUser();
        sessionLimitService.pollUserFromWaitQueue();
    }

    @Scheduled(fixedDelay = 1 * 60 * 1000L)
    public void refreshLabDataSource() {
        List<Configuration> current = systemConfigService.queryByKeyPrefix("odc.lab.ob.connection.key");
        Verify.notEmpty(current, "currentLabDataSourceConfig");
        String currentConfig = current.get(0).getValue();
        if (!StringUtils.equals(currentConfig, lastDataSourceConfig)) {
            lastDataSourceConfig = currentConfig;
            labDataSourceFactory.refreshDataSource(currentConfig);
        }
    }

}
