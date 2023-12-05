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
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.TestConnectionUtil;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.service.db.model.OdcDBVariable;

public class DBVariableServiceTest extends ServiceTestEnv {

    @Autowired
    private DBVariablesService variablesService;

    @Test
    public void list_mysqlMode_listSucceed() {
        ConnectionSession session = TestConnectionUtil.getTestConnectionSession(ConnectType.OB_MYSQL);
        // sid:1-1:d:db:var:session
        List<OdcDBVariable> variables = this.variablesService.list(session, "session");
        Assert.assertFalse(variables.isEmpty());
        Assert.assertTrue(variables.get(0).getKey().equalsIgnoreCase("autocommit"));
    }

    @Test
    public void list_oracleMode_listSucceed() {
        ConnectionSession session = TestConnectionUtil.getTestConnectionSession(ConnectType.OB_ORACLE);
        // sid:1-1:d:db:var:session
        List<OdcDBVariable> variables = this.variablesService.list(session, "session");
        Assert.assertFalse(variables.isEmpty());
        Assert.assertTrue(variables.get(0).getKey().equalsIgnoreCase("autocommit"));
    }

    @Test
    public void update_setNlsFormat_setConnectSessionAndDbSucceed() {
        ConnectionSession session = TestConnectionUtil.getTestConnectionSession(ConnectType.OB_ORACLE);
        OdcDBVariable dbVirable = new OdcDBVariable();
        String target = "YYYY-MM-DD HH24:MI:SS";
        dbVirable.setName("nls_date_format");
        dbVirable.setValue(target);
        dbVirable.setVariableScope("session");
        this.variablesService.update(session, dbVirable);
        Optional<OdcDBVariable> optional = this.variablesService.list(session, "session").stream()
                .filter(i -> StringUtils.endsWithIgnoreCase(i.getKey(), "nls_date_format")).findFirst();
        Assert.assertTrue(optional.isPresent());
        Assert.assertEquals(target, ConnectionSessionUtil.getNlsDateFormat(session));
        Assert.assertEquals(target, dbVirable.getValue());
    }

}
