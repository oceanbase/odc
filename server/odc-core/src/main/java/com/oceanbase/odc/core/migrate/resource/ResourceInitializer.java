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
package com.oceanbase.odc.core.migrate.resource;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import com.oceanbase.odc.core.migrate.resource.factory.DefaultResourceMapperFactory;
import com.oceanbase.odc.core.migrate.resource.factory.EntityMapperFactory;
import com.oceanbase.odc.core.migrate.resource.model.DataRecord;
import com.oceanbase.odc.core.migrate.resource.model.ResourceConfig;
import com.oceanbase.odc.core.migrate.resource.model.ResourceSpec;
import com.oceanbase.odc.core.migrate.resource.repository.DataRecordRepository;

import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link ResourceInitializer}
 *
 * @author yh263208
 * @date 2022-04-23 22:29
 * @since ODC_release_3.3.1
 */
@Slf4j
public class ResourceInitializer {
    @Getter
    private final ResourceConfig config;
    private final DataRecordRepository recordRepository;

    public ResourceInitializer(@NonNull ResourceConfig config) {
        this.config = config;
        this.recordRepository = new DataRecordRepository(config.getDataSource());
    }

    public void init() throws IOException {
        ResourceManager manager = new ResourceManager(config.getVariables(), config.getResourceLocations());
        EntityMapperFactory<ResourceSpec, List<DataRecord>> factory =
                new DefaultResourceMapperFactory(config, manager);
        ResourceSpecMigrator migrator = new ResourceSpecMigrator(recordRepository, factory, config.getHandle());
        for (URL location : manager.getResourceUrls()) {
            ResourceSpec resourceSpec = manager.findByUrl(location);
            migrator.migrate(resourceSpec);
            log.info("Resource migration is complete, location={}", ResourceManager.getShortFilePath(location));
        }
    }

}
