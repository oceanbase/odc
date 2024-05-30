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
package com.oceanbase.odc.service.session;

import java.util.HashMap;

import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.core.sql.execute.model.SqlExecuteStatus;
import com.oceanbase.odc.core.sql.execute.model.SqlTuple;
import com.oceanbase.odc.service.session.interceptor.NlsFormatInterceptor;
import com.oceanbase.odc.service.session.model.AsyncExecuteContext;
import com.oceanbase.odc.service.session.model.SqlExecuteResult;

/**
 * Test cases for {@link NlsFormatInterceptor}
 *
 * @author yh263208
 * @date 2023-07-04 15:48
 * @since ODC_release_4.2.0
 */
public class NlsFormatInterceptorTest {

    @Test
    public void afterCompletion_mysql_notingSet() throws Exception {
        ConnectionSession session = getConnectionSession(ConnectType.OB_MYSQL);
        NlsFormatInterceptor interceptor = new NlsFormatInterceptor();
        SqlExecuteResult r = getResponse("set session nls_date_format='DD-MON-RR'", SqlExecuteStatus.SUCCESS);
        interceptor.afterCompletion(r, session, getContext());
        Assert.assertNull(ConnectionSessionUtil.getNlsDateFormat(session));
    }

    @Test
    public void afterCompletion_failedSqlResult_notingSet() throws Exception {
        ConnectionSession session = getConnectionSession(ConnectType.OB_ORACLE);
        NlsFormatInterceptor interceptor = new NlsFormatInterceptor();
        SqlExecuteResult r = getResponse("set session nls_date_format='DD-MON-RR'", SqlExecuteStatus.FAILED);
        interceptor.afterCompletion(r, session, getContext());
        Assert.assertNull(ConnectionSessionUtil.getNlsDateFormat(session));
    }

    @Test
    public void afterCompletion_multiSqls_notingSet() throws Exception {
        ConnectionSession session = getConnectionSession(ConnectType.OB_ORACLE);
        NlsFormatInterceptor interceptor = new NlsFormatInterceptor();
        SqlExecuteResult r = getResponse("create or replace procedure abcd(p in varchar) as\n"
                + "name varchar2(64) := 'aaaa';\n"
                + "begin\n"
                + "dbms_output.put_line('aaaa');\n"
                + "end;", SqlExecuteStatus.SUCCESS);
        interceptor.afterCompletion(r, session, getContext());
        Assert.assertNull(ConnectionSessionUtil.getNlsDateFormat(session));
    }

    @Test
    public void afterCompletion_noSetVarExists_notingSet() throws Exception {
        ConnectionSession session = getConnectionSession(ConnectType.OB_ORACLE);
        NlsFormatInterceptor interceptor = new NlsFormatInterceptor();
        SqlExecuteResult r = getResponse("-- comment\nselect 123 from dual;", SqlExecuteStatus.SUCCESS);
        interceptor.afterCompletion(r, session, getContext());
        Assert.assertNull(ConnectionSessionUtil.getNlsDateFormat(session));
    }

    @Test
    public void afterCompletion_commentWithSetVar_setSucceed() throws Exception {
        ConnectionSession session = getConnectionSession(ConnectType.OB_ORACLE);
        NlsFormatInterceptor interceptor = new NlsFormatInterceptor();
        String expect = "DD-MON-RR";
        SqlExecuteResult r = getResponse("-- comment\nset session nls_date_format='" + expect + "';",
                SqlExecuteStatus.SUCCESS);
        interceptor.afterCompletion(r, session, getContext());
        Assert.assertEquals(expect, ConnectionSessionUtil.getNlsDateFormat(session));
    }

    @Test
    public void afterCompletion_multiCommentsWithSetVar_setSucceed() throws Exception {
        ConnectionSession session = getConnectionSession(ConnectType.OB_ORACLE);
        NlsFormatInterceptor interceptor = new NlsFormatInterceptor();
        String expect = "DD-MON-RR";
        SqlExecuteResult r = getResponse("/*asdasdasd*/    set session nls_date_format='" + expect + "';",
                SqlExecuteStatus.SUCCESS);
        interceptor.afterCompletion(r, session, getContext());
        Assert.assertEquals(expect, ConnectionSessionUtil.getNlsDateFormat(session));
    }

    @Test
    public void afterCompletion_nlsTimestampFormat_setSucceed() throws Exception {
        ConnectionSession session = getConnectionSession(ConnectType.OB_ORACLE);
        NlsFormatInterceptor interceptor = new NlsFormatInterceptor();
        String expect = "DD-MON-RR";
        SqlExecuteResult r = getResponse("/*asdasdasd*/    set session nls_timestamp_format='" + expect + "';",
                SqlExecuteStatus.SUCCESS);
        interceptor.afterCompletion(r, session, getContext());
        Assert.assertEquals(expect, ConnectionSessionUtil.getNlsTimestampFormat(session));
    }

    @Test
    public void afterCompletion_nlsTimestampTZFormat_setSucceed() throws Exception {
        ConnectionSession session = getConnectionSession(ConnectType.OB_ORACLE);
        NlsFormatInterceptor interceptor = new NlsFormatInterceptor();
        String expect = "DD-MON-RR";
        SqlExecuteResult r = getResponse("/*asdasdasd*/    set session nls_timestamp_tz_format='" + expect + "';",
                SqlExecuteStatus.SUCCESS);
        interceptor.afterCompletion(r, session, getContext());
        Assert.assertEquals(expect, ConnectionSessionUtil.getNlsTimestampTZFormat(session));
    }

    @Test
    public void afterCompletion_setGlobal_nothingSet() throws Exception {
        ConnectionSession session = getConnectionSession(ConnectType.OB_ORACLE);
        NlsFormatInterceptor interceptor = new NlsFormatInterceptor();
        String expect = "DD-MON-RR";
        SqlExecuteResult r = getResponse("/*asdasdasd*/    set global nls_timestamp_tz_format='" + expect + "';",
                SqlExecuteStatus.SUCCESS);
        interceptor.afterCompletion(r, session, getContext());
        Assert.assertNull(ConnectionSessionUtil.getNlsTimestampTZFormat(session));
    }

    @Test
    public void afterCompletion_alterSession_setSucceed() throws Exception {
        ConnectionSession session = getConnectionSession(ConnectType.OB_ORACLE);
        NlsFormatInterceptor interceptor = new NlsFormatInterceptor();
        String expect = "DD-MON-RR";
        SqlExecuteResult r = getResponse("/*asdsd*/  alter   session \n\t\r set \"nls_date_format\"='" + expect + "';",
                SqlExecuteStatus.SUCCESS);
        interceptor.afterCompletion(r, session, getContext());
        Assert.assertEquals(expect, ConnectionSessionUtil.getNlsDateFormat(session));
    }

    private SqlExecuteResult getResponse(String sql, SqlExecuteStatus status) {
        SqlExecuteResult result = new SqlExecuteResult();
        result.setSqlTuple(SqlTuple.newTuple(sql));
        result.setStatus(status);
        return result;
    }

    private ConnectionSession getConnectionSession(ConnectType type) {
        ConnectionSession session = new TestConnectionSession("id", null, type);
        session.removeAttribute(ConnectionSessionConstants.NLS_TIMESTAMP_TZ_FORMAT_NAME);
        session.removeAttribute(ConnectionSessionConstants.NLS_DATE_FORMAT_NAME);
        session.removeAttribute(ConnectionSessionConstants.NLS_TIMESTAMP_FORMAT_NAME);
        return session;
    }

    private AsyncExecuteContext getContext() {
        return new AsyncExecuteContext(null, new HashMap<>());
    }

}
