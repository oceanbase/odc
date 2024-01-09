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

package com.oceanbase.odc.plugin.connect.oracle;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.oceanbase.odc.core.shared.jdbc.HostAddress;
import com.oceanbase.odc.core.shared.jdbc.JdbcUrlParser;

/**
 * @author jingtian
 * @date 2023/11/28
 * @since ODC_release_4.2.4
 */
public class OracleJdbcUrlParserTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void createParser_invalidJdbcUrl_expThrown() throws SQLException {
        String jdbcUrl = "jdbc://0.0.0.0:1234";

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Invalid JDBC URL for Oracle: " + jdbcUrl);
        new OracleJdbcUrlParser(jdbcUrl);
    }

    @Test
    public void getHostAndPort_with_serviceName_in_jdbcUrl() throws SQLException {
        JdbcUrlParser parser = new OracleJdbcUrlParser("jdbc:oracle:thin:@//0.0.0.0:1234/serviceName");

        List<HostAddress> expect = new ArrayList<>();
        expect.add(new HostAddress("0.0.0.0", 1234));
        List<HostAddress> acutal = parser.getHostAddresses();
        Assert.assertEquals(expect, acutal);
    }

    @Test
    public void getHostAndPort_with_sid_in_jdbcUrl() throws SQLException {
        JdbcUrlParser parser = new OracleJdbcUrlParser("jdbc:oracle:thin:@0.0.0.0:1234:sid");

        List<HostAddress> expect = new ArrayList<>();
        expect.add(new HostAddress("0.0.0.0", 1234));
        List<HostAddress> acutal = parser.getHostAddresses();
        Assert.assertEquals(expect, acutal);
    }

    @Test
    public void getParameters_noParametersExists_returnEmpty() throws SQLException {
        JdbcUrlParser parser = new OracleJdbcUrlParser("jdbc:oracle:thin:@0.0.0.0:1234:sid");
        Assert.assertTrue(parser.getParameters().isEmpty());
    }

    @Test
    public void getParameters_userParametersExists_returnNotEmpty() throws SQLException {
        JdbcUrlParser parser = new OracleJdbcUrlParser("jdbc:oracle:thin:@0.0.0.0:1234:sid?user=David");
        Assert.assertEquals("David", parser.getParameters().get("user"));
    }
}
