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

import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcOperations;

/**
 * @author longpeng.zlp
 * @date 2024/8/2 10:22
 * @since 4.3.1
 */
public class OBOracleRenameTableHandlerTest {
    @Test
    public void testOBOracleRenameTableHandler() {
        JdbcOperations jdbcOperations = Mockito.mock(JdbcOperations.class);
        OBOracleRenameTableHandler handler = new OBOracleRenameTableHandler(jdbcOperations);
        handler.rename("oracleSchema", "originTable", "targetTable");
        ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(jdbcOperations).execute(argumentCaptor.capture());
        Assert.assertEquals(argumentCaptor.getValue(), "RENAME \"originTable\" TO \"targetTable\"");
    }
}
