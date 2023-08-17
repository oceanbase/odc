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
package com.oceanbase.odc.core.session;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.odc.core.shared.constant.DialectType;

public class ConnectionSessionUtilTest {

    @Test
    public void getUserOrSchemaString_Oracle() {
        String schema = "\"quota_schema\"";
        Assert.assertEquals(ConnectionSessionUtil.getUserOrSchemaString(schema, DialectType.OB_ORACLE),
                StringUtils.unwrap(schema, "\""));
        schema = "quota_schema";
        Assert.assertEquals(ConnectionSessionUtil.getUserOrSchemaString(schema, DialectType.OB_ORACLE),
                schema.toUpperCase());
        schema = null;
        Assert.assertNull(ConnectionSessionUtil.getUserOrSchemaString(schema, DialectType.OB_ORACLE));
    }

    @Test
    public void getUserOrSchemaString_Mysql() {
        String schema = "\"quota_schema\"";
        Assert.assertEquals(ConnectionSessionUtil.getUserOrSchemaString(schema, DialectType.OB_MYSQL), schema);
        schema = "quota_schema";
        Assert.assertEquals(ConnectionSessionUtil.getUserOrSchemaString(schema, DialectType.OB_MYSQL), schema);
    }
}

