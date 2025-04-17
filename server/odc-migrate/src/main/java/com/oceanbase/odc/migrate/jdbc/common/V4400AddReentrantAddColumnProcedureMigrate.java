/*
 * Copyright (c) 2025 OceanBase.
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
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        String sql = "CREATE PROCEDURE if not exist AddColumnIfNotExists(\n"
                     + "  IN tableName VARCHAR(255),\n"
                     + "  IN columnName VARCHAR(255),\n"
                     + "  IN columnDefinition VARCHAR(255)\n"
                     + ")\n"
                     + "BEGIN\n"
                     + "    DECLARE column_exists INT;\n"
                     + "\n"
                     + "    SELECT COUNT(*)\n"
                     + "    INTO column_exists\n"
                     + "    FROM INFORMATION_SCHEMA.COLUMNS\n"
                     + "    WHERE TABLE_SCHEMA = DATABASE()\n"
                     + "      AND TABLE_NAME = tableName\n"
                     + "      AND COLUMN_NAME = columnName;\n"
                     + "\n"
                     + "    IF column_exists = 0 THEN\n"
                     + "        SET @sql = CONCAT('ALTER TABLE ', tableName, ' ADD COLUMN ', columnName, ' ', columnDefinition);\n"
                     + "    ELSE\n"
                     + "        SET @sql = 'SELECT ''Column already exists'' AS status';\n"
                     + "    END IF;\n"
                     + "\n"
                     + "    PREPARE stmt FROM @sql;\n"
                     + "    EXECUTE stmt;\n"
                     + "    DEALLOCATE PREPARE stmt;\n"
                     + "END";

        jdbcTemplate.execute(sql);

        String foreignKeysql = "CREATE PROCEDURE if not exist AddForeignKeyIfNotExists(\n"
                      + "    IN tableName VARCHAR(255),\n"
                      + "    IN constraintName VARCHAR(255),\n"
                      + "    IN columnName VARCHAR(255),\n"
                      + "    IN referencedTable VARCHAR(255),\n"
                      + "    IN referencedColumn VARCHAR(255)\n"
                      + ")\n"
                      + "BEGIN\n"
                      + "    DECLARE foreignKeyCount INT;\n"
                      + "\n"
                      + "    -- 检查外键是否存在\n"
                      + "    SELECT COUNT(*)\n"
                      + "    INTO foreignKeyCount\n"
                      + "    FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS tc\n"
                      + "    JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE kcu\n"
                      + "      ON tc.CONSTRAINT_NAME = kcu.CONSTRAINT_NAME\n"
                      + "    WHERE tc.TABLE_SCHEMA = DATABASE()\n"
                      + "      AND tc.TABLE_NAME = tableName\n"
                      + "      AND tc.CONSTRAINT_NAME = constraintName;\n"
                      + "\n"
                      + "    -- 如果外键不存在，则添加外键\n"
                      + "    IF foreignKeyCount = 0 THEN\n"
                      + "        SET @sql = CONCAT(\n"
                      + "            'ALTER TABLE ', tableName,\n"
                      + "            ' ADD CONSTRAINT ', constraintName,\n"
                      + "            ' FOREIGN KEY (', columnName, ')',\n"
                      + "            ' REFERENCES ', referencedTable, '(', referencedColumn, ')'\n"
                      + "        );\n"
                      + "        PREPARE stmt FROM @sql;\n"
                      + "        EXECUTE stmt;\n"
                      + "        DEALLOCATE PREPARE stmt;\n"
                      + "        SELECT CONCAT('Foreign key \"', constraintName, '\" added successfully.') AS Message;\n"
                      + "    ELSE\n"
                      + "        SELECT CONCAT('Foreign key \"', constraintName, '\" already exists.') AS Message;\n"
                      + "    END IF;\n"
                      + "END";

        jdbcTemplate.execute(foreignKeysql);

    }
}
