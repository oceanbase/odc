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
package com.oceanbase.odc.core.migrate.resource.mapper;

import java.util.List;
import java.util.stream.Collectors;

import com.oceanbase.odc.core.migrate.resource.model.DataRecord;
import com.oceanbase.odc.core.migrate.resource.model.ResourceSpec;
import com.oceanbase.odc.core.migrate.resource.model.TableTemplate;

import lombok.NonNull;

/**
 * {@link ResourceSpecDataRecordMapper}
 *
 * @author yh263208
 * @date 2022-04-25 11:52
 * @since ODC_release_3.3.1
 * @see com.oceanbase.odc.core.migrate.resource.mapper.EntityMapper
 */
public class ResourceSpecDataRecordMapper implements EntityMapper<ResourceSpec, List<DataRecord>> {

    private final EntityMapper<TableTemplate, List<DataRecord>> mapper;

    public ResourceSpecDataRecordMapper(@NonNull EntityMapper<TableTemplate, List<DataRecord>> mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<DataRecord> entityToModel(@NonNull ResourceSpec entity) {
        return entity.getTemplates().stream().filter(t -> !t.isIgnore())
                .flatMap(template -> mapper.entityToModel(template).stream()).collect(Collectors.toList());
    }

}
