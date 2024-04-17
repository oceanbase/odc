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
package com.oceanbase.odc.service.db.schema.syncer;

import java.util.Collections;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

import com.oceanbase.odc.core.shared.constant.DialectType;

import lombok.Data;

/**
 * @author gaoda.xy
 * @date 2024/4/17 15:36
 */
@Data
@RefreshScope
@Configuration
@ConfigurationProperties(prefix = "odc.database.schema.sync")
public class DBSchemaSyncProperties {

    private List<String> oracleExcludeSchemas;

    private List<String> mysqlExcludeSchemas;

    private List<String> obMysqlExcludeSchemas;

    private List<String> obOracleExcludeSchemas;

    public List<String> getExcludeSchemas(DialectType dialect) {
        if (dialect == DialectType.ORACLE) {
            return oracleExcludeSchemas;
        } else if (dialect == DialectType.MYSQL || dialect == DialectType.DORIS) {
            return mysqlExcludeSchemas;
        } else if (dialect == DialectType.OB_MYSQL || dialect == DialectType.ODP_SHARDING_OB_MYSQL) {
            return obMysqlExcludeSchemas;
        } else if (dialect == DialectType.OB_ORACLE) {
            return obOracleExcludeSchemas;
        } else {
            return Collections.emptyList();
        }
    }

}
