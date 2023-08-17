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
package com.oceanbase.odc.core.migrate.resource.factory;

import java.util.List;

import com.oceanbase.odc.core.migrate.resource.ResourceManager;
import com.oceanbase.odc.core.migrate.resource.mapper.EntityMapper;
import com.oceanbase.odc.core.migrate.resource.mapper.ResourceSpecDataRecordMapper;
import com.oceanbase.odc.core.migrate.resource.mapper.TableSpecDataSpecsMapper;
import com.oceanbase.odc.core.migrate.resource.mapper.TableTemplateDataRecordMapper;
import com.oceanbase.odc.core.migrate.resource.model.DataRecord;
import com.oceanbase.odc.core.migrate.resource.model.ResourceConfig;
import com.oceanbase.odc.core.migrate.resource.model.ResourceSpec;

import lombok.NonNull;

/**
 * {@link DefaultResourceMapperFactory}
 *
 * @author yh263208
 * @date 2022-04-25 11:58
 * @since ODC_release_3.3.1
 * @see com.oceanbase.odc.core.migrate.resource.factory.EntityMapperFactory
 */
public class DefaultResourceMapperFactory implements EntityMapperFactory<ResourceSpec, List<DataRecord>> {

    private final ResourceConfig config;
    private final ResourceManager manager;

    public DefaultResourceMapperFactory(@NonNull ResourceConfig config, @NonNull ResourceManager manager) {
        this.config = config;
        this.manager = manager;
    }

    @Override
    public EntityMapper<ResourceSpec, List<DataRecord>> generate(@NonNull ResourceSpec target) {
        TableSpecDataSpecsMapper specsMapper = new TableSpecDataSpecsMapper(target,
                config.getValueEncoderFactory(), config.getValueGeneratorFactory(), manager, config.getDataSource());
        TableTemplateDataRecordMapper templateMapper = new TableTemplateDataRecordMapper(specsMapper);
        return new ResourceSpecDataRecordMapper(templateMapper);
    }

}
