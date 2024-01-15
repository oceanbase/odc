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

package com.oceanbase.odc.service.task.caller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.metadb.task.JobEntity;
import com.oceanbase.odc.service.common.response.SuccessResponse;
import com.oceanbase.odc.service.task.config.JobConfiguration;
import com.oceanbase.odc.service.task.config.JobConfigurationHolder;
import com.oceanbase.odc.service.task.constants.JobUrlConstants;
import com.oceanbase.odc.service.task.enums.JobCallerAction;
import com.oceanbase.odc.service.task.listener.JobCallerEvent;
import com.oceanbase.odc.service.task.schedule.JobIdentity;
import com.oceanbase.odc.service.task.service.TaskFrameworkService;
import com.oceanbase.odc.service.task.util.HttpUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2023-11-16
 * @since 4.2.4
 */
@Slf4j
public abstract class BaseJobCaller implements JobCaller {

    @Override
    public void start(JobContext context) throws JobException {
        try {
            ExecutorIdentifier executorIdentifier = tryStart(context);
            publishEvent(new JobCallerEvent(context.getJobIdentity(), JobCallerAction.START, true,
                    executorIdentifier, null));
        } catch (Exception ex) {
            publishEvent(new JobCallerEvent(context.getJobIdentity(), JobCallerAction.START, false, ex));
            throw new JobException(ex);
        }
    }

    private ExecutorIdentifier tryStart(JobContext context) throws Exception {
        try {
            return doStart(context);
        } catch (Exception e) {
            log.warn("Start job {} failed and retry again, error is: ", context.getJobIdentity().getId(), e);
            throw e;
        }
    }

    @Override
    public void stop(JobIdentity ji) throws JobException {
        // send stop to executor
        JobConfiguration jobConfiguration = JobConfigurationHolder.getJobConfiguration();
        if (jobConfiguration == null || jobConfiguration.getTaskFrameworkService() == null) {
            return;
        }
        TaskFrameworkService taskFrameworkService = jobConfiguration.getTaskFrameworkService();
        JobEntity jobEntity = taskFrameworkService.find(ji.getId());
        String executorEndpoint = jobEntity.getExecutorEndpoint();

        try {
            String url = executorEndpoint + String.format(JobUrlConstants.STOP_TASK, ji.getId());
            SuccessResponse<Boolean> response =
                    HttpUtil.request(url, new TypeReference<SuccessResponse<Boolean>>() {});
            log.info("Stop job {} response is {}.", ji.getId(), JsonUtils.toJson(response));
            if (response != null && response.getSuccessful() && response.getData()) {
                publishEvent(new JobCallerEvent(ji, JobCallerAction.STOP, true, null));
            } else {
                publishEvent(new JobCallerEvent(ji, JobCallerAction.STOP, false, null));
            }
            log.info("Stop job {} successfully, executor endpoint {}.", ji.getId(), executorEndpoint);
        } catch (Exception e) {
            publishEvent(new JobCallerEvent(ji, JobCallerAction.STOP, false, e));
            throw new JobException(e);
        }

    }

    @Override
    public void destroy(JobIdentity ji) throws JobException {
        JobConfiguration jobConfiguration = JobConfigurationHolder.getJobConfiguration();
        if (jobConfiguration == null || jobConfiguration.getTaskFrameworkService() == null) {
            return;
        }
        TaskFrameworkService taskFrameworkService = jobConfiguration.getTaskFrameworkService();
        JobEntity jobEntity = taskFrameworkService.find(ji.getId());
        String executorIdentifier = jobEntity.getExecutorIdentifier();
        PreConditions.notNull(executorIdentifier, "executorIdentifier");
        log.info("Preparing stop job {}, executor name {}.", ji.getId(), executorIdentifier);

        try {
            destroy(ExecutorIdentifierParser.parser(executorIdentifier));
            log.info("Stop job {} successfully, executor name {}.", ji.getId(), executorIdentifier);
            publishEvent(new JobCallerEvent(ji, JobCallerAction.DESTROY, true, null));
        } catch (JobException ex) {
            publishEvent(new JobCallerEvent(ji, JobCallerAction.DESTROY, false, ex));
            throw ex;
        }
    }

    private void publishEvent(JobCallerEvent event) {
        JobConfiguration configuration = JobConfigurationHolder.getJobConfiguration();
        if (configuration != null && configuration.getEventPublisher() != null) {
            configuration.getEventPublisher().publishEvent(event);
        }
    }

    @Override
    public void destroy(ExecutorIdentifier identifier) throws JobException {
        doDestroy(identifier);
    }


    protected abstract ExecutorIdentifier doStart(JobContext context) throws JobException;

    protected abstract void doStop(JobIdentity ji) throws JobException;

    protected abstract void doDestroy(ExecutorIdentifier identifier) throws JobException;

}
