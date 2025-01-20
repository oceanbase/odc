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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.TestConnectionUtil;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.tools.dbbrowser.model.DBTable;

/**
 * @Author：tinker
 * @Date: 2023/3/6 12:02
 * @Descripition:
 */
public class DBTableServiceTest extends ServiceTestEnv {

    @Autowired
    private DBTableService dbTableService;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void getTable_not_exist_OB_MYSQL() {

        expectedException.expect(NotFoundException.class);

        ConnectionSession session = TestConnectionUtil.getTestConnectionSession(ConnectType.OB_MYSQL);

        DBTable table = dbTableService.getTable(session, "abc", "abc");
    }

    @Test
    public void getTable_not_exist_OB_ORACLE() {

        expectedException.expect(NotFoundException.class);

        ConnectionSession session = TestConnectionUtil.getTestConnectionSession(ConnectType.OB_ORACLE);

        DBTable table = dbTableService.getTable(session, "abc", "abc");
    }
}
