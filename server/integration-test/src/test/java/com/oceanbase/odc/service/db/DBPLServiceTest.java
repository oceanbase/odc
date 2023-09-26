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

import static org.awaitility.Awaitility.await;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcOperations;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.TestConnectionUtil;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.service.db.model.BatchCompileResp;
import com.oceanbase.odc.service.db.model.BatchCompileStatus;
import com.oceanbase.odc.service.db.model.StartBatchCompileReq;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;
import com.oceanbase.tools.dbbrowser.model.DBPLObjectIdentity;

/**
 * @author wenniu.ly
 * @date 2022/6/14
 */
public class DBPLServiceTest extends ServiceTestEnv {

    @Autowired
    private DBPLService plService;
    private String batchCompileId;

    private static final String CREATE_FUNCTION =
            "CREATE OR REPLACE function fun_out(a out int) return integer as v1 int;\n"
                    + "begin\n"
                    + "a := 1;\n"
                    + "v1 := a;\n"
                    + "return v1;\n"
                    + "end;";
    private static final String CREATE_PROCEDURE = "create or replace PROCEDURE SEASON\n"
            + "        (\n"
            + "           month in int\n"
            + "        ) IS \n"
            + "        BEGIN\n"
            + "            if month >=1 and month <= 3 then\n"
            + "                dbms_output.put_line('春季');\n"
            + "            elsif month >= 4 and month <=6 THEN\n"
            + "                dbms_output.put_line('夏季');\n"
            + "            elsif month >= 7 and month <= 9 THEN\n"
            + "                dbms_output.put_line('秋季');\n"
            + "            elsif month >= 10 and month <= 12 THEN\n"
            + "                dbms_output.put_line('冬季');\n"
            + "            end if;\n"
            + "        END;";
    private static final String CREATE_PACKAGE_HEAD =
            "create or replace PACKAGE PKG1 AS FUNCTION fun_example (p1 IN NUMBER) RETURN NUMBER; PROCEDURE proc_example(p1 IN NUMBER); END PKG1";
    private static final String CREATE_PACKAGE_BODY =
            "create or replace PACKAGE BODY PKG1 AS FUNCTION fun_example (p1 IN NUMBER) RETURN NUMBER AS BEGIN return p1; END; PROCEDURE proc_example(p1 IN NUMBER) AS BEGIN dbms_output.put_line(p1); END; END PKG1";

    @BeforeClass
    public static void setUp() {
        ConnectionSession session = TestConnectionUtil.getTestConnectionSession(ConnectType.OB_ORACLE);
        JdbcOperations jdbcOperations = session.getSyncJdbcExecutor(ConnectionSessionConstants.CONSOLE_DS_KEY);
        // drop exist resources
        try {
            jdbcOperations.execute("drop function fun_out");
            jdbcOperations.execute("drop procedure season");
            jdbcOperations.execute("drop package body PKG1");
            jdbcOperations.execute("drop package PKG1");
        } catch (Exception e) {
            // Eat exception
        }
        // insert several pl
        jdbcOperations.execute(CREATE_FUNCTION);
        jdbcOperations.execute(CREATE_PROCEDURE);
        jdbcOperations.execute(CREATE_PACKAGE_HEAD);
        jdbcOperations.execute(CREATE_PACKAGE_BODY);
    }

    @After
    public void clear() {
        // end batch compile
        if (StringUtils.isNotBlank(batchCompileId)) {
            plService.endBatchCompile(batchCompileId);
        }
    }

    @AfterClass
    public static void tearDown() {
        // drop pls
        ConnectionSession session = TestConnectionUtil.getTestConnectionSession(ConnectType.OB_ORACLE);
        JdbcOperations jdbcOperations = session.getSyncJdbcExecutor(ConnectionSessionConstants.CONSOLE_DS_KEY);
        jdbcOperations.execute("drop function fun_out");
        jdbcOperations.execute("drop procedure season");
        jdbcOperations.execute("drop package body PKG1");
        jdbcOperations.execute("drop package PKG1");
    }

    @Test
    public void test_start_batch_compile() {
        batchCompileId = startBatchCompile();
        Assert.assertFalse(StringUtils.isEmpty(batchCompileId));
    }

    @Test
    public void test_start_batch_compile_with_scope() {
        batchCompileId = startBatchCompileWithScope();
        Assert.assertFalse(StringUtils.isEmpty(batchCompileId));
    }

    @Test
    public void test_start_batch_compile_with_scope_and_type() throws Exception {
        batchCompileId = startBatchCompileWithScopeAndType();
        Assert.assertFalse(StringUtils.isEmpty(batchCompileId));
    }

    @Test
    public void test_end_batch_compile() {
        batchCompileId = startBatchCompile();
        boolean success = plService.endBatchCompile(batchCompileId);
        Assert.assertTrue(success);
        batchCompileId = null;
    }

    @Test
    public void test_get_batch_compile_result() {
        batchCompileId = startBatchCompile();
        await().atMost(10L, TimeUnit.SECONDS).until(() -> {
            BatchCompileResp resp = plService.getBatchCompileResult(batchCompileId);
            return BatchCompileStatus.COMPLETED == resp.getStatus();
        });
        BatchCompileResp resp = plService.getBatchCompileResult(batchCompileId);
        Assert.assertEquals(3, resp.getTotalCount().intValue());
        Assert.assertEquals(3, resp.getCompletedCount().intValue());
        Assert.assertEquals(3, resp.getResults().size());
    }

    @Test
    public void test_get_batch_compile_result_with_package_and_body() {
        batchCompileId = startBatchCompileWithScope();
        await().atMost(3L, TimeUnit.SECONDS).until(() -> {
            BatchCompileResp resp = plService.getBatchCompileResult(batchCompileId);
            return BatchCompileStatus.COMPLETED == resp.getStatus();
        });
        BatchCompileResp resp = plService.getBatchCompileResult(batchCompileId);
        Assert.assertEquals(3, resp.getTotalCount().intValue());
        Assert.assertEquals(3, resp.getCompletedCount().intValue());
        Assert.assertEquals(3, resp.getResults().size());
    }

    private String startBatchCompile() {
        List<DBPLObjectIdentity> identities = new ArrayList<>();
        DBPLObjectIdentity i1 = new DBPLObjectIdentity();
        i1.setType(DBObjectType.FUNCTION);
        i1.setName("fun_out");
        identities.add(i1);
        DBPLObjectIdentity i2 = new DBPLObjectIdentity();
        i2.setType(DBObjectType.PROCEDURE);
        i2.setName("season");
        identities.add(i2);
        DBPLObjectIdentity i3 = new DBPLObjectIdentity();
        i3.setType(DBObjectType.PACKAGE);
        i3.setName("PKG1");
        identities.add(i3);
        ConnectionSession session = TestConnectionUtil.getTestConnectionSession(ConnectType.OB_ORACLE);
        String schema = ConnectionSessionUtil.getCurrentSchema(session);
        identities.forEach(i -> i.setSchemaName(schema));
        return plService.startBatchCompile(session, null, identities);
    }

    private String startBatchCompileWithScope() {
        StartBatchCompileReq req = new StartBatchCompileReq();
        req.setScope("ALL");
        ConnectionSession session = TestConnectionUtil.getTestConnectionSession(ConnectType.OB_ORACLE);
        return plService.startBatchCompile(session, null, req);
    }

    private String startBatchCompileWithScopeAndType() {
        StartBatchCompileReq req = new StartBatchCompileReq();
        req.setScope("ALL");
        req.setObjectType(DBObjectType.FUNCTION);
        ConnectionSession session = TestConnectionUtil.getTestConnectionSession(ConnectType.OB_ORACLE);
        return plService.startBatchCompile(session, null, req);
    }

}
