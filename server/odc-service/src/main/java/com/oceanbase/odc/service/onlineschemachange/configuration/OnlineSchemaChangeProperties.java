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
package com.oceanbase.odc.service.onlineschemachange.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

/**
 * @author yaobin
 * @date 2023-06-06
 * @since 4.2.0
 */
@Data
@RefreshScope
@Configuration
@ConfigurationProperties(prefix = "odc.osc")
public class OnlineSchemaChangeProperties {

    @NestedConfigurationProperty
    private OmsProperties oms;
    // in json array["obv1", "obv*",....]
    private String supportLockTableObVersionJson;

    private boolean enableFullVerify;

    // if use odc migrate tool
    private boolean useOdcMigrateTool = false;
    // if this url provided, use provided url, or use k8s pod to create new instance
    private String odcMigrateUrl = null;

    @Data
    public static class OmsProperties {
        private String url;
        private String authorization;
        private String region;

        /**
         * osc task will be failed when check oms project step failed time exceed this seconds
         */
        private int checkProjectStepFailedTimeoutSeconds;
        /**
         * check oms project process expression
         */
        private String checkProjectProgressCronExpression;
    }

}
