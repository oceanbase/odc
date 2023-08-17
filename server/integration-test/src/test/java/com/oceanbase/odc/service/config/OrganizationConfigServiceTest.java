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
package com.oceanbase.odc.service.config;

import java.util.ArrayList;
import java.util.List;

import javax.validation.ConstraintViolationException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.MockedAuthorityTestEnv;
import com.oceanbase.odc.core.authority.DefaultLoginSecurityManager;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.service.config.model.Configuration;

public class OrganizationConfigServiceTest extends MockedAuthorityTestEnv {
    private final Long userId = 1L;

    private final Long organizationId = 1L;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Autowired
    private OrganizationConfigService service;

    @Before
    public void setUp() {
        grantAllPermissions(ResourceType.ODC_SYSTEM_CONFIG);
    }

    @After
    public void clear() {
        DefaultLoginSecurityManager.removeContext();
        DefaultLoginSecurityManager.removeSecurityContext();
    }

    @Test
    public void testQueryNullConfig() {
        String value = service.query(organizationId, "delimiter");
        Assert.assertNull(value);
    }

    @Test
    public void testUpdateOrganizationConfig() {
        Configuration configuration = new Configuration("defaultDelimiter", ";");
        Configuration result = service.insert(configuration, organizationId, userId);
        Assert.assertEquals(configuration, result);

        configuration.setValue("//");
        result = service.update(configuration, organizationId, userId);
        Assert.assertEquals(configuration, result);

        String value = service.query(organizationId, "defaultDelimiter");
        Assert.assertEquals(value, configuration.getValue());
    }

    @Test
    public void testInsertOrganizationConfig() {
        Configuration configuration = new Configuration("delimiter", ";");
        Configuration result = service.insert(configuration, organizationId, userId);
        Assert.assertEquals(configuration, result);

        String value = service.query(organizationId, "delimiter");
        Assert.assertEquals(value, configuration.getValue());
    }

    @Test
    public void testBatchUpdateNullConfig() {
        thrown.expect(ConstraintViolationException.class);
        List<Configuration> configurations = service.batchUpdate(null, organizationId, userId);
    }


    @Test
    public void testBatchUpdateDefaultOrganizationConfig() {
        List<Configuration> configurationsReq = new ArrayList<>(4);
        configurationsReq.add(new Configuration("sqlexecute.defaultDelimiter", ";"));
        configurationsReq.add(new Configuration("sqlexecute.mysqlAutoCommitMode", "ON"));
        configurationsReq.add(new Configuration("sqlexecute.oracleAutoCommitMode", "ON"));
        configurationsReq.add(new Configuration("sqlexecute.defaultQueryLimit", "1000"));
        configurationsReq.add(new Configuration("sqlexecute.defaultObjectDraggingOption", "object_name"));
        configurationsReq.add(new Configuration("connect.sessionMode", "SingleSession"));
        configurationsReq.add(new Configuration("sqlexecute.sqlCheckMode", "AUTO"));
        List<Configuration> configurationsRep = service.batchUpdate(configurationsReq, organizationId, userId);
        Assert.assertEquals(configurationsRep, configurationsReq);

        String value = service.query(organizationId, "sqlexecute.defaultDelimiter");
        Assert.assertEquals(value, ";");
    }

    @Test
    public void testBatchUpdateNotDefaultOrganizationConfig() {
        List<Configuration> configurationsReq = new ArrayList<>(4);
        configurationsReq.add(new Configuration("sqlexecute.defaultDelimiter", "$$"));
        configurationsReq.add(new Configuration("sqlexecute.mysqlAutoCommitMode", "ON"));
        configurationsReq.add(new Configuration("sqlexecute.oracleAutoCommitMode", "ON"));
        configurationsReq.add(new Configuration("sqlexecute.defaultQueryLimit", "1000"));
        configurationsReq.add(new Configuration("sqlexecute.defaultObjectDraggingOption", "object_name"));
        configurationsReq.add(new Configuration("connect.sessionMode", "SingleSession"));
        configurationsReq.add(new Configuration("sqlexecute.sqlCheckMode", "AUTO"));
        List<Configuration> configurationsRep = service.batchUpdate(configurationsReq, organizationId, userId);
        Assert.assertEquals(configurationsRep, configurationsReq);

        String value = service.query(organizationId, "sqlexecute.defaultDelimiter");
        Assert.assertEquals(value, "$$");
    }
}
