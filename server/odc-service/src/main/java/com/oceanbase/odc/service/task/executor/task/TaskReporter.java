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

package com.oceanbase.odc.service.task.executor.task;

import java.text.MessageFormat;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.util.SystemUtils;
import com.oceanbase.odc.core.flow.model.FlowTaskResult;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.service.common.response.SuccessResponse;
import com.oceanbase.odc.service.task.caller.JobUtils;
import com.oceanbase.odc.service.task.executor.util.HttpUtil;
import com.oceanbase.odc.service.task.model.ExecutorInfo;
import com.oceanbase.odc.service.task.schedule.JobIdentity;

import lombok.extern.slf4j.Slf4j;

/**
 * @author gaoda.xy
 * @date 2023/11/30 19:41
 */
@Slf4j
public class TaskReporter {

    private final List<String> hostUrls;

    private static final String UPLOAD_RESULT_URL = "/api/v2/task/result";

    public TaskReporter(List<String> hostUrls) {
        this.hostUrls = hostUrls;
    }

    public void report(JobIdentity jobIdentity, TaskStatus status, double progress, FlowTaskResult taskResult) {
        if (CollectionUtils.isEmpty(hostUrls)) {
            log.warn("host url is empty");
            return;
        }
        DefaultTaskResult result = new DefaultTaskResult();
        result.setJobIdentity(jobIdentity);
        result.setTaskStatus(status);
        result.setProgress(progress);
        result.setResultJson(JsonUtils.toJson(taskResult));

        ExecutorInfo ei = new ExecutorInfo();
        ei.setHost(SystemUtils.getLocalIpAddress());
        ei.setPort(JobUtils.getPort());
        ei.setHostName(SystemUtils.getHostName());
        ei.setPid(SystemUtils.getPid());
        ei.setJvmStartTime(SystemUtils.getJVMStartTime());
        result.setExecutorInfo(ei);

        // Task executor prefers to upload result to the first host, if failed, try the next one
        for (String host : hostUrls) {
            try {
                String url = host + UPLOAD_RESULT_URL;
                SuccessResponse<String> response = HttpUtil.request(url, JsonUtils.toJson(result),
                        new TypeReference<SuccessResponse<String>>() {});
                if (response != null && response.getSuccessful()) {
                    log.info("Report to host {} success, result is {}, response is {}.", host, JsonUtils.toJson(result),
                            JsonUtils.toJson(response));
                    break;
                }
            } catch (Exception e) {
                log.warn(MessageFormat.format("Report to host {0} failed, result is {}, error is {1}", host,
                        JsonUtils.toJson(result)), e);
            }
        }
    }

}
