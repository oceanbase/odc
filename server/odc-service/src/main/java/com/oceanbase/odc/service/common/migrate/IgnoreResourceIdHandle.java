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
package com.oceanbase.odc.service.common.migrate;

import java.util.List;
import java.util.function.Function;

import org.apache.commons.collections4.CollectionUtils;

import com.oceanbase.odc.core.migrate.resource.model.ResourceSpec;
import com.oceanbase.odc.core.migrate.resource.model.TableSpec;
import com.oceanbase.odc.core.migrate.resource.model.TableTemplate;

/**
 * {@link IgnoreResourceIdHandle}
 *
 * @author yh263208
 * @date 2022-04-27 11:22
 * @since ODC_release_3.3.0
 */
public class IgnoreResourceIdHandle implements Function<ResourceSpec, ResourceSpec> {

    @Override
    public ResourceSpec apply(ResourceSpec entity) {
        List<TableTemplate> templateEntities = entity.getTemplates();
        if (CollectionUtils.isEmpty(templateEntities)) {
            return entity;
        }
        for (TableTemplate tableTemplate : templateEntities) {
            List<TableSpec> specEntities = tableTemplate.getSpecs();
            if (CollectionUtils.isEmpty(specEntities)) {
                continue;
            }
            for (TableSpec tableSpec : specEntities) {
                if (!"id".equals(tableSpec.getName())) {
                    continue;
                }
                tableSpec.setIgnore(true);
            }
        }
        return entity;
    }

}
