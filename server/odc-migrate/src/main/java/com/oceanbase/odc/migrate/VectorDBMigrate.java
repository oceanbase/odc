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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.integration.jdbc.lock.JdbcLockRegistry;

import com.oceanbase.odc.core.migrate.MigrateConfiguration;
import com.oceanbase.odc.core.migrate.resource.model.ResourceConfig;
import com.oceanbase.odc.service.common.migrate.DefaultValueEncoderFactory;
import com.oceanbase.odc.service.common.migrate.DefaultValueGeneratorFactory;
import com.oceanbase.odc.service.common.migrate.ResourceConstants;

@Configuration
@DependsOn({"vectordbLockConfiguration"})
@ConditionalOnProperty(prefix = "odc.datasource.vectordb",
        name = {"jdbc-url", "driver-class-name", "username", "password"})
public class VectorDBMigrate extends AbstractMigrate {

    @Autowired
    @Qualifier("vectordbDataSource")
    protected DataSource vectordbDataSource;
    @Autowired
    @Qualifier("vectordbJdbcLockRepository")
    private JdbcLockRegistry vectordbJdbcLockRepository;

    @PostConstruct
    public void migrate() throws InterruptedException {
        doMigrate(this.vectordbJdbcLockRepository);
    }

    @Override
    public MigrateConfiguration migrateConfiguration() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.putIfAbsent(ResourceConstants.CREATOR_ID_PLACEHOLDER_NAME, 1L);
        parameters.putIfAbsent(ResourceConstants.ORGANIZATION_ID_PLACEHOLDER_NAME, 1L);

        ResourceConfig resourceConfig = ResourceConfig.builder()
                .valueEncoderFactory(new DefaultValueEncoderFactory())
                .valueGeneratorFactory(new DefaultValueGeneratorFactory())
                .variables(parameters).build();

        return MigrateConfiguration.builder()
                .dataSource(this.vectordbDataSource)
                .initVersion("2.0.0")
                .resourceLocations(Collections.singletonList("migrate/vectordb"))
                .basePackages(Collections.singletonList("com.oceanbase.odc.migrate.jdbc.vectordb"))
                .resourceConfigs(Collections.singletonList(resourceConfig))
                .build();
    }

}
