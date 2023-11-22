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

    private TaskFrameworkDeploySense sense = TaskFrameworkDeploySense.STANDALONE ;

    @NestedConfigurationProperty
    private K8sProperties k8sProperties;

    @Data
    public static class K8sProperties {
        private String url;
    }

}
