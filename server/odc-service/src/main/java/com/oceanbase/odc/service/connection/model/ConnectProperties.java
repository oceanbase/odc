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
package com.oceanbase.odc.service.connection.model;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

import com.google.common.collect.Sets;

import lombok.Data;

/**
 * @Author: Lebie
 * @Date: 2022/4/8 上午10:55
 * @Description: []
 */
@RefreshScope
@Configuration
@Data
public class ConnectProperties {
    private static final String MATCH_ALL_OPERATION = "*";
    private static final String DELETE_OPERATION = "delete";
    private static final Set<String> ALL_CONNECTION_OPERATIONS =
            Sets.newHashSet("create", "delete", "update", "read", "query");
    private static final Set<String> NONE_OPERATIONS = Sets.newHashSet();

    @Value("${odc.connect.host-white-list:}")
    private List<String> hostWhiteList;

    @Value("${odc.connect.temp.expire-after-inactive-interval-seconds:86400}")
    private int tempExpireAfterInactiveIntervalSeconds = 86400;

    @Value("${odc.connect.min-query-timeout-seconds:60}")
    private Integer minQueryTimeoutSeconds = 60;

    @Value("${odc.connect.private.temp-connection-only:false}")
    private boolean privateConnectTempOnly = false;

    @Value("${odc.session.default-time-zone:Asia/Shanghai}")
    private String defaultTimeZone;

    /**
     * 临时连接支持的操作列表
     */
    @Value("${odc.connect.temp-connection-operations:create,delete,update,read}")
    private Set<String> tempConnectionOperations = new HashSet<>();

    /**
     * 持久连接支持的操作列表
     */
    @Value("${odc.connect.persistent-connection-operations:create,delete,update,read}")
    private Set<String> persistentConnectionOperations = new HashSet<>();

    public Set<String> getConnectionSupportedOperations(boolean temp, Set<String> permittedActions) {
        // temp connection can only be private connection, skip permittedActions heere
        if (temp) {
            return getTempConnectionOperations();
        }
        return getPersistentConnectionOperations();
    }

    private Set<String> getTempConnectionOperations() {
        if (CollectionUtils.isEmpty(this.tempConnectionOperations)
                || this.tempConnectionOperations.contains(MATCH_ALL_OPERATION)) {
            return ALL_CONNECTION_OPERATIONS;
        }
        return tempConnectionOperations;
    }

    private Set<String> getPersistentConnectionOperations() {
        if (CollectionUtils.isEmpty(this.persistentConnectionOperations)
                || this.persistentConnectionOperations.contains(MATCH_ALL_OPERATION)) {
            return ALL_CONNECTION_OPERATIONS;
        }
        return persistentConnectionOperations;
    }
}
