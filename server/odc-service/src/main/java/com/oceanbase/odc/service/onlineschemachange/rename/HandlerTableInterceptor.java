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

import com.oceanbase.odc.service.onlineschemachange.model.OriginTableCleanStrategy;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2023-08-07
 * @since 4.2.0
 */
@Slf4j
public class HandlerTableInterceptor implements RenameTableInterceptor {

    private final JdbcOperations jdbcOperations;

    public HandlerTableInterceptor(JdbcOperations jdbcOperations) {
        this.jdbcOperations = jdbcOperations;
    }

    @Override
    public void preRename(RenameTableParameters parameters) {

    }

    @Override
    public void renameSucceed(RenameTableParameters parameters) {
        dropOldTable(parameters);
    }

    @Override
    public void renameFailed(RenameTableParameters parameters) {
        dropNewTable(parameters);
    }

    @Override
    public void postRenamed(RenameTableParameters parameters) {

    }

    private void dropOldTable(RenameTableParameters parameters) {
        if (parameters.getOriginTableCleanStrategy() == OriginTableCleanStrategy.ORIGIN_TABLE_DROP) {
            log.info("Because origin table clean strategy is {}, so we drop the old table. ",
                    parameters.getOriginTableCleanStrategy());
            String oldTable = getWithSchema(parameters.getSchemaName(), parameters.getRenamedTableName());
            dropTable(oldTable, "Drop old table {} occur error {} ");
        }
    }

    private void dropNewTable(RenameTableParameters parameters) {
        String newTable = getWithSchema(parameters.getSchemaName(), parameters.getNewTableName());
        dropTable(newTable, "Drop new table {} occur error {} ");
    }

    private void dropTable(String tableName, String s) {
        try {
            // drop table
            jdbcOperations.execute("DROP TABLE " + tableName);
            log.info("DROP TABLE {}", tableName);
        } catch (Exception exception) {
            log.warn(s, tableName, exception.getMessage());
        }
    }

    private String getWithSchema(String schema, String tableName) {
        return schema + "." + tableName;
    }

}
