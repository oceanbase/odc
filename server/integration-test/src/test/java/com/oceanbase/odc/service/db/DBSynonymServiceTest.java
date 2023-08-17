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
package com.oceanbase.odc.service.db;

import java.util.List;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcOperations;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.TestConnectionUtil;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.tools.dbbrowser.model.DBSynonym;
import com.oceanbase.tools.dbbrowser.model.DBSynonymType;

/**
 * ODC同义词Service对象测试类
 *
 * @author yh263208
 * @date 2020-12-19 20:51
 * @since ODC_release_2.4.0
 */
public class DBSynonymServiceTest extends ServiceTestEnv {

    @Autowired
    private DBSynonymService synonymService;
    private final static String TABLE_NAME = "EMP";
    private final static String COMMON_SYNONYM_NAME = "EMP_COMMON_SYNONYM";
    private final static String PUBLIC_SYNONYM_NAME = "EMP_PUBLIC_SYNONYM";

    @BeforeClass
    public static void setUp() {
        ConnectionSession session = TestConnectionUtil.getTestConnectionSession(ConnectType.OB_ORACLE);
        String createTableDdl = String.format("create table %s (col varchar(20))", TABLE_NAME);
        session.getSyncJdbcExecutor(ConnectionSessionConstants.CONSOLE_DS_KEY).execute(createTableDdl);
        String createCommonSynonymDdl =
                String.format("create or replace synonym %s for %s", COMMON_SYNONYM_NAME, TABLE_NAME);
        JdbcOperations jdbcOperations = session.getSyncJdbcExecutor(ConnectionSessionConstants.CONSOLE_DS_KEY);
        jdbcOperations.execute(createCommonSynonymDdl);
        String createPublicSynonymDdl =
                String.format("create or replace public synonym %s for %s", PUBLIC_SYNONYM_NAME, TABLE_NAME);
        jdbcOperations.execute(createPublicSynonymDdl);
    }

    @AfterClass
    public static void clear() {
        ConnectionSession connectionSession = TestConnectionUtil.getTestConnectionSession(ConnectType.OB_ORACLE);
        JdbcOperations jdbcOperations =
                connectionSession.getSyncJdbcExecutor(ConnectionSessionConstants.CONSOLE_DS_KEY);
        jdbcOperations.execute("drop synonym " + COMMON_SYNONYM_NAME);
        jdbcOperations.execute("drop public synonym " + PUBLIC_SYNONYM_NAME);
        jdbcOperations.execute("drop table " + TABLE_NAME);
    }

    @Test
    public void testPublicSynonymListForOracle() {
        ConnectionSession session = TestConnectionUtil.getTestConnectionSession(ConnectType.OB_ORACLE);
        String schema = ConnectionSessionUtil.getCurrentSchema(session);
        List<DBSynonym> list = synonymService.list(session, schema, DBSynonymType.PUBLIC);
        Assert.assertNotEquals(0, list.size());
        boolean flag = false;
        for (DBSynonym synonym : list) {
            if (PUBLIC_SYNONYM_NAME.equals(synonym.getSynonymName())) {
                flag = true;
            }
        }
        Assert.assertTrue(flag);
    }

    @Test
    public void testCommonSynonymListForOracle() {
        ConnectionSession session = TestConnectionUtil.getTestConnectionSession(ConnectType.OB_ORACLE);
        String schema = ConnectionSessionUtil.getCurrentSchema(session);
        List<DBSynonym> list = synonymService.list(session, schema, DBSynonymType.COMMON);
        Assert.assertNotEquals(0, list.size());
        boolean flag = false;
        for (DBSynonym synonym : list) {
            if (COMMON_SYNONYM_NAME.equals(synonym.getSynonymName())) {
                flag = true;
            }
        }
        Assert.assertTrue(flag);
    }

    @Test
    public void testDetailCommonSynonymServiceForOracle() {
        ConnectionSession session = TestConnectionUtil.getTestConnectionSession(ConnectType.OB_ORACLE);
        DBSynonym synonym = new DBSynonym();
        synonym.setSynonymType(DBSynonymType.COMMON);
        synonym.setSynonymName(COMMON_SYNONYM_NAME);
        synonym.setOwner(ConnectionSessionUtil.getCurrentSchema(session));
        DBSynonym result = synonymService.detail(session, synonym);
        Assert.assertEquals(COMMON_SYNONYM_NAME, result.getSynonymName());
        Assert.assertEquals(DBSynonymType.COMMON, result.getSynonymType());
    }

    @Test
    public void testDetailPublicSynonymServiceForOracle() {
        ConnectionSession session = TestConnectionUtil.getTestConnectionSession(ConnectType.OB_ORACLE);
        DBSynonym synonym = new DBSynonym();
        synonym.setSynonymType(DBSynonymType.PUBLIC);
        synonym.setSynonymName(PUBLIC_SYNONYM_NAME);
        synonym.setOwner(ConnectionSessionUtil.getCurrentSchema(session));
        DBSynonym result = synonymService.detail(session, synonym);
        Assert.assertEquals(PUBLIC_SYNONYM_NAME, result.getSynonymName());
        Assert.assertEquals(DBSynonymType.PUBLIC, result.getSynonymType());
    }

}
