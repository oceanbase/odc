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

package com.oceanbase.odc.service.onlineschemachange.pipeline;

import java.util.Base64;

import org.springframework.stereotype.Component;

import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.sql.execute.SyncJdbcExecutor;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.onlineschemachange.exception.OmsException;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeScheduleTaskParameters;
import com.oceanbase.odc.service.onlineschemachange.oms.enums.OmsOceanBaseType;
import com.oceanbase.odc.service.onlineschemachange.oms.request.CreateOceanBaseDataSourceRequest;
import com.oceanbase.odc.service.onlineschemachange.oms.request.CreateProjectRequest;

/**
 * @author yaobin
 * @date 2023-08-11
 * @since 4.2.0
 */
@Component
public class WebCreateOmsProjectValve extends BaseCreateOmsProjectValve {
    @Override
    protected void doCreateDataSourceRequest(ConnectionConfig config, ConnectionSession connectionSession,
            OnlineSchemaChangeScheduleTaskParameters oscScheduleTaskParameters,
            CreateOceanBaseDataSourceRequest request) {
        request.setIp(config.getHost());
        request.setPort(config.getPort());
        request.setRegion(oscProperties.getOms().getRegion());
        request.setOcpName(null);
        String configUrl = getConfigUrl(connectionSession);
        request.setConfigUrl(configUrl);
        request.setDrcUserName(config.getSysTenantUsername());
        if (config.getSysTenantPassword() != null) {
            request.setDrcPassword(Base64.getEncoder().encodeToString(config.getSysTenantPassword().getBytes()));
        }
        if (config.getDialectType() == DialectType.OB_MYSQL && isObCE(connectionSession)) {
            request.setType(OmsOceanBaseType.OB_MYSQL_CE.name());
        }
    }

    @Override
    protected String reCreateDataSourceRequestAfterThrowsException(
            OnlineSchemaChangeScheduleTaskParameters oscScheduleTaskParameters,
            CreateOceanBaseDataSourceRequest request, OmsException ex) {
        return null;
    }

    @Override
    protected void doCreateProjectRequest(String omsDsId, Long scheduleId,
            OnlineSchemaChangeScheduleTaskParameters oscScheduleTaskParameters, CreateProjectRequest request) {

    }

    private String getConfigUrl(ConnectionSession connectionSession) {

        SyncJdbcExecutor syncJdbcExecutor = connectionSession.getSyncJdbcExecutor(
                ConnectionSessionConstants.BACKEND_DS_KEY);
        String queryClusterUrlSql = "show parameters like 'obconfig_url'";
        return syncJdbcExecutor.query(queryClusterUrlSql, rs -> {
            if (!rs.next()) {
                throw new IllegalArgumentException("Get ob config_url is empty");
            }
            return rs.getString("value");
        });
    }

    private boolean isObCE(ConnectionSession connectionSession) {
        SyncJdbcExecutor syncJdbcExecutor = connectionSession.getSyncJdbcExecutor(
                ConnectionSessionConstants.BACKEND_DS_KEY);
        String queryVersionSql = "show variables like 'version_comment'";
        String versionString = syncJdbcExecutor.query(queryVersionSql, rs -> {
            if (!rs.next()) {
                throw new IllegalArgumentException("Get ob version is empty");
            }
            return rs.getString("value");
        });
        return versionString != null && versionString.startsWith("OceanBase_CE");
    }
}
