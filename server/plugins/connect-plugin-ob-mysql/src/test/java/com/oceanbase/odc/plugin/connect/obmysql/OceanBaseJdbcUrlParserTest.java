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

package com.oceanbase.odc.plugin.connect.obmysql;

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
 * {@link OceanBaseJdbcUrlParserTest}
 *
 * @author yh263208
 * @date 2022-09-29 17:28
 * @since ODC_release_3.5.0
 */
public class OceanBaseJdbcUrlParserTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void createParser_invalidJdbcUrl_expThrown() throws SQLException {
        String jdbcUrl = "jdbc://0.0.0.0:1234";

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Invalid jdbc url: " + jdbcUrl);
        new OceanBaseJdbcUrlParser(jdbcUrl);
    }

    @Test
    public void getHostEndPoints_singleHostEndPoint_returnSingleton() throws SQLException {
        JdbcUrlParser parser = new OceanBaseJdbcUrlParser("jdbc:oceanbase://0.0.0.0:1234");

        List<HostAddress> expect = new ArrayList<>();
        expect.add(new HostAddress("0.0.0.0", 1234));
        List<HostAddress> acutal = parser.getHostAddresses();
        Assert.assertEquals(expect, acutal);
    }

    @Test
    public void getHostEndPoints_multiHostEndPoint_returnMultiEndPoints() throws SQLException {
        JdbcUrlParser parser = new OceanBaseJdbcUrlParser("jdbc:oceanbase://0.0.0.0:1234,8.8.8.8:4321");

        List<HostAddress> expect = new ArrayList<>();
        expect.add(new HostAddress("0.0.0.0", 1234));
        expect.add(new HostAddress("8.8.8.8", 4321));
        List<HostAddress> acutal = parser.getHostAddresses();
        Assert.assertEquals(expect, acutal);
    }

    @Test
    public void getSchema_noSchemaExists_returnNull() throws SQLException {
        JdbcUrlParser parser = new OceanBaseJdbcUrlParser("jdbc:oceanbase://0.0.0.0:1234");
        Assert.assertNull(parser.getSchema());
    }

    @Test
    public void getSchema_schemaExists_returnNotNull() throws SQLException {
        JdbcUrlParser parser = new OceanBaseJdbcUrlParser("jdbc:oceanbase://0.0.0.0:1234/abcd");
        Assert.assertEquals("abcd", parser.getSchema());
    }

    @Test
    public void getParameters_noParametersExists_returnNotEmpty() throws SQLException {
        JdbcUrlParser parser = new OceanBaseJdbcUrlParser("jdbc:oceanbase://0.0.0.0:1234/abcd");
        Assert.assertFalse(parser.getParameters().isEmpty());
    }

    @Test
    public void getParameters_userParametersExists_returnNotEmpty() throws SQLException {
        JdbcUrlParser parser = new OceanBaseJdbcUrlParser("jdbc:oceanbase://0.0.0.0:1234/abcd?user=David");
        Assert.assertEquals("David", parser.getParameters().get("user"));
    }

}

