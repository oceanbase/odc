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

    private String odcUrl;

    @NestedConfigurationProperty
    private K8sProperties k8sProperties = new K8sProperties();

    // job will be timeout when last report time more than this duration
    private int jobHeartTimeoutSeconds;

    // job to be canceled timeout and current status is cancelling
    private int jobCancelTimeoutSeconds = 2 * 60;

    // single fetch job rows for schedule
    private int singleFetchPreparingJobRows = 10;

    private int singleFetchCancelingJobRows = 100;

    private int singleFetchDestroyExecutorJobRows = 100;

    // single fetch job rows to check report timeout or not
    private int singleFetchCheckHeartTimeoutJobRows = 1000;

    // max retry times after heart timeout
    private int maxHeartTimeoutRetryTimes = 3;

    // number of seconds to wait after initialization before starting the scheduler
    private int quartzStartDelaySeconds = 30;

    private String startPreparingJobCronExpression;

    private String checkRunningJobCronExpression;

    private String doCancelingJobCronExpression;

    private String destroyExecutorJobCronExpression;

}
