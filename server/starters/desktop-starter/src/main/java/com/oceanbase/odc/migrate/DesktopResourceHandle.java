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
package com.oceanbase.odc.migrate;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.apache.commons.collections4.CollectionUtils;

import com.oceanbase.odc.core.migrate.resource.model.ResourceSpec;
import com.oceanbase.odc.core.migrate.resource.model.TableSpec;
import com.oceanbase.odc.core.migrate.resource.model.TableTemplate;
import com.oceanbase.odc.service.common.migrate.IgnoreResourceIdHandle;
import com.oceanbase.odc.service.common.util.SpringContextUtil;

public class DesktopResourceHandle implements Function<ResourceSpec, ResourceSpec> {
    private static final Set<String> RESERVE_RESOURCE_ID_TABLES = new HashSet<>();

    static {
        RESERVE_RESOURCE_ID_TABLES.add("iam_organization");
    }

    @Override
    public ResourceSpec apply(ResourceSpec entity) {
        Set<String> profiles = new HashSet<>(Arrays.asList(SpringContextUtil.getProfiles()));
        if (!profiles.contains("test")) {
            if (entity.getTemplates().stream()
                    .anyMatch(t -> RESERVE_RESOURCE_ID_TABLES.contains(t.getMetadata().getTable()))) {
                return entity;
            }
            return new IgnoreResourceIdHandle().apply(entity);
        }
        /**
         * test 模式下，所有列都允许为空，避免单元测试中删除内置资源而引发的引用错误
         */
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
                tableSpec.setAllowNull(true);
            }
        }
        return entity;
    }

}
