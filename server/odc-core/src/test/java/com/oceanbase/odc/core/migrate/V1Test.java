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

import javax.sql.DataSource;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yizhou.xw
 * @version : V1Test.java, v 0.1 2021-03-26 20:38
 */
@Slf4j
@Migratable(version = "1.1.1", description = "wow")
public class V1Test implements JdbcMigratable {

    @Override
    public void migrate(DataSource dataSource) {
        log.info("V1Test migrate");
    }
}
