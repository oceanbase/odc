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
package com.oceanbase.odc.migrate.jdbc.common;

import java.util.HashSet;
import java.util.List;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.metadb.integration.IntegrationEntity;
import com.oceanbase.odc.metadb.integration.IntegrationRepository;
import com.oceanbase.odc.service.integration.model.Encryption.EncryptionAlgorithm;
import com.oceanbase.odc.service.integration.model.IntegrationType;
import com.oceanbase.odc.service.integration.model.Oauth2Parameter;
import com.oceanbase.odc.service.integration.model.SSOIntegrationConfig;

public class V4324Oauth2UseStateParamDefaultFalseTest extends ServiceTestEnv {

    @Autowired
    private DataSource dataSource;
    @Autowired
    private IntegrationRepository integrationRepository;

    @Before
    public void setUp() {
        integrationRepository.deleteAll();
        initData();
    }

    @After
    public void clear() {
        integrationRepository.deleteAll();
    }

    @Test
    public void test_set_use_state_param_default_false() {
        V4324Oauth2UseStateParamDefaultFalse migrate = new V4324Oauth2UseStateParamDefaultFalse();
        migrate.migrate(dataSource);
        List<IntegrationEntity> all = integrationRepository.findAll();
        Assert.assertEquals(1, all.size());
        IntegrationEntity integrationEntity = all.get(0);
        SSOIntegrationConfig config = JsonUtils.fromJson(integrationEntity.getConfiguration(),
                SSOIntegrationConfig.class);
        Assert.assertTrue(config.isOauth2OrOidc());
        Oauth2Parameter ssoParameter = (Oauth2Parameter) config.getSsoParameter();
        Assert.assertFalse(ssoParameter.getUseStateParams());
    }

    private void initData() {
        IntegrationEntity entity = new IntegrationEntity();
        entity.setName("test");
        entity.setEnabled(true);
        entity.setCreatorId(0L);
        entity.setType(IntegrationType.SSO);
        entity.setOrganizationId(0L);
        entity.setBuiltin(false);
        entity.setEncrypted(false);
        entity.setAlgorithm(EncryptionAlgorithm.RAW);

        Oauth2Parameter oauth2Parameter = new Oauth2Parameter();
        oauth2Parameter.setUseStateParams(true);
        oauth2Parameter.setClientId("1");
        oauth2Parameter.setScope(new HashSet<>());

        SSOIntegrationConfig config = new SSOIntegrationConfig();
        config.setSsoParameter(oauth2Parameter);
        config.setType("OAUTH2");
        entity.setConfiguration(JsonUtils.toJson(config));
        integrationRepository.save(entity);
    }
}
