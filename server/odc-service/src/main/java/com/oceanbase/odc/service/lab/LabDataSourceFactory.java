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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.druid.pool.DruidDataSource;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.service.connection.ConnectionService;
import com.oceanbase.odc.service.lab.model.LabProperties;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2022/8/5 下午6:07
 * @Description: []
 */
@Component
@Slf4j
public class LabDataSourceFactory {

    private List<DataSourceContext> dataSourceContexts;

    @Autowired
    private ResourceService resourceService;

    @Autowired
    private ConnectionService connectionService;

    @Autowired
    private LabProperties labProperties;

    @PostConstruct
    public void init() {
        refreshDataSource(labProperties.getObConnectionKey());
    }


    public DataSourceContext getDataSourceContext(long userId) {
        if (CollectionUtils.isNotEmpty(dataSourceContexts)) {
            return dataSourceContexts.get((int) userId % dataSourceContexts.size());
        } else {
            log.error("Failed to get lab DataSource due to empty Lab DataSource configuration");
            throw new RuntimeException("Failed to get lab DataSource due to empty Lab DataSource configuration");
        }
    }

    public void refreshDataSource(String allConfig) {
        if (!labProperties.isLabEnabled()) {
            return;
        }
        List<ConnectionProperty> newConnectionPropertyList =
                JsonUtils.fromJsonList(allConfig, ConnectionProperty.class);
        if (CollectionUtils.isEmpty(newConnectionPropertyList)) {
            return;
        }
        if (CollectionUtils.isEmpty(dataSourceContexts)) {
            dataSourceContexts = new ArrayList<>();
            newConnectionPropertyList.forEach(connectionProperty -> dataSourceContexts.add(create(connectionProperty)));
        }
        for (int i = 0; i < newConnectionPropertyList.size(); i++) {
            if (!Objects.equals(newConnectionPropertyList.get(i), dataSourceContexts.get(i).getConnectionProperty())) {
                dataSourceContexts.set(i, create(newConnectionPropertyList.get(i)));
                String[] oldHost = dataSourceContexts.get(i).getConnectionProperty().getHost().split(":");
                if (oldHost.length == 2) {
                    Set<Long> connectionIds = connectionService.findIdsByHost(oldHost[0]);
                    connectionIds.forEach(resourceService::revokeResource);
                }
                log.info("DataSource refreshed, new connection host:{}", newConnectionPropertyList.get(i).host);
            }
        }
    }


    private DataSourceContext create(ConnectionProperty properties) {
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl(properties.getJdbcUrl());
        dataSource.setUsername(properties.getUsername());
        dataSource.setPassword(properties.getPassword());
        dataSource.setValidationQuery("select 1 from dual");
        dataSource.setTestWhileIdle(true);
        dataSource.setKeepAlive(true);
        dataSource.setTimeBetweenEvictionRunsMillis(30000);
        dataSource.setDefaultAutoCommit(false);
        dataSource.setMaxActive(100);
        dataSource.setInitialSize(100);
        dataSource.setMinIdle(30);
        dataSource.setMaxWait(10_000L);

        DataSourceContext context = new DataSourceContext();
        context.setDataSource(dataSource);
        context.setConnectionProperty(properties);
        return context;
    }

    @Data
    @EqualsAndHashCode
    static class ConnectionProperty {
        private String host;
        private String username;
        private String password;
        private String jdbcUrl;
    }

    @Data
    static class DataSourceContext {
        private DataSource dataSource;
        private ConnectionProperty connectionProperty;

        /**
         * tenantName may null while use cloud database instance
         */
        private String tenantName;
        /**
         * clusterName may null while use cloud database instance
         */
        private String clusterName;
        private String username;

        public void setConnectionProperty(ConnectionProperty connectionProperty) {
            this.connectionProperty = connectionProperty;
            String username = connectionProperty.getUsername();
            if (StringUtils.contains(username, "#")) {
                String[] usernameAndClusterName = username.split("#");
                username = usernameAndClusterName[0];
                this.clusterName = usernameAndClusterName[1];
            }
            if (StringUtils.contains(username, "@")) {
                String[] usernameAndTenantName = username.split("@");
                username = usernameAndTenantName[0];
                this.tenantName = usernameAndTenantName[1];
            }
            this.username = username;
        }
    }
}

