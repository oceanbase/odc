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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oceanbase.odc.common.json.JacksonFactory;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.service.connection.model.OBTenantMode;

public class OBTenantModeTest {
    private final ObjectMapper objectMapper = JacksonFactory.jsonMapper();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void getValue_MYSQL_lowercase() {
        String value = OBTenantMode.MYSQL.getValue();
        Assert.assertEquals("MySQL", value);
    }

    @Test
    public void fromValue_Valid() {
        OBTenantMode tenantMode = JsonUtils.fromJson("\"oracle\"", OBTenantMode.class);
        Assert.assertEquals(OBTenantMode.ORACLE, tenantMode);
    }

    @Test
    public void fromValue_Invalid() throws JsonProcessingException {
        thrown.expect(JsonProcessingException.class);
        thrown.expectMessage("TenantMode value not supported, given value 'something'");

        objectMapper.readValue("\"something\"", OBTenantMode.class);
    }
}
