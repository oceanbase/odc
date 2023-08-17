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

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;

import com.oceanbase.odc.common.util.ListUtils;
import com.oceanbase.odc.core.migrate.resource.model.DataRecord;
import com.oceanbase.odc.core.migrate.resource.model.DataSpec;
import com.oceanbase.odc.core.migrate.resource.model.TableMetaData;
import com.oceanbase.odc.core.migrate.resource.model.TableSpec;
import com.oceanbase.odc.core.migrate.resource.model.TableTemplate;

import lombok.NonNull;

/**
 * {@link TableTemplateDataRecordMapper} for
 * {@link com.oceanbase.odc.core.migrate.resource.model.DataRecord}
 *
 * @author yh263208
 * @date 2022-04-22 18:09
 * @since ODC_release_3.3.1
 */
public class TableTemplateDataRecordMapper implements EntityMapper<TableTemplate, List<DataRecord>> {

    private final EntityMapper<TableSpec, List<DataSpec>> mapper;

    public TableTemplateDataRecordMapper(@NonNull EntityMapper<TableSpec, List<DataSpec>> mapper) {
        this.mapper = mapper;
    }

    /**
     * Generate a {@link DataRecord}, which stands for a line of record in metadb
     *
     * @param entity {@link TableTemplate}
     * @return list of {@link DataRecord}
     */
    @Override
    public List<DataRecord> entityToModel(@NonNull TableTemplate entity) {
        List<TableSpec> specs = entity.getSpecs();
        if (CollectionUtils.isEmpty(specs)) {
            return Collections.emptyList();
        }
        TableMetaData meta = entity.getMetadata();
        Validate.notNull(meta, "MetaData can not be null");
        List<List<DataSpec>> list = specs.stream().map(mapper::entityToModel).collect(Collectors.toList());
        return ListUtils.cartesianProduct(list).stream().map(s -> new DataRecord(meta, s)).collect(Collectors.toList());
    }

}
