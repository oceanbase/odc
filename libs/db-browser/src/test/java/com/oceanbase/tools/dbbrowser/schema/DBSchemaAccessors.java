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
package com.oceanbase.tools.dbbrowser.schema;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.jdbc.core.JdbcTemplate;

import com.oceanbase.tools.dbbrowser.VersionUtils;
import com.oceanbase.tools.dbbrowser.schema.mysql.MySQLNoGreaterThan5740SchemaAccessor;
import com.oceanbase.tools.dbbrowser.schema.mysql.OBMySQLBetween220And225XSchemaAccessor;
import com.oceanbase.tools.dbbrowser.schema.mysql.OBMySQLBetween2260And2276SchemaAccessor;
import com.oceanbase.tools.dbbrowser.schema.mysql.OBMySQLBetween2277And3XSchemaAccessor;
import com.oceanbase.tools.dbbrowser.schema.mysql.OBMySQLSchemaAccessor;
import com.oceanbase.tools.dbbrowser.schema.oracle.OBOracleLessThan2270SchemaAccessor;
import com.oceanbase.tools.dbbrowser.schema.oracle.OBOracleLessThan400SchemaAccessor;
import com.oceanbase.tools.dbbrowser.schema.oracle.OBOracleSchemaAccessor;
import com.oceanbase.tools.dbbrowser.schema.oracle.OracleSchemaAccessor;
import com.oceanbase.tools.dbbrowser.util.ALLDataDictTableNames;

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
        if (VersionUtils.isGreaterThanOrEqualsTo(this.version, "4.0.0")) {
            // OB 版本 >= 4.0.0
            return new OBMySQLSchemaAccessor(this.jdbcTemplate);
        } else if (VersionUtils.isGreaterThan(this.version, "2.2.76")) {
            // OB 版本为 [2.2.77, 4.0.0)
            return new OBMySQLBetween2277And3XSchemaAccessor(this.jdbcTemplate);
        } else if (VersionUtils.isGreaterThan(this.version, "2.2.60")) {
            // OB 版本为 [2.2.60, 2.2.77)
            return new OBMySQLBetween2260And2276SchemaAccessor(this.jdbcTemplate);
        } else if (VersionUtils.isGreaterThan(this.version, "1.4.79")) {
            // OB 版本为 (1.4.79, 2.2.60)
            return new OBMySQLBetween220And225XSchemaAccessor(this.jdbcTemplate);
        }
        // OB 版本 <= 1.4.79
        throw new UnsupportedOperationException("Not supported yet");
    }

    public DBSchemaAccessor createOBOracle() {
        this.version = getOBVersion();
        if (VersionUtils.isGreaterThanOrEqualsTo(this.version, "4.0.0")) {
            // OB 版本 >= 4.0.0
            return new OBOracleSchemaAccessor(this.jdbcTemplate, new ALLDataDictTableNames());
        } else if (VersionUtils.isGreaterThanOrEqualsTo(this.version, "2.2.7")) {
            // OB 版本为 [2.2.7, 4.0.0)
            return new OBOracleLessThan400SchemaAccessor(this.jdbcTemplate, new ALLDataDictTableNames());
        } else {
            // OB 版本 < 2.2.7
            return new OBOracleLessThan2270SchemaAccessor(this.jdbcTemplate, new ALLDataDictTableNames());
        }
    }

    public DBSchemaAccessor createOracle() {
        return new OracleSchemaAccessor(this.jdbcTemplate, new ALLDataDictTableNames());
    }

    public DBSchemaAccessor createMysql() {
        return new MySQLNoGreaterThan5740SchemaAccessor(this.jdbcTemplate);
    }

}
