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
import java.util.List;

import com.oceanbase.odc.core.migrate.MigrateConfiguration;
import com.oceanbase.odc.core.migrate.resource.model.ResourceConfig;

/**
 * {@link AbstractWebModeMetaDB}
 *
 * @author yh263208
 * @date 2022-05-19 14:30
 * @since ODC_release_3.3.1
 */
public abstract class AbstractWebModeMetaDB extends AbstractMetaDBMigrate {

    @Override
    public MigrateConfiguration migrateConfiguration() {
        return MigrateConfiguration.builder()
                .dataSource(dataSource)
                .initVersion(getInitVersion())
                .resourceLocations(Arrays.asList("migrate/common", "migrate/oceanbase", "migrate/web", "migrate/rbac"))
                .basePackages(
                        Arrays.asList("com.oceanbase.odc.migrate.jdbc.common", "com.oceanbase.odc.migrate.jdbc.web"))
                .resourceConfigs(resourceConfigs())
                .build();
    }

    protected abstract List<ResourceConfig> resourceConfigs();

}
