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
package com.oceanbase.odc.service.onlineschemachange.oscfms.action.oms;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.core.sql.execute.SyncJdbcExecutor;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.onlineschemachange.configuration.OnlineSchemaChangeProperties;
import com.oceanbase.odc.service.onlineschemachange.model.FullVerificationResult;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeScheduleTaskParameters;
import com.oceanbase.odc.service.onlineschemachange.oms.enums.OmsOceanBaseType;
import com.oceanbase.odc.service.onlineschemachange.oms.enums.OmsProjectStatusEnum;
import com.oceanbase.odc.service.onlineschemachange.oms.enums.OscStepName;
import com.oceanbase.odc.service.onlineschemachange.oms.openapi.OmsProjectOpenApiService;
import com.oceanbase.odc.service.onlineschemachange.oms.request.CreateOceanBaseDataSourceRequest;
import com.oceanbase.odc.service.onlineschemachange.oms.request.ListOmsProjectFullVerifyResultRequest;
import com.oceanbase.odc.service.onlineschemachange.oms.request.OmsProjectControlRequest;
import com.oceanbase.odc.service.onlineschemachange.oms.response.OmsProjectFullVerifyResultResponse;
import com.oceanbase.odc.service.onlineschemachange.oms.response.OmsProjectProgressResponse;
import com.oceanbase.odc.service.onlineschemachange.oms.response.OmsProjectStepVO;
import com.oceanbase.odc.service.onlineschemachange.oscfms.action.ProjectStepResult;

import lombok.extern.slf4j.Slf4j;

/**
 * util for oms request
 * 
 * @author longpeng.zlp
 * @date 2024/7/9 10:36
 * @since 4.3.1
 */
@Slf4j
public class OmsRequestUtil {
    /**
     *
     * @return
     */
    protected static OmsProjectControlRequest getProjectRequest(String uid, String projectID) {
        OmsProjectControlRequest projectRequest = new OmsProjectControlRequest();
        projectRequest.setUid(uid);
        projectRequest.setId(projectID);
        return projectRequest;
    }

    protected static OmsProjectStatusEnum getProjectProjectStatus(String uid, String projectID,
            OmsProjectOpenApiService omsProjectOpenApiService) {
        OmsProjectControlRequest projectRequest = new OmsProjectControlRequest();
        projectRequest.setUid(uid);
        projectRequest.setId(projectID);
        OmsProjectProgressResponse progress = omsProjectOpenApiService.describeProjectProgress(projectRequest);
        return progress.getStatus();
    }

    protected static boolean isOmsTaskReady(ProjectStepResult projectStepResult) {
        return projectStepResult.getTaskStatus() == TaskStatus.DONE
                && (projectStepResult.getFullVerificationResult() == FullVerificationResult.CONSISTENT ||
                        projectStepResult.getFullVerificationResult() == FullVerificationResult.UNCHECK);
    }

    protected static ProjectStepResult buildProjectStepResult(OmsProjectOpenApiService omsProjectOpenApiService,
            OnlineSchemaChangeProperties onlineSchemaChangeProperties,
            String uid, String projectID, String databaseName,
            Map<OscStepName, Long> checkFailedTimes) {
        // do remain job
        OmsProjectControlRequest projectRequest = getProjectRequest(uid, projectID);
        List<OmsProjectStepVO> projectSteps = omsProjectOpenApiService.describeProjectSteps(projectRequest);
        if (log.isDebugEnabled()) {
            log.debug("Get project step list from projectOpenApiService is {} ", JsonUtils.toJson(projectSteps));
        }

        OmsProjectProgressResponse progress = omsProjectOpenApiService.describeProjectProgress(projectRequest);
        log.info("Osc check increment checkpoint, current ts = {}, checkpoint = {}", System.currentTimeMillis() / 1000,
                progress.getIncrSyncCheckpoint());
        return new ProjectStepResultChecker(progress, projectSteps,
                onlineSchemaChangeProperties.isEnableFullVerify(),
                onlineSchemaChangeProperties.getOms().getCheckProjectStepFailedTimeoutSeconds(),
                checkFailedTimes)
                        .withCheckerVerifyResult(() -> listProjectFullVerifyResult(projectID,
                                databaseName, uid, omsProjectOpenApiService))
                        .withResumeProject(() -> {
                            omsProjectOpenApiService.resumeProject(projectRequest);
                            return null;
                        })
                        .getCheckerResult();
    }

    private static OmsProjectFullVerifyResultResponse listProjectFullVerifyResult(String projectId, String databaseName,
            String uid, OmsProjectOpenApiService omsProjectOpenApiService) {
        ListOmsProjectFullVerifyResultRequest request = new ListOmsProjectFullVerifyResultRequest();
        request.setProjectId(projectId);
        request.setSourceSchemas(new String[] {databaseName});
        request.setDestSchemas(new String[] {databaseName});
        request.setStatus(new String[] {"FINISHED", "SUSPEND", "RUNNING"});
        request.setPageSize(10);
        request.setPageNumber(1);
        request.setUid(uid);

        OmsProjectFullVerifyResultResponse response = omsProjectOpenApiService.listProjectFullVerifyResult(request);
        if (log.isDebugEnabled()) {
            log.debug("Get project full verify result from projectOpenApiService is {} ", JsonUtils.toJson(response));
        }
        return response;
    }

    /**
     * sleep with exception swallowed
     *
     * @param millionSeconds ms to sleep
     */
    public static void sleep(long millionSeconds) {
        try {
            Thread.sleep(millionSeconds);
        } catch (InterruptedException e) { // swallow exception
        }
    }

    /**
     * get create data source request for oms
     * 
     * @param config
     * @param connectionSession
     * @param oscScheduleTaskParameters
     * @param oscProperties
     * @return
     */
    public static CreateOceanBaseDataSourceRequest getCreateDataSourceRequest(ConnectionConfig config,
            ConnectionSession connectionSession, OnlineSchemaChangeScheduleTaskParameters oscScheduleTaskParameters,
            OnlineSchemaChangeProperties oscProperties) {

        CreateOceanBaseDataSourceRequest request = new CreateOceanBaseDataSourceRequest();
        request.setName(UUID.randomUUID().toString().replace("-", ""));
        request.setType(OmsOceanBaseType.from(config.getType()).name());
        request.setTenant(config.getTenantName());
        request.setCluster(config.getClusterName());
        request.setUserName(config.getUsername());
        if (config.getPassword() != null) {
            request.setPassword(Base64.getEncoder().encodeToString(config.getPassword().getBytes()));
        }
        fillCreateDataSourceRequest(config, connectionSession, oscScheduleTaskParameters, request, oscProperties);
        return request;
    }

    /**
     * fill remain variables if needed
     */
    protected static void fillCreateDataSourceRequest(ConnectionConfig config, ConnectionSession connectionSession,
            OnlineSchemaChangeScheduleTaskParameters oscScheduleTaskParameters,
            CreateOceanBaseDataSourceRequest request, OnlineSchemaChangeProperties oscProperties) {
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

    protected static String getConfigUrl(ConnectionSession connectionSession) {

        SyncJdbcExecutor syncJdbcExecutor = connectionSession.getSyncJdbcExecutor(
                ConnectionSessionConstants.CONSOLE_DS_KEY);
        String queryClusterUrlSql = "show parameters like 'obconfig_url'";
        return syncJdbcExecutor.query(queryClusterUrlSql, rs -> {
            if (!rs.next()) {
                throw new IllegalArgumentException("Get ob config_url is empty");
            }
            return rs.getString("value");
        });
    }

    protected static boolean isObCE(ConnectionSession connectionSession) {
        SyncJdbcExecutor syncJdbcExecutor = connectionSession.getSyncJdbcExecutor(
                ConnectionSessionConstants.CONSOLE_DS_KEY);
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
