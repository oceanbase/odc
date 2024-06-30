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
package com.oceanbase.odc.common.jdbc;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class JdbcUrlParserTest {

    private final String jdbcUrl;
    private final String expectedType;
    private final String expectedHost;
    private final Integer expectedPort;
    private final String expectedDatabase;

    public JdbcUrlParserTest(String jdbcUrl, String expectedType, String expectedHost, Integer expectedPort,
            String expectedDatabase) {
        this.jdbcUrl = jdbcUrl;
        this.expectedType = expectedType;
        this.expectedHost = expectedHost;
        this.expectedPort = expectedPort;
        this.expectedDatabase = expectedDatabase;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                // with port in jdbc url
                {"jdbc:mysql://0.0.0.0:1234/testdb", "mysql", "0.0.0.0", 1234, "testdb"},
                {"jdbc:oracle://0.0.0.0:1521/testdb", "oracle", "0.0.0.0", 1521, "testdb"},
                {"jdbc:postgresql://0.0.0.0:5432/testdb", "postgresql", "0.0.0.0", 5432, "testdb"},
                {"jdbc:sqlserver://0.0.0.0:1433/testdb", "sqlserver", "0.0.0.0", 1433, "testdb"},
                {"jdbc:oceanbase://0.0.0.0:2883/testdb", "oceanbase", "0.0.0.0", 2883, "testdb"},

                // without port in jdbc url
                {"jdbc:mysql://0.0.0.0/testdb", "mysql", "0.0.0.0", 3306, "testdb"},
                {"jdbc:oracle://0.0.0.0/testdb", "oracle", "0.0.0.0", 1521, "testdb"},
                {"jdbc:postgresql://0.0.0.0/testdb", "postgresql", "0.0.0.0", 5432, "testdb"},
                {"jdbc:sqlserver://0.0.0.0/testdb", "sqlserver", "0.0.0.0", 1433, "testdb"},
                {"jdbc:oceanbase://0.0.0.0/testdb", "oceanbase", "0.0.0.0", 2883, "testdb"}
        });
    }

    @Test
    public void parser_validJdbcUrl_returnConnectionInfo() {
        JdbcUrlParser.ConnectionInfo connectionInfo = JdbcUrlParser.parse(jdbcUrl);

        Assert.assertEquals(expectedType, connectionInfo.getType());
        Assert.assertEquals(expectedHost, connectionInfo.getHost());
        Assert.assertEquals(expectedPort, connectionInfo.getPort());
        Assert.assertEquals(expectedDatabase, connectionInfo.getDatabase());
    }
}
