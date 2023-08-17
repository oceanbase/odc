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
package com.oceanbase.odc.service.objectstorage.cloud;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.service.config.SystemConfigService;
import com.oceanbase.odc.service.config.model.Configuration;
import com.oceanbase.odc.service.objectstorage.cloud.model.CloudEnvConfigurations;
import com.oceanbase.odc.service.objectstorage.cloud.model.ObjectStorageConfiguration.CloudProvider;

@Component
public class CloudResourceSystemConfigAdaptor {
    @Autowired
    private SystemConfigService systemConfigService;
    @Autowired
    private CloudEnvConfigurations cloudEnvConfigurations;
    private CloudProvider cloudProvider;

    @PostConstruct
    public void init() {
        cloudProvider = cloudEnvConfigurations.getObjectStorageConfiguration().getCloudProvider();
        systemConfigService.addConfigurationConsumer(this::consumeSystemConfig);
    }

    private void consumeSystemConfig(Configuration configuration) {
        String key = configuration.getKey();
        if (cloudProvider == CloudProvider.NONE) {
            return;
        }
        if (StringUtils.equals("odc.file.interaction-mode", key)) {
            configuration.setValue("CLOUD_STORAGE");
        }
    }
}
