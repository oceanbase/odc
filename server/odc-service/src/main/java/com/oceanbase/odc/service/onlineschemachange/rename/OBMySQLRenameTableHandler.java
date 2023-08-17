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
package com.oceanbase.odc.service.onlineschemachange.rename;

import org.springframework.jdbc.core.JdbcOperations;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2023-06-15
 * @since 4.2.0
 */
@Slf4j
public class OBMySQLRenameTableHandler implements RenameTableHandler {
    private final JdbcOperations jdbcOperations;

    public OBMySQLRenameTableHandler(JdbcOperations jdbcOperations) {
        this.jdbcOperations = jdbcOperations;
    }

    @Override
    public void rename(String schema, String fromName, String toName) {
        String renameTableSql = String.format("RENAME TABLE %s TO %s",
                getWithSchema(schema, fromName),
                getWithSchema(schema, toName));
        jdbcOperations.execute(renameTableSql);
        log.info("Renamed {} TO {}",
                getWithSchema(schema, fromName),
                getWithSchema(schema, toName));
    }

    @Override
    public void rename(String schema, String originName, String oldName, String newName) {
        renameTables(schema, originName, oldName, newName);
    }

    private void renameTables(String schema, String originName, String oldName, String newName) {
        String renameAllTablesSql =
                String.format("RENAME TABLE %s TO %s, %s TO %s",
                        getWithSchema(schema, originName),
                        getWithSchema(schema, oldName),
                        getWithSchema(schema, newName),
                        getWithSchema(schema, originName));
        jdbcOperations.execute(renameAllTablesSql);
        log.info("Renamed {} TO {}, {} TO {}",
                getWithSchema(schema, originName),
                getWithSchema(schema, oldName),
                getWithSchema(schema, newName),
                getWithSchema(schema, originName));
    }


    private String getWithSchema(String schema, String tableName) {
        return schema + "." + tableName;
    }

}
