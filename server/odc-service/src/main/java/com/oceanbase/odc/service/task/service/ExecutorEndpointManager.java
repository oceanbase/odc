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
package com.oceanbase.odc.service.task.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.metadb.task.JobEntity;
import com.oceanbase.odc.service.task.caller.ExecutorIdentifier;
import com.oceanbase.odc.service.task.caller.ExecutorIdentifierParser;
import com.oceanbase.odc.service.task.caller.JobContext;
import com.oceanbase.odc.service.task.caller.K8sJobClient;
import com.oceanbase.odc.service.task.caller.K8sJobClientSelector;
import com.oceanbase.odc.service.task.caller.K8sJobResponse;
import com.oceanbase.odc.service.task.enums.JobStatus;
import com.oceanbase.odc.service.task.enums.TaskRunMode;
import com.oceanbase.odc.service.task.schedule.DefaultJobContextBuilder;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ExecutorEndpointManager {
    @Autowired
    private K8sJobClientSelector k8sJobClientSelector;
    @Autowired
    private TaskFrameworkService taskFrameworkService;

    public String getExecutorEndpoint(@NonNull JobEntity je) {
        Long jobId = je.getId();
        JobStatus status = je.getStatus();

        if (status.isExecuting()) {
            throw new RuntimeException("Job is not executing, no executor endpoint available, jobId=" + jobId);
        }
        String executorEndpoint = je.getExecutorEndpoint();
        if (StringUtils.isNotBlank(executorEndpoint)) {
            return executorEndpoint;
        }
        if (TaskRunMode.PROCESS == je.getRunMode()) {
            throw new RuntimeException("No executor endpoint available for process mode, jobId=" + jobId);
        }
        // here TaskRunMode.K8S == je.getRunMode()
        JobContext jobContext = new DefaultJobContextBuilder().build(je);
        ExecutorIdentifier executorIdentifier = ExecutorIdentifierParser.parser(je.getExecutorIdentifier());
        K8sJobClient k8sJobClient = k8sJobClientSelector.select(jobContext);
        try {
            Optional<K8sJobResponse> responseOptional = k8sJobClient.get(executorIdentifier.getNamespace(),
                    executorIdentifier.getExecutorName());
            if (responseOptional.isPresent()) {
                K8sJobResponse response = responseOptional.get();
                String podIpAddress = response.getPodIpAddress();
                if (StringUtils.isNotBlank(podIpAddress)) {
                    executorEndpoint = "http://" + podIpAddress + ":8080";
                    taskFrameworkService.updateExecutorEndpoint(jobId, executorEndpoint);
                    return executorEndpoint;
                } else {
                    throw new RuntimeException(
                            "Failed to get executor endpoint, pod status=" + response.getResourceStatus());
                }
            } else {
                throw new RuntimeException("Failed to get executor endpoint, pod not exists");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to get executor endpoint, " + e.getMessage());
        }
    }

}
