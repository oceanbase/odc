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
package com.oceanbase.odc.core.migrate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.sql.DataSource;

import com.oceanbase.odc.core.migrate.resource.model.ResourceConfig;

import lombok.Builder;
import lombok.Data;

/**
 * @author yizhou.xw
 * @version : MigrateConfiguration.java, v 0.1 2021-03-23 16:24
 */
@Data
@Builder
public class MigrateConfiguration {
    private static final String DEFAULT_TABLE = "migrate_schema_history";

    /**
     * dataSource for connect to target database, the history table will also be created into
     */
    private DataSource dataSource;

    /**
     * schema history table name
     */
    @Builder.Default
    private String historyTable = DEFAULT_TABLE;

    /**
     * if run in dry mode
     */
    @Builder.Default
    private boolean dryRun = false;

    @Builder.Default
    private List<String> resourceLocations = new ArrayList<>(Collections.singletonList("migrate/common"));

    @Builder.Default
    private List<String> basePackages = new ArrayList<>();

    /**
     * init version, if not empty, the migratable who's version less or equal than initVersion will be
     * ignored even no matched history found
     */
    private String initVersion;

    @Builder.Default
    private List<ResourceConfig> resourceConfigs = new LinkedList<>();

}
