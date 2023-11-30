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

import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.flow.model.FlowTaskResult;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.service.common.model.HostProperties;
import com.oceanbase.odc.service.common.response.SuccessResponse;
import com.oceanbase.odc.service.task.executor.util.HttpUtil;
import com.oceanbase.odc.service.task.schedule.JobIdentity;

/**
 * @author gaoda.xy
 * @date 2023/11/30 19:41
 */
public class TaskReporter {

    private final List<HostProperties> hosts;

    private static final String UPLOAD_RESULT_URL = "/api/v2/task/result";

    public TaskReporter(List<HostProperties> hosts) {
        this.hosts = hosts;
    }

    public void report(JobIdentity jobIdentity, TaskStatus status, double progress, FlowTaskResult taskResult) {
        if (hosts == null || hosts.isEmpty()) {
            return;
        }
        DefaultTaskResult result = new DefaultTaskResult();
        result.setJobIdentity(jobIdentity);
        result.setTaskStatus(status);
        result.setProgress(progress);
        result.setResultJson(JsonUtils.toJson(taskResult));
        // Task executor prefers to upload result to the first host, if failed, try the next one
        for (HostProperties host : hosts) {
            try {
                String url = "http://" + host.getOdcHost() + ":" + host.getPort() + UPLOAD_RESULT_URL;
                SuccessResponse<String> response = HttpUtil.request(url, JsonUtils.toJson(result),
                        new TypeReference<SuccessResponse<String>>() {});
                if (response != null && response.getSuccessful()) {
                    break;
                }
            } catch (Exception e) {
                // eat exception
            }
        }
    }

}
