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
package com.oceanbase.odc.migrate.jdbc.common;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;

import com.oceanbase.odc.core.migrate.JdbcMigratable;
import com.oceanbase.odc.core.migrate.Migratable;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Migratable(version = "4.4.0.0", description = "iam_role_permission migrate")
public class V4400AddReentrantAddColumnProcedureMigrate implements JdbcMigratable {
    @Override
    public void migrate(DataSource dataSource) {
        if(isH2Database(dataSource)) {
            log.info("Skip add procedure migrating for H2 database");
            return;
        }
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        String sql = """
            CREATE PROCEDURE  AddColumnIfNotExists(
                IN tableName VARCHAR(255),
                IN columnName VARCHAR(255),
                IN columnDefinition VARCHAR(255)
            )
            BEGIN
                DECLARE column_exists INT;

                -- 检查列是否存在
                SET @table_name = tableName;
                SET @column_name = columnName;

                SELECT COUNT(*)
                INTO column_exists
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = @table_name
                  AND COLUMN_NAME = @column_name;

                -- 如果列不存在，则添加列
                IF column_exists = 0 THEN
                    SET @sql = CONCAT('ALTER TABLE ', @table_name, ' ADD COLUMN ', @column_name, ' ', columnDefinition);
                    PREPARE stmt FROM @sql;
                    EXECUTE stmt;
                    DEALLOCATE PREPARE stmt;
                    SELECT CONCAT('Column "', @column_name, '" added successfully.') AS Message;
                ELSE
                    SELECT CONCAT('Column "', @column_name, '" already exists.') AS Message;
                END IF;
            END;
            """;


        jdbcTemplate.execute(sql);

        String foreignKeysql = """
            CREATE PROCEDURE AddForeignKeyIfNotExists(
                IN tableName VARCHAR(255),
                IN constraintName VARCHAR(255),
                IN columnName VARCHAR(255),
                IN referencedTable VARCHAR(255),
                IN referencedColumn VARCHAR(255)
            )
            BEGIN
                DECLARE foreignKeyCount INT;

                -- 检查外键是否存在
                SET @table_name = tableName;
                SET @constraint_name = constraintName;

                SELECT COUNT(*)
                INTO foreignKeyCount
                FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS tc
                JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE kcu
                  ON tc.CONSTRAINT_NAME = kcu.CONSTRAINT_NAME
                WHERE tc.TABLE_SCHEMA = DATABASE()
                  AND tc.TABLE_NAME = @table_name
                  AND tc.CONSTRAINT_NAME = @constraint_name;

                -- 如果外键不存在，则添加外键
                IF foreignKeyCount = 0 THEN
                    SET @sql = CONCAT(
                        'ALTER TABLE ', @table_name,
                        ' ADD CONSTRAINT ', @constraint_name,
                        ' FOREIGN KEY (', columnName, ')',
                        ' REFERENCES ', referencedTable, '(', referencedColumn, ')'
                    );
                    PREPARE stmt FROM @sql;
                    EXECUTE stmt;
                    DEALLOCATE PREPARE stmt;
                    SELECT CONCAT('Foreign key "', @constraint_name, '" added successfully.') AS Message;
                ELSE
                    SELECT CONCAT('Foreign key "', @constraint_name, '" already exists.') AS Message;
                END IF;
            END;
            """;

        jdbcTemplate.execute(foreignKeysql);

    }

    private boolean isH2Database(DataSource dataSource) {
        try (Connection conn = dataSource.getConnection()) {
            return conn.getMetaData().getDatabaseProductName().contains("H2");
        } catch (SQLException e) {
            return false;
        }
    }
}
