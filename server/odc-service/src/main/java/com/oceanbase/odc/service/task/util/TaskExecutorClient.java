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
package com.oceanbase.odc.service.task.util;

import java.io.IOException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.util.ExceptionUtils;
import com.oceanbase.odc.service.common.response.SuccessResponse;
import com.oceanbase.odc.service.task.constants.JobExecutorUrls;
import com.oceanbase.odc.service.task.exception.JobException;
import com.oceanbase.odc.service.task.executor.TaskResult;
import com.oceanbase.odc.service.task.schedule.JobIdentity;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * task-executor api calling encapsulation <br>
 * see @{@link JobExecutorUrls}
 */
@Slf4j
public class TaskExecutorClient {

    public void stop(@NonNull String executorEndpoint, @NonNull JobIdentity ji) throws JobException {
        String url = executorEndpoint + String.format(JobExecutorUrls.STOP_TASK, ji.getId());
        log.info("Try stop job in executor, ji={}, url={}", ji.getId(), url);
        try {
            SuccessResponse<Boolean> response =
                    HttpClientUtils.request("POST", url, "", new TypeReference<SuccessResponse<Boolean>>() {});
            if (response != null && response.getSuccessful() && response.getData()) {
                log.info("Stop job in executor succeed, ji={}, response={}.", ji.getId(), JsonUtils.toJson(response));
            } else {
                throw new JobException("Stop job response failed, ji={0}, response={1}", ji.getId(),
                        JsonUtils.toJson(response));
            }
        } catch (IOException e) {
            log.warn("Stop job in executor occur error, ji={}, url={}", ji.getId(), url, e);
            throw new JobException("Stop job in executor occur error, jobId={0}, causeMessage={1}",
                    ji.getId(), ExceptionUtils.getRootCauseReason(e));
        }
    }

    public void modifyJobParameters(@NonNull String executorEndpoint, @NonNull JobIdentity ji,
            @NonNull String jobParametersJson) throws JobException {
        String url = executorEndpoint + String.format(JobExecutorUrls.MODIFY_JOB_PARAMETERS, ji.getId());
        log.info("Try to modify job parameters, jobId={}, url={}", ji.getId(), url);
        try {
            SuccessResponse<Boolean> response =
                    HttpClientUtils.request("POST", url, jobParametersJson,
                            new TypeReference<SuccessResponse<Boolean>>() {});
            if (response != null && response.getSuccessful() && response.getData()) {
                log.info("Modify job parameters success, jobId={}, response={}.", ji.getId(),
                        JsonUtils.toJson(response));
            } else {
                throw new JobException("Modify job parameters failed, jobId={0}, response={1}",
                        ji.getId(), response);
            }
        } catch (IOException e) {
            throw new JobException("Modify job parameters occur error, jobId={0}, causeMessage={1}",
                    ji.getId(), ExceptionUtils.getRootCauseReason(e));
        }
    }

    public TaskResultWrap getResult(@NonNull String executorEndpoint, @NonNull JobIdentity ji) throws JobException {
        String url = executorEndpoint + String.format(JobExecutorUrls.GET_RESULT, ji.getId());
        log.info("Try query job result from executor, jobId={}, url={}", ji.getId(), url);
        try {
            SuccessResponse<TaskResult> response =
                    HttpClientUtils.request("GET", url, new TypeReference<SuccessResponse<TaskResult>>() {});
            if (response != null && response.getSuccessful()) {
                return TaskResultWrap.successTaskResult(response.getData());
            } else {
                return TaskResultWrap
                        .failedTaskResult(new JobException("Get job result failed, jobId={0}, response={1}",
                                ji.getId(), JsonUtils.toJson(response)));
            }
        } catch (IOException e) {
            return TaskResultWrap
                    .unreachedTaskResult(new JobException("Get job result occur error, jobId={0}, causeMessage={1}",
                            ji.getId(), ExceptionUtils.getRootCauseReason(e)));
        }
    }



}
