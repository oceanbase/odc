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

import com.oceanbase.odc.service.task.enums.TaskMonitorMode;
import com.oceanbase.odc.service.task.enums.TaskRunMode;

/**
 * @author yaobin
 * @date 2024-01-10
 * @since 4.2.4
 */
public interface TaskFrameworkProperties {
    boolean isEnabled();

    TaskRunMode getRunMode();

    TaskMonitorMode getMonitorMode();

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

    int getSinglePullResultJobRows();

    int getMaxHeartTimeoutRetryTimes();

    int getQuartzStartDelaySeconds();

    long getJobProcessMinMemorySizeInMB();

    long getJobProcessMaxMemorySizeInMB();

    long getSystemReserveMinFreeMemorySizeInMB();

    String getStartPreparingJobCronExpression();

    String getCheckRunningJobCronExpression();

    String getPullTaskResultJobCronExpression();

    String getDoCancelingJobCronExpression();

    String getDestroyExecutorJobCronExpression();


    // v2 cron expression
    String getStartPreparingJobV2CronExpression();

    String getPullTaskResultJobV2CronExpression();

    String getDoStopJobCronV2Expression();

    String getDoFinishJobV2CronExpression();

    String getManageResourceJobV2CronExpression();


    /**
     * main class to boot process, default is null, to upgrade from process caller to supervisor
     * 
     * @return
     */
    String getProcessMainClassName();

    boolean isEnableK8sLocalDebugMode();

    boolean isEnableTaskSupervisorAgent();

    int getResourceAllocateTimeOutSeconds();

    int getMaxAllowRunningJobs();

    int getTaskSupervisorAgentListenPort();

    int getSupervisorEndpointKeepAliveSeconds();

}
