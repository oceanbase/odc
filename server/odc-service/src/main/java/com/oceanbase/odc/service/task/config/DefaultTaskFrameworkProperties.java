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

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

import com.oceanbase.odc.service.task.enums.TaskMonitorMode;
import com.oceanbase.odc.service.task.enums.TaskRunMode;

import lombok.Data;

/**
 * @author yaobin
 * @date 2023-11-21
 * @since 4.2.4
 */
@Data
@RefreshScope
@Configuration
@ConfigurationProperties(prefix = "odc.task-framework")
public class DefaultTaskFrameworkProperties implements TaskFrameworkProperties {

    // task-framework enabled
    private boolean enabled;

    private TaskRunMode runMode;

    private TaskMonitorMode monitorMode = TaskMonitorMode.PUSH;

    private String odcUrl;

    @NestedConfigurationProperty
    private K8sProperties k8sProperties = new K8sProperties();

    // job will be timeout when last report time more than this duration
    private int jobHeartTimeoutSeconds = 300;

    // TODO: remove unused property executorWaitingToRunThresholdSeconds
    private int executorWaitingToRunThresholdSeconds = 3;
    // TODO: remove unused property executorWaitingToRunThresholdCount
    private int executorWaitingToRunThresholdCount = 10;

    // job to be canceled timeout and current status is cancelling
    private int jobCancelTimeoutSeconds = 2 * 60;

    // single fetch job rows for schedule
    private int singleFetchPreparingJobRows = 1;

    private int singleFetchCancelingJobRows = 10;

    private int singleFetchDestroyExecutorJobRows = 10;

    // single fetch job rows to check report timeout or not
    private int singleFetchCheckHeartTimeoutJobRows = 30;

    // single fetch job rows to pull task result
    private int singlePullResultJobRows = 200;

    // max retry times after heart timeout
    private int maxHeartTimeoutRetryTimes = 3;

    // number of seconds to wait after initialization before starting the scheduler
    private int quartzStartDelaySeconds = 30;

    // min memory required for start process, unit is MB, this setting only usage for linux
    private long jobProcessMinMemorySizeInMB = 1024;

    // max memory required for start process, unit is MB, this setting only usage for linux
    private long jobProcessMaxMemorySizeInMB = 1024;

    // job will not be started if systemFreeMemory less than this setting
    private long systemReserveMinFreeMemorySizeInMB = 1024;

    private String startPreparingJobCronExpression;

    private String checkRunningJobCronExpression;

    private String doCancelingJobCronExpression;

    private String destroyExecutorJobCronExpression;

    private String pullTaskResultJobCronExpression;

    // start and manage resource should invoke frequent to reduce job schedule time
    private String startPreparingJobV2CronExpression = "0/1 * * * * ?";

    private String manageResourceJobV2CronExpression = "0/1 * * * * ?";

    private String pullTaskResultJobV2CronExpression = "0/10 * * * * ?";

    private String doStopJobCronV2Expression = "0/2 * * * * ?";

    private String doFinishJobV2CronExpression = "0/2 * * * * ?";

    private String processMainClassName;
    /**
     * local k8s debug mode, use process builder mock k8s
     */
    private boolean enableK8sLocalDebugMode;

    /**
     * if enable task supervisor agent, current only in process mode, this flag can enabled, k8s mode
     * will ignore this mode
     */
    private boolean enableTaskSupervisorAgent;

    // resource allocate will expired after resourceAllocateTimeOutSeconds
    private int resourceAllocateTimeOutSeconds = 600;

    // config for max job count running in one region
    private int maxAllowRunningJobs = 8;

    // negative means not given, this value should not be changed, cause a new port means a new agent
    // server
    private int taskSupervisorAgentListenPort = -1;

    // keep supervisor endpoint alive for a period if there is no task running on it or release it
    private int supervisorEndpointKeepAliveSeconds = 300;
}
