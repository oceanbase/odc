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
package com.oceanbase.odc.service.objectstorage.pure;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.oceanbase.odc.service.cloud.model.CloudProvider;
import com.oceanbase.odc.service.objectstorage.CloudEnvironmentObjectStorageFacade;
import com.oceanbase.odc.service.objectstorage.LocalObjectStorageFacade;
import com.oceanbase.odc.service.objectstorage.ObjectStorageFacade;
import com.oceanbase.odc.service.objectstorage.cloud.CloudObjectStorage;
import com.oceanbase.odc.service.objectstorage.cloud.model.CloudEnvConfigurations;
import com.oceanbase.odc.service.objectstorage.operator.ObjectBlockOperator;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class PureObjectStorageConfiguration {
    @Autowired
    private CloudEnvConfigurations cloudEnvConfigurations;

    @Autowired
    @Qualifier("publicEndpointCloudClient")
    CloudObjectStorage publicEndpointCloudObjectStorage;
    @Autowired
    @Qualifier("internalEndpointCloudClient")
    CloudObjectStorage internalEndpointCloudObjectStorage;
    @Autowired
    private ObjectBlockOperator blockOperator;
    @Value("${odc.objectstorage.default-block-split-length:#{1024*1024}}")
    private long blockSplitLength = 1024 * 1024L;

    @Primary
    @Bean("objectStorageFacade")
    @RefreshScope
    public PureObjectStorage pureObjectStorage() {
        log.info("pureObjectStorage is initializing");
        CloudProvider cloudProvider = cloudEnvConfigurations.getObjectStorageConfiguration().getCloudProvider();
        return CloudProvider.NONE == cloudProvider ? localPureObjectStorage() : cloudPureObjectStorage();
    }

    private CloudPureObjectStorage cloudPureObjectStorage() {
        return new CloudPureObjectStorage(publicEndpointCloudObjectStorage, internalEndpointCloudObjectStorage,
                cloudEnvConfigurations.getObjectStorageConfiguration());
    }

    private LocalPureObjectStorage localPureObjectStorage() {
        return new LocalPureObjectStorage(blockOperator, blockSplitLength);
    }


}
