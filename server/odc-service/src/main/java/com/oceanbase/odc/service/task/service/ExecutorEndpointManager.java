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

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.metadb.task.JobEntity;
import com.oceanbase.odc.service.cloud.model.CloudProvider;
import com.oceanbase.odc.service.resource.ResourceID;
import com.oceanbase.odc.service.resource.ResourceManager;
import com.oceanbase.odc.service.task.caller.ExecutorIdentifier;
import com.oceanbase.odc.service.task.caller.ExecutorIdentifierParser;
import com.oceanbase.odc.service.task.caller.JobContext;
import com.oceanbase.odc.service.task.caller.ResourceIDUtil;
import com.oceanbase.odc.service.task.enums.JobStatus;
import com.oceanbase.odc.service.task.enums.TaskRunMode;
import com.oceanbase.odc.service.task.resource.K8sPodResource;
import com.oceanbase.odc.service.task.schedule.DefaultJobContextBuilder;
import com.oceanbase.odc.service.task.util.JobPropertiesUtils;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@SkipAuthorize("odc internal usage")
public class ExecutorEndpointManager {
    @Autowired
    private TaskFrameworkService taskFrameworkService;
    @Autowired
    private ResourceManager resourceManager;

    private ExecutorHostAdapter hostAdapter = null;

    public void setHostAdapter(ExecutorHostAdapter hostAdapter) {
        this.hostAdapter = hostAdapter;
    }

    public String getExecutorEndpoint(@NonNull JobEntity je) {
        Long jobId = je.getId();
        JobStatus status = je.getStatus();

        if (!status.isExecuting()) {
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
        Map<String, String> jobProperties = jobContext.getJobProperties();
        int executorListenPort = JobPropertiesUtils.getExecutorListenPort(jobProperties);
        if (executorListenPort <= 0) {
            throw new RuntimeException("Failed to get executor listen port, jobId=" + jobId
                    + ", executorListenPort=" + executorListenPort);
        }
        try {
            ResourceID resourceID = ResourceIDUtil.getResourceID(executorIdentifier, je);
            Optional<K8sPodResource> resourceOptional =
                    resourceManager.query(resourceID);
            if (resourceOptional.isPresent()) {
                K8sPodResource response = resourceOptional.get();
                String podIpAddress = response.getPodIpAddress();
                if (StringUtils.isNotBlank(podIpAddress)) {
                    String adaptedHost = adaptHost(podIpAddress, jobProperties);
                    executorEndpoint = "http://" + adaptedHost + ":" + executorListenPort;
                    taskFrameworkService.updateExecutorEndpoint(jobId, executorEndpoint);
                    return executorEndpoint;
                } else {
                    throw new RuntimeException(
                            "Failed to get executor endpoint, pod status=" + response.getResourceState());
                }
            } else {
                throw new RuntimeException("Failed to get executor endpoint, pod not exists");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to get executor endpoint", e);
        }
    }

    private String adaptHost(String host, Map<String, String> jobProperties) {
        if (hostAdapter == null) {
            return host;
        }
        CloudProvider cloudProvider = JobPropertiesUtils.getCloudProvider(jobProperties);
        String regionName = JobPropertiesUtils.getRegionName(jobProperties);
        if (Objects.isNull(cloudProvider) || StringUtils.isBlank(regionName)) {
            throw new RuntimeException("Failed to get cloud provider or region name");
        }
        return hostAdapter.adapt(host, cloudProvider, regionName);
    }

}
