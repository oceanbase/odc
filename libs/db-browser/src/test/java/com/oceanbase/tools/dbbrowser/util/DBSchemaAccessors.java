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
package com.oceanbase.tools.dbbrowser.util;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.jdbc.core.JdbcTemplate;

import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessorGenerator;

import lombok.extern.slf4j.Slf4j;

/**
 * @author jingtian
 */
@Slf4j
public class DBSchemaAccessors {

    private final JdbcTemplate jdbcTemplate;
    private String version;

    public DBSchemaAccessors(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    private String getOBVersion() {
        String sql = "show variables like 'version_comment'";
        String v = this.jdbcTemplate.query(sql, rs -> {
            if (!rs.next()) {
                return null;
            }
            return rs.getString(2);
        });
        if (StringUtils.isBlank(v)) {
            throw new IllegalStateException("failed to get OB's version, reason: result set is empty");
        }
        return parseObVersionComment(v);
    }

    private String getMySQLVersion() {
        String querySql = "show variables like 'innodb_version'";
        String dbVersion;
        dbVersion = this.jdbcTemplate.query(querySql, rs -> {
            if (!rs.next()) {
                return null;
            }
            return rs.getString(2);
        });
        if (StringUtils.isBlank(dbVersion)) {
            throw new IllegalStateException("failed to get MYSQL's version, reason: result set is empty");
        }
        return dbVersion;
    }

    private String parseObVersionComment(String obVersionComment) {
        Validate.notBlank(obVersionComment);
        String[] obVersion = obVersionComment.split("\\s+");
        if (obVersion.length < 4) {
            throw new IllegalArgumentException("version_comment get failed");
        }
        return obVersion[1];
    }

    public DBSchemaAccessor createOBMysql() {
        this.version = getOBVersion();
        return DBSchemaAccessorGenerator.createForOBMySQL(this.jdbcTemplate, null,
                this.version, null);
    }

    public DBSchemaAccessor createOBOracle() {
        this.version = getOBVersion();
        return DBSchemaAccessorGenerator.createForOBOracle(this.jdbcTemplate,
                this.version);
    }

    public DBSchemaAccessor createOracle() {
        return DBSchemaAccessorGenerator.createForOracle(this.jdbcTemplate);
    }

    public DBSchemaAccessor createMysql() {
        this.version = getMySQLVersion();
        return DBSchemaAccessorGenerator.createForMySQL(this.jdbcTemplate, this.version);
    }

}
