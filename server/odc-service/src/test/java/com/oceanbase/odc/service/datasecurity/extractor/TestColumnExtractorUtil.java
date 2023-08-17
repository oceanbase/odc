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
package com.oceanbase.odc.service.datasecurity.extractor;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import org.apache.commons.io.IOUtils;

import com.oceanbase.odc.common.util.YamlUtils;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.shared.exception.UnsupportedException;

import lombok.Data;

/**
 * @author gaoda.xy
 * @date 2023/6/8 09:43
 */
public class TestColumnExtractorUtil {
    private static final String SQL_FILE = "src/test/resources/datasecurity/test_sensitive_column_extractor_sql.yaml";

    private static final TestSql TEST_SQL = getTestSql();

    public static String getTestSql(DialectType dialectType, String key) {
        if (dialectType == DialectType.OB_MYSQL) {
            return TEST_SQL.getOBMysqlSql(key);
        } else if (dialectType == DialectType.OB_ORACLE) {
            return TEST_SQL.getOBOracleSql(key);
        }
        throw new UnsupportedException("Unsupported dialect type");
    }

    private static TestSql getTestSql() {
        String sqlYaml;
        try (InputStream in = new BufferedInputStream(Files.newInputStream(Paths.get(SQL_FILE)))) {
            sqlYaml = IOUtils.toString(in, String.valueOf(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return YamlUtils.from(sqlYaml, TestSql.class);
    }

    @Data
    private static class TestSql {
        private Map<String, String> mysql;
        private Map<String, String> oracle;

        public String getOBMysqlSql(String id) {
            return mysql.get(id);
        }

        public String getOBOracleSql(String id) {
            return oracle.get(id);
        }
    }

}
