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
package com.oceanbase.odc.service.onlineschemachange.oms;

import java.util.List;
import java.util.Map;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.service.onlineschemachange.configuration.OnlineSchemaChangeProperties;
import com.oceanbase.odc.service.onlineschemachange.model.FullVerificationResult;
import com.oceanbase.odc.service.onlineschemachange.oms.enums.OmsStepName;
import com.oceanbase.odc.service.onlineschemachange.oms.openapi.OmsProjectOpenApiService;
import com.oceanbase.odc.service.onlineschemachange.oms.request.ListOmsProjectFullVerifyResultRequest;
import com.oceanbase.odc.service.onlineschemachange.oms.request.OmsProjectControlRequest;
import com.oceanbase.odc.service.onlineschemachange.oms.response.OmsProjectFullVerifyResultResponse;
import com.oceanbase.odc.service.onlineschemachange.oms.response.OmsProjectProgressResponse;
import com.oceanbase.odc.service.onlineschemachange.oms.response.OmsProjectStepVO;
import com.oceanbase.odc.service.onlineschemachange.oscfms.action.oms.ProjectStepResultChecker;
import com.oceanbase.odc.service.onlineschemachange.oscfms.action.oms.ProjectStepResultChecker.ProjectStepResult;

import lombok.extern.slf4j.Slf4j;

/**
 * @author longpeng.zlp
 * @date 2024/7/29 14:01
 * @since 4.3.1
 */
@Slf4j
public class OmsRequestUtil {
    /**
     *
     * @return
     */
    public static OmsProjectControlRequest getProjectRequest(String uid, String projectID) {
        OmsProjectControlRequest projectRequest = new OmsProjectControlRequest();
        projectRequest.setUid(uid);
        projectRequest.setId(projectID);
        return projectRequest;
    }

    public static boolean isOmsTaskReady(ProjectStepResult projectStepResult) {
        return projectStepResult.getTaskStatus() == TaskStatus.DONE
                && (projectStepResult.getFullVerificationResult() == FullVerificationResult.CONSISTENT ||
                        projectStepResult.getFullVerificationResult() == FullVerificationResult.UNCHECK);
    }

    public static ProjectStepResult buildProjectStepResult(OmsProjectOpenApiService omsProjectOpenApiService,
            OnlineSchemaChangeProperties onlineSchemaChangeProperties,
            String uid, String projectID, String databaseName,
            Map<OmsStepName, Long> checkFailedTimes) {
        // do remain job
        OmsProjectControlRequest projectRequest = getProjectRequest(uid, projectID);
        List<OmsProjectStepVO> projectSteps = omsProjectOpenApiService.describeProjectSteps(projectRequest);
        if (log.isDebugEnabled()) {
            log.debug("Get project step list from projectOpenApiService is {} ", JsonUtils.toJson(projectSteps));
        }


        OmsProjectProgressResponse progress = omsProjectOpenApiService.describeProjectProgress(projectRequest);

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
}
