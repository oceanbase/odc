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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.core.migrate.MigrateConfiguration;
import com.oceanbase.odc.core.migrate.resource.model.ResourceConfig;
import com.oceanbase.odc.service.common.migrate.DefaultValueEncoderFactory;
import com.oceanbase.odc.service.common.migrate.DefaultValueGeneratorFactory;
import com.oceanbase.odc.service.common.migrate.ResourceConstants;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@ConditionalOnExpression("#{environment.acceptsProfiles('clientMode') && !environment.acceptsProfiles('test')}")
@Component("metadbMigrate")
@DependsOn({"localObjectStorageFacade", "springContextUtil"})
public class DesktopModeMetaDB extends AbstractMetaDBMigrate {

    @Override
    public MigrateConfiguration migrateConfiguration() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.putIfAbsent(ResourceConstants.CREATOR_ID_PLACEHOLDER_NAME, 1L);
        parameters.putIfAbsent(ResourceConstants.ORGANIZATION_ID_PLACEHOLDER_NAME, 1L);

        ResourceConfig resourceConfig = ResourceConfig.builder()
                .valueEncoderFactory(new DefaultValueEncoderFactory())
                .valueGeneratorFactory(new DefaultValueGeneratorFactory())
                .handle(new DesktopResourceHandle())
                .variables(parameters).build();

        return MigrateConfiguration.builder()
                .dataSource(dataSource)
                .initVersion(getInitVersion())
                .resourceLocations(getResourceLocations())
                .basePackages(getBasePackages())
                .resourceConfigs(Collections.singletonList(resourceConfig))
                .build();
    }

    protected List<String> getResourceLocations() {
        return Arrays.asList("migrate/common", "migrate/h2", "migrate/web", "migrate/rbac");
    }

    protected List<String> getBasePackages() {
        return Arrays.asList("com.oceanbase.odc.migrate.jdbc.common",
                "com.oceanbase.odc.migrate.jdbc.web", "com.oceanbase.odc.migrate.jdbc.desktop");
    }

}
