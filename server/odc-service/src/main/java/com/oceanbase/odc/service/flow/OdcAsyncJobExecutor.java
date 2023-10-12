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
package com.oceanbase.odc.service.flow;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.flowable.job.api.JobInfo;
import org.flowable.job.service.impl.asyncexecutor.DefaultAsyncJobExecutor;
import org.flowable.job.service.impl.persistence.entity.AbstractJobEntityImpl;
import org.springframework.data.jpa.domain.Specification;

import com.oceanbase.odc.common.util.RetryExecutor;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.constant.FlowStatus;
import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.metadb.flow.FlowInstanceEntity;
import com.oceanbase.odc.metadb.flow.FlowInstanceRepository;
import com.oceanbase.odc.metadb.flow.FlowInstanceSpecs;
import com.oceanbase.odc.metadb.flow.ServiceTaskInstanceEntity;
import com.oceanbase.odc.metadb.flow.ServiceTaskInstanceRepository;
import com.oceanbase.odc.metadb.flow.ServiceTaskInstanceSpecs;

import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link OdcAsyncJobExecutor}
 *
 * @author yh263208
 * @date 2022-08-01 20:27
 * @since ODC_release_3.4.0
 * @see DefaultAsyncJobExecutor
 */
@Slf4j
public class OdcAsyncJobExecutor extends DefaultAsyncJobExecutor {

    private final FlowInstanceRepository flowInstanceRepository;
    private final ServiceTaskInstanceRepository serviceTaskRepository;
    @Setter
    private ExecutorService mockDataExecutorService;
    @Setter
    private ExecutorService loaderDumperExecutorService;
    private final RetryExecutor retryExecutor =
            RetryExecutor.builder().initialDelay(true).retryIntervalMillis(1000).retryTimes(3).build();

    public OdcAsyncJobExecutor(@NonNull FlowInstanceRepository flowInstanceRepository,
            @NonNull ServiceTaskInstanceRepository serviceTaskRepository) {
        this.flowInstanceRepository = flowInstanceRepository;
        this.serviceTaskRepository = serviceTaskRepository;
    }

    @Override
    protected boolean executeAsyncJob(final JobInfo job, Runnable runnable) {
        Long flowInstanceId = null;
        try {
            flowInstanceId = getFlowInstanceIdByJob((AbstractJobEntityImpl) job);
            if (mockDataExecutorService == null && loaderDumperExecutorService == null) {
                this.executorService.execute(runnable);
                return true;
            }
            Set<TaskType> taskTypes = getTaskTypesByJob(flowInstanceId);
            if (taskTypes.contains(TaskType.MOCKDATA) && mockDataExecutorService != null) {
                mockDataExecutorService.submit(runnable);
                return true;
            }
            if (loaderDumperExecutorService != null
                    && (taskTypes.contains(TaskType.IMPORT) || taskTypes.contains(TaskType.EXPORT))) {
                loaderDumperExecutorService.submit(runnable);
                return true;
            }
            this.executorService.execute(runnable);
            return true;
        } catch (Exception e) {
            log.warn("Failed to submit a task", e);
            this.unacquireJobAfterRejection(job);
            if (flowInstanceId != null) {
                final Long fId = flowInstanceId;
                this.retryExecutor.run(() -> {
                    try {
                        flowInstanceRepository.updateStatusById(fId, FlowStatus.EXECUTION_FAILED);
                        return true;
                    } catch (Exception ex) {
                        log.warn("Failed to update flow instance status, flowInstanceId={}", fId, ex);
                        return false;
                    }
                }, b -> b);
            }
            return false;
        }
    }

    @Override
    protected void stopExecutingAsyncJobs() {
        super.stopExecutingAsyncJobs();
        if (mockDataExecutorService != null) {
            mockDataExecutorService.shutdown();
            try {
                if (!mockDataExecutorService.awaitTermination(secondsToWaitOnShutdown, TimeUnit.SECONDS)) {
                    log.warn("Timeout during shutdown of async job executor.");
                }
            } catch (InterruptedException e) {
                log.warn("Interrupted while shutting down the async job executor.", e);
            }
        }
        if (loaderDumperExecutorService != null) {
            loaderDumperExecutorService.shutdown();
            try {
                if (!loaderDumperExecutorService.awaitTermination(secondsToWaitOnShutdown, TimeUnit.SECONDS)) {
                    log.warn("Timeout during shutdown of async job executor.");
                }
            } catch (InterruptedException e) {
                log.warn("Interrupted while shutting down the async job executor.", e);
            }
        }
    }

    private Long getFlowInstanceIdByJob(@NonNull AbstractJobEntityImpl job) {
        String processDefinitionId = job.getProcessDefinitionId();
        Specification<FlowInstanceEntity> flowSpec =
                Specification.where(FlowInstanceSpecs.processDefinitionIdEquals(processDefinitionId));
        List<FlowInstanceEntity> list = flowInstanceRepository.findAll(flowSpec);
        Verify.singleton(list, "Flow instance list has to be singleton by " + processDefinitionId);
        return list.get(0).getId();
    }

    private Set<TaskType> getTaskTypesByJob(Long flowInstanceId) {
        Specification<ServiceTaskInstanceEntity> serviceSpec =
                Specification.where(ServiceTaskInstanceSpecs.flowInstanceIdEquals(flowInstanceId));
        return serviceTaskRepository.findAll(serviceSpec).stream()
                .map(ServiceTaskInstanceEntity::getTaskType).collect(Collectors.toSet());
    }

}
