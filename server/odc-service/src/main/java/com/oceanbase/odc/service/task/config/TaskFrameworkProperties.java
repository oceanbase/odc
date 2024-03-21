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
package com.oceanbase.odc.service.task.config;

import com.oceanbase.odc.service.task.enums.TaskRunMode;

/**
 * @author yaobin
 * @date 2024-01-10
 * @since 4.2.4
 */
public interface TaskFrameworkProperties {
    boolean isEnabled();

    TaskRunMode getRunMode();

    String getOdcUrl();

    K8sProperties getK8sProperties();

    int getJobHeartTimeoutSeconds();

    int getExecutorWaitingToRunThresholdSeconds();

    int getExecutorWaitingToRunThresholdCount();

    int getJobCancelTimeoutSeconds();

    int getSingleFetchPreparingJobRows();

    int getSingleFetchCancelingJobRows();

    int getSingleFetchDestroyExecutorJobRows();

    int getSingleFetchCheckHeartTimeoutJobRows();

    int getMaxHeartTimeoutRetryTimes();

    int getQuartzStartDelaySeconds();

    int getJobProcessMinMemorySizeInMB();

    int getJobProcessMaxMemorySizeInMB();

    String getStartPreparingJobCronExpression();

    String getCheckRunningJobCronExpression();

    String getDoCancelingJobCronExpression();

    String getDestroyExecutorJobCronExpression();

}
