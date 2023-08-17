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
package com.oceanbase.odc.service.objectstorage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.oceanbase.odc.service.objectstorage.cloud.model.CloudEnvConfigurations;
import com.oceanbase.odc.service.objectstorage.cloud.model.ObjectStorageConfiguration.CloudProvider;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class ObjectStorageFacadeConfiguration {
    @Autowired
    private CloudEnvConfigurations cloudEnvConfigurations;

    @Autowired
    @Qualifier("cloudEnvironmentObjectStorageFacade")
    private CloudEnvironmentObjectStorageFacade cloudEnvironmentObjectStorageFacade;

    @Autowired
    @Qualifier("localObjectStorageFacade")
    private LocalObjectStorageFacade localObjectStorageFacade;

    @Primary
    @Bean("objectStorageFacade")
    @RefreshScope
    public ObjectStorageFacade objectStorageFacade() {
        log.info("objectStorageFacade is initializing");
        CloudProvider cloudProvider = cloudEnvConfigurations.getObjectStorageConfiguration().getCloudProvider();
        return CloudProvider.NONE == cloudProvider ? localObjectStorageFacade : cloudEnvironmentObjectStorageFacade;
    }
}
