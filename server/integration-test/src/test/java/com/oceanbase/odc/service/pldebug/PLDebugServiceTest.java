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
package com.oceanbase.odc.service.pldebug;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.TestConnectionUtil;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.core.sql.execute.SyncJdbcExecutor;
import com.oceanbase.odc.service.pldebug.model.PLDebugBreakpoint;
import com.oceanbase.odc.service.pldebug.model.PLDebugContextResp;
import com.oceanbase.odc.service.pldebug.model.StartPLDebugReq;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;
import com.oceanbase.tools.dbbrowser.model.DBPLParam;
import com.oceanbase.tools.dbbrowser.model.DBPLParamMode;
import com.oceanbase.tools.dbbrowser.model.DBProcedure;

/**
 * @author wenniu.ly
 * @date 2021/11/17
 */

@Ignore("manual verify only, may cost plenty of time and cause observer working thread exhausted")
public class PLDebugServiceTest extends ServiceTestEnv {
    @Autowired
    private PLDebugService plDebugService;

    private ConnectionSession connectionSession;

    private String debugId;

    private SyncJdbcExecutor asyncJdbcExecutor;

    @Before
    public void setUp() throws Exception {
        connectionSession = TestConnectionUtil.getTestConnectionSession(ConnectType.OB_ORACLE);

        String sql = "create or replace PROCEDURE proc(A INT, B INT) IS\n"
                + "  z INT := 0;\n"
                + "BEGIN\n"
                + "  Z := A+B;\n"
                + "END";
        asyncJdbcExecutor = connectionSession.getSyncJdbcExecutor(ConnectionSessionConstants.CONSOLE_DS_KEY);
        asyncJdbcExecutor.execute(sql);
    }

    @After
    public void tearDown() throws Exception {
        if (StringUtils.isNotEmpty(debugId)) {
            plDebugService.end(debugId);
        }
        asyncJdbcExecutor.execute("drop procedure proc");
    }

    @Test
    public void test_start_debug() throws Exception {
        debugId = startDebug();
        Assert.assertFalse(StringUtils.isEmpty(debugId));
    }

    @Test
    public void test_get_context() throws Exception {
        debugId = startDebug();

        PLDebugContextResp resp = plDebugService.getContext(debugId);
        Assert.assertFalse(resp.isTerminated());
        Assert.assertEquals(3, resp.getVariables().size());
    }

    @Test
    public void test_step_over() throws Exception {
        debugId = startDebug();

        Boolean result = plDebugService.stepOver(debugId);
        Assert.assertEquals(true, result);
        PLDebugContextResp resp = plDebugService.getContext(debugId);
        Assert.assertFalse(resp.isTerminated());
        // Old value is expected is 1, but now is 2
        Assert.assertEquals(2, resp.getBacktrace().getLineNum().intValue());
    }

    @Test
    public void test_set_breakpoint() throws Exception {
        debugId = startDebug();
        List<PLDebugBreakpoint> breakpoints = new ArrayList<>();
        breakpoints.add(PLDebugBreakpoint.of("PROC", DBObjectType.PROCEDURE, 4));
        breakpoints = plDebugService.setBreakpoints(debugId, breakpoints);
        Assert.assertEquals(1, breakpoints.size());
        Assert.assertEquals(4, breakpoints.get(0).getLineNum().intValue());
        Assert.assertTrue(breakpoints.get(0).getBreakpointNum() > 0);
    }

    private String startDebug() throws Exception {
        StartPLDebugReq req = new StartPLDebugReq();
        req.setDebugType(DBObjectType.PROCEDURE);

        List<DBPLParam> params = new ArrayList<>();
        DBPLParam paramA = DBPLParam.of("A", DBPLParamMode.IN, "INT");
        paramA.setDefaultValue("2");
        params.add(paramA);
        DBPLParam paramB = DBPLParam.of("B", DBPLParamMode.IN, "INT");
        paramB.setDefaultValue("3");
        params.add(paramB);
        DBProcedure odcProcedure = DBProcedure.of(null, "PROC", params);
        req.setProcedure(odcProcedure);

        String debugId = plDebugService.start(connectionSession, req);
        return debugId;
    }
}
