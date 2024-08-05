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
package com.oceanbase.odc.service.onlineschemachange.rename;

import java.util.Collections;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.sql.execute.SyncJdbcExecutor;
import com.oceanbase.odc.service.onlineschemachange.model.OriginTableCleanStrategy;

/**
 * @author longpeng.zlp
 * @date 2024/8/2 10:42
 * @since 4.3.1
 */
public class LockTableInterceptorTest {
    private RenameTableParameters renameTableParameters;
    private LockTableInterceptor interceptor;
    private SyncJdbcExecutor operations;

    @Before
    public void init() {
        ConnectionSession session = Mockito.mock(ConnectionSession.class);
        Mockito.when(session.getDialectType()).thenReturn(DialectType.OB_MYSQL);
        operations = Mockito.mock(SyncJdbcExecutor.class);
        Mockito.when(session.getSyncJdbcExecutor(ArgumentMatchers.anyString())).thenReturn(operations);
        interceptor = new LockTableInterceptor(session);
        renameTableParameters = new RenameTableParameters("schema",
                "originTable", "oldTable", "ghostTable",
                5, OriginTableCleanStrategy.ORIGIN_TABLE_RENAME_AND_RESERVED, Collections.emptyList());
    }

    @Test
    public void testLockTable() {
        interceptor.preRename(renameTableParameters);
        ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(operations).execute(argumentCaptor.capture());
        Assert.assertEquals(argumentCaptor.getValue(), "lock table  `originTable` write");
    }

    @Test
    public void testUnLockTable() {
        interceptor.postRenamed(renameTableParameters);
        ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(operations).execute(argumentCaptor.capture());
        Assert.assertEquals(argumentCaptor.getValue(), "unlock tables");
    }
}
