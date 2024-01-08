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

import com.oceanbase.odc.service.task.enums.TaskRunModeEnum;

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
public class TaskFrameworkProperties {

    private TaskRunModeEnum runMode;

    private String odcUrl;

    @NestedConfigurationProperty
    private K8sProperties k8s = new K8sProperties();

    // job will be expired after this duration
    private int jobExpiredDurationSeconds = 10 * 60;

    // single fetch job rows for schedule
    private int singleFetchJobRowsForSchedule = 100;

    // single fetch job rows to check report timeout or not
    private int singleFetchJobRowsForCheckReportTimeout = 100;

    // max fetch job rows to check there is expired between once job schedule
    private int maxFetchJobRowsForCheckExpired = 2000;

    // max retry times after report timeout
    private int maxRetryTimesAfterReportTimeout = 3;

    @Data
    public static class K8sProperties {
        private String kubeUrl;
        private String namespace;
        private String kubeConfig;
    }

}
