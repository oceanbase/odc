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

import java.util.List;
import java.util.function.Function;

import org.apache.commons.collections4.CollectionUtils;

import com.oceanbase.odc.core.migrate.resource.factory.EntityMapperFactory;
import com.oceanbase.odc.core.migrate.resource.mapper.EntityMapper;
import com.oceanbase.odc.core.migrate.resource.model.DataRecord;
import com.oceanbase.odc.core.migrate.resource.model.ResourceSpec;
import com.oceanbase.odc.core.migrate.resource.repository.DataRecordRepository;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link ResourceSpecMigrator}
 *
 * @author yh263208
 * @date 2022-04-23 21:40
 * @since ODC_release_3.3.1
 */
@Slf4j
public class ResourceSpecMigrator {

    private final DataRecordRepository repository;
    private final EntityMapperFactory<ResourceSpec, List<DataRecord>> factory;
    private final Function<ResourceSpec, ResourceSpec> handle;

    public ResourceSpecMigrator(@NonNull DataRecordRepository repository,
            @NonNull EntityMapperFactory<ResourceSpec, List<DataRecord>> factory,
            @NonNull Function<ResourceSpec, ResourceSpec> handle) {
        this.factory = factory;
        this.repository = repository;
        this.handle = handle;
    }

    public ResourceSpecMigrator(@NonNull DataRecordRepository repository,
            @NonNull EntityMapperFactory<ResourceSpec, List<DataRecord>> factory) {
        this(repository, factory, s -> s);
    }

    public ResourceSpec migrate(@NonNull ResourceSpec resourceSpec) {
        ResourceSpec entity = handle.apply(resourceSpec);
        if (entity == null) {
            return null;
        }
        EntityMapper<ResourceSpec, List<DataRecord>> mapper = factory.generate(entity);
        List<DataRecord> recordList = mapper.entityToModel(entity);
        for (DataRecord record : recordList) {
            if (record.isAllowDuplicated()) {
                save(record);
            } else {
                List<DataRecord> savedRecords = repository.find(record);
                if (CollectionUtils.isEmpty(savedRecords)) {
                    save(record);
                } else {
                    savedRecords.forEach(DataRecord::refresh);
                }
            }
        }
        return entity;
    }

    public ResourceSpec refresh(@NonNull ResourceSpec resourceSpec) {
        ResourceSpec entity = handle.apply(resourceSpec);
        if (entity == null) {
            return null;
        }
        EntityMapper<ResourceSpec, List<DataRecord>> mapper = factory.generate(entity);
        List<DataRecord> recordList = mapper.entityToModel(entity);
        for (DataRecord record : recordList) {
            List<DataRecord> savedRecords = repository.find(record);
            if (CollectionUtils.isNotEmpty(savedRecords)) {
                savedRecords.forEach(DataRecord::refresh);
            }
        }
        return entity;
    }

    private void save(DataRecord record) {
        DataRecord saved = repository.save(record);
        if (log.isDebugEnabled()) {
            log.debug("Resource is saved successfully, resource={}", saved);
        }
        saved.refresh();
    }

}
