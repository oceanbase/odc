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
package com.oceanbase.odc.service.connection;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.oceanbase.odc.core.shared.exception.BadArgumentException;
import com.oceanbase.odc.service.connection.model.ConnectionStringParseResult;
import com.oceanbase.odc.service.connection.model.OdcConnectionParseResult;
import com.oceanbase.odc.service.connection.util.MySQLClientArgsParser;

public class MySQLClientArgsParserTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void parse2_withClusterWithMultipleWhitespace_expectSuccess() {
        ConnectionStringParseResult expected = new ConnectionStringParseResult();
        expected.setClusterName("C1");
        expected.setTenantName("tenant1");
        expected.setHost("127.0.0.1");
        expected.setPort(2883);
        expected.setUsername("root");
        expected.setDefaultSchema("oceanbase");
        expected.setPassword("pwd");

        String connStr = "obclient -h127.0.0.1  -P2883   -uroot@tenant1#C1 -Doceanbase -ppwd";
        ConnectionStringParseResult result = MySQLClientArgsParser.parse2(connStr);

        Assert.assertEquals(expected, result);
    }

    @Test
    public void parse_withClusterWithMultipleWhitespace_expectSuccess() {
        String connStr = "obclient -h127.0.0.1  -P2883   -uroot@tenant1#C1 -Doceanbase -ppwd";
        OdcConnectionParseResult session = MySQLClientArgsParser.parse(connStr);

        OdcConnectionParseResult expected = buildSession();
        expected.setTenant("tenant1");

        Assert.assertEquals(expected, session);
    }

    @Test
    public void parse_sysTenantWithDoubleQuote_expectMySqlMode() {
        String connStr = "obclient -h\"127.0.0.1\" -P2883 -u\"root@sys#C1\" -D\"oceanbase\" -p\"pwd\"";
        OdcConnectionParseResult session = MySQLClientArgsParser.parse(connStr);

        OdcConnectionParseResult expected = buildSession();
        expected.setDbMode("OB_MYSQL");
        Assert.assertEquals(expected, session);
    }

    @Test
    public void parse_withoutClusterLongOptionWithTab_expectSuccess() {
        String connStr =
                "obclient --host=127.0.0.1 --port 2883 \t --user=root@orcl1 --database=oceanbase --password=pwd";
        OdcConnectionParseResult session = MySQLClientArgsParser.parse(connStr);

        OdcConnectionParseResult expected = buildSession();
        expected.setCluster(null);
        expected.setTenant("orcl1");
        Assert.assertEquals(expected, session);
    }

    @Test
    public void parse_userWithDoubleAt_expectException() {
        String connStr = "obclient --user=root@@sys";

        thrown.expect(BadArgumentException.class);

        MySQLClientArgsParser.parse(connStr);
    }

    @Test
    public void parse_userWithDoubleNumber_expectException() {
        String connStr = "obclient --user=root@sys#C1#2";

        thrown.expect(BadArgumentException.class);

        MySQLClientArgsParser.parse(connStr);
    }

    @Test
    public void parse_empty_expectException() {
        thrown.expectMessage("parameter commandLineStr may not be empty");

        MySQLClientArgsParser.parse("");
    }

    @Test
    public void parse_onlyProgramMysql_expectModeMysql() {
        OdcConnectionParseResult session = MySQLClientArgsParser.parse("mysql");

        Assert.assertEquals("OB_MYSQL", session.getDbMode());
    }

    @Test
    public void parse_onlyProgramObClient_expectModeNull() {
        OdcConnectionParseResult session = MySQLClientArgsParser.parse("obclient ");

        Assert.assertNull(session.getDbMode());
    }

    @Test
    public void parse_containsUnknownOption_expectException() {
        String connStr = "obclient -h127.0.0.1  -P2883   -uroot@tenant1#C1 -Doceanbase -ppwd --unknown=some";

        thrown.expect(BadArgumentException.class);

        MySQLClientArgsParser.parse(connStr);
    }

    @Test
    public void parse_passwordWrapperWithSingleQuote_expectMatch() {
        String connStr = "obclient -p'pwd'";
        OdcConnectionParseResult session = MySQLClientArgsParser.parse(connStr);

        Assert.assertEquals("pwd", session.getPassword());
    }

    @Test
    public void parse_passwordShortOptionWithoutValue_expectPasswordNull() {
        String connStr = "obclient -p";
        OdcConnectionParseResult session = MySQLClientArgsParser.parse(connStr);

        Assert.assertNull(session.getPassword());
    }

    @Test
    public void parse_passwordLongOptionWithoutValue_expectPasswordNull() {
        String connStr = "obclient --password";
        OdcConnectionParseResult session = MySQLClientArgsParser.parse(connStr);

        Assert.assertNull(session.getPassword());
    }

    @Test
    public void parse_publicCloudWithoutTenant_dbUserDetected() {
        String connStr = "mysql -hlocalhost -utestuser -P2883 -c";
        OdcConnectionParseResult session = MySQLClientArgsParser.parse(connStr);

        Assert.assertEquals("testuser", session.getDbUser());
    }

    private OdcConnectionParseResult buildSession() {
        OdcConnectionParseResult odcConnectionSession = new OdcConnectionParseResult();
        odcConnectionSession.setCluster("C1");
        odcConnectionSession.setTenant("sys");
        odcConnectionSession.setHost("127.0.0.1");
        odcConnectionSession.setPort(2883);
        odcConnectionSession.setDbUser("root");
        odcConnectionSession.setDefaultDBName("oceanbase");
        odcConnectionSession.setPassword("pwd");
        return odcConnectionSession;
    }

}
