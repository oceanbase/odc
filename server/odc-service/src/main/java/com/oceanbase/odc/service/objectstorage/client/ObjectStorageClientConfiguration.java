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
package com.oceanbase.odc.service.objectstorage.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.oceanbase.odc.service.cloud.model.CloudProvider;
import com.oceanbase.odc.service.objectstorage.cloud.CloudObjectStorage;
import com.oceanbase.odc.service.objectstorage.cloud.model.CloudEnvConfigurations;
import com.oceanbase.odc.service.objectstorage.operator.ObjectBlockOperator;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class ObjectStorageClientConfiguration {
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
    @Bean("objectStorageClient")
    @RefreshScope
    public ObjectStorageClient objectStorageClient() {
        log.info("objectStorageClient is initializing");
        CloudProvider cloudProvider = cloudEnvConfigurations.getObjectStorageConfiguration().getCloudProvider();
        return CloudProvider.NONE == cloudProvider ? localObjectStorageClient() : cloudObjectStorageClient();
    }

    private CloudObjectStorageClient cloudObjectStorageClient() {
        return new CloudObjectStorageClient(publicEndpointCloudObjectStorage, internalEndpointCloudObjectStorage,
                cloudEnvConfigurations.getObjectStorageConfiguration());
    }

    private LocalObjectStorageClient localObjectStorageClient() {
        return new LocalObjectStorageClient(blockOperator, blockSplitLength);
    }
}
