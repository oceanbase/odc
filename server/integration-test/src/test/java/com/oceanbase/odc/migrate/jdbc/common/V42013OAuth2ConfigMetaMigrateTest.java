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

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.common.security.PasswordUtils;
import com.oceanbase.odc.service.integration.IntegrationService;
import com.oceanbase.odc.service.integration.model.Oauth2Parameter;
import com.oceanbase.odc.service.integration.model.SSOIntegrationConfig;

import lombok.SneakyThrows;

public class V42013OAuth2ConfigMetaMigrateTest extends ServiceTestEnv {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private IntegrationService integrationService;

    private JdbcTemplate jdbcTemplate;

    @Before
    public void init() {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        String addOrg = "insert into iam_organization("
                + "`id`,`unique_identifier`,`secret`,`name`,`creator_id`,`is_builtin`,`description`,`type`) "
                + "values(2,'a','%s','CompanyA',1,0,'D','TEAM')";
        String secret = PasswordUtils.random(32);
        jdbcTemplate.update(String.format(addOrg, secret));
    }

    @After
    public void clean() {
        jdbcTemplate.update("delete from iam_organization where type='TEAM'");
        jdbcTemplate.update("delete from integration_integration where 1=1");
    }

    @Test
    @SneakyThrows
    public void migrate_auth_type_not_oauth2() {
        V42013OAuth2ConfigMetaMigrate migrate = new V42013OAuth2ConfigMetaMigrate();
        updateAuthType("local");
        migrate.migrate(dataSource);
        Assert.assertEquals("local", selectValueByKey("odc.iam.auth.type"));
    }


    @Test
    @SneakyThrows
    public void migrate_auth_type_contain_oauth2() {
        V42013OAuth2ConfigMetaMigrate migrate = new V42013OAuth2ConfigMetaMigrate();
        addOauth2Config();
        migrate.migrate(dataSource);
        Assert.assertEquals("local", selectValueByKey("odc.iam.auth.type"));
        SSOIntegrationConfig sSoClientRegistration = integrationService.getSSoIntegrationConfig();
        Assert.assertNotNull(sSoClientRegistration);
        Assert.assertEquals("secret", ((Oauth2Parameter) sSoClientRegistration.getSsoParameter()).getSecret());
    }

    @Test
    @SneakyThrows
    public void migrate_auth_type_contain_buc() {
        V42013OAuth2ConfigMetaMigrate migrate = new V42013OAuth2ConfigMetaMigrate();
        addBucConfig();
        migrate.migrate(dataSource);
        Assert.assertEquals("local", selectValueByKey("odc.iam.auth.type"));
        SSOIntegrationConfig sSoClientRegistration = integrationService.getSSoIntegrationConfig();
        Assert.assertNotNull(sSoClientRegistration);
        Assert.assertEquals("secret", ((Oauth2Parameter) sSoClientRegistration.getSsoParameter()).getSecret());
    }

    private String selectValueByKey(String key) {
        String querySql = "select `value` from `config_system_configuration` where `key` = '" + key + "'";
        return jdbcTemplate.queryForObject(querySql, String.class);
    }

    private void updateAuthType(String authType) {
        String updateAuthType =
                "update `config_system_configuration` set `value` = '" + authType
                        + "' where `key` = 'odc.iam.auth.type'";
        jdbcTemplate.update(updateAuthType);
    }

    private void addBucConfig() {
        String sql = "UPDATE config_system_configuration SET `value`='buc' where `key`='odc.iam.auth.type';\n"
                + "UPDATE config_system_configuration SET `value`='https://127.0.0.1:80/cas/login' where `key`='odc.oauth2.buc.logoutUrl';\n"
                + "UPDATE config_system_configuration SET `value`='http://localhost:8989/oauth2/authorization/cas' where `key`='odc.oauth2.buc.loginRedirectUrl';\n"
                + "UPDATE config_system_configuration SET `value`='admin,admin1' where `key`='odc.oauth2.buc.adminAccountNames';\n"
                + "\n"
                + "UPDATE config_system_configuration SET `value`='userAccountNameField' where `key`='odc.oauth2.userAccountNameField';\n"
                + "UPDATE config_system_configuration SET `value`='userNickNameField' where `key`='odc.oauth2.userNickNameField';\n"
                + "UPDATE config_system_configuration SET `value`='organizationNameField' where `key`='odc.oauth2.organizationNameField';\n"
                + "\n"
                + "UPDATE config_system_configuration SET `value`='buc' where `key`='spring.security.oauth2.client.registration.odc.provider';\n"
                + "UPDATE config_system_configuration SET `value`='your client id' where `key`='spring.security.oauth2.client.registration.buc.client-id';\n"
                + "UPDATE config_system_configuration SET `value`='secret' where `key`='spring.security.oauth2.client.registration.buc.client-secret';\n"
                + "UPDATE config_system_configuration SET `value`='http://localhost:8989/login/oauth2/code/{registrationId}' where `key`='spring.security.oauth2.client.registration.buc.redirect-uri';\n"
                + "UPDATE config_system_configuration SET `value`='authorization_code' where `key`='spring.security.oauth2.client.registration.buc.authorization-grant-type';\n"
                + "UPDATE config_system_configuration SET `value`='profile' where `key`='spring.security.oauth2.client.registration.buc.scope';\n"
                + "UPDATE config_system_configuration SET `value`='post' where `key`='spring.security.oauth2.client.registration.buc.clientAuthenticationMethod';\n"
                + "\n"
                + "UPDATE config_system_configuration SET `value`='https://127.0.0.1:80/cas/oauth2.0/authorize' where `key`='spring.security.oauth2.client.provider.buc.authorization-uri';\n"
                + "UPDATE config_system_configuration SET `value`='https://127.0.0.1:80/cas/oauth2.0/accessToken' where `key`='spring.security.oauth2.client.provider.buc.token-uri';\n"
                + "UPDATE config_system_configuration SET `value`='https://127.0.0.1:80/cas/oauth2.0/profile' where `key`='spring.security.oauth2.client.provider.buc.user-info-uri';\n"
                + "UPDATE config_system_configuration SET `value`='header' where `key`='spring.security.oauth2.client.provider.buc.userInfoAuthenticationMethod';";
        jdbcTemplate.update(sql);
    }

    private void addOauth2Config() {
        String sql = "UPDATE config_system_configuration SET `value`='oauth2' where `key`='odc.iam.auth.type';\n"
                + "UPDATE config_system_configuration SET `value`='https://127.0.0.1:80/cas/login' where `key`='odc.oauth2.logoutUrl';\n"
                + "UPDATE config_system_configuration SET `value`='http://localhost:8989/oauth2/authorization/odc' where `key`='odc.oauth2.loginRedirectUrl';\n"
                + "UPDATE config_system_configuration SET `value`='admin,admin1' where `key`='odc.oauth2.adminAccountNames';\n"
                + "\n"
                + "UPDATE config_system_configuration SET `value`='emp_id' where `key`='odc.oauth2.buc.userAccountNameField';\n"
                + "UPDATE config_system_configuration SET `value`='name' where `key`='odc.oauth2.userNickNameField';\n"
                + "UPDATE config_system_configuration SET `value`='CompanyA' where `key`='odc.oauth2.organizationName';\n"
                + "\n"
                + "UPDATE config_system_configuration SET `value`='cas' where `key`='spring.security.oauth2.client.registration.odc.provider';\n"
                + "UPDATE config_system_configuration SET `value`='100001' where `key`='spring.security.oauth2.client.registration.odc.client-id';\n"
                + "UPDATE config_system_configuration SET `value`='secret' where `key`='spring.security.oauth2.client.registration.odc.client-secret';\n"
                + "UPDATE config_system_configuration SET `value`='http://localhost:8989/login/oauth2/code/{registrationId}' where `key`='spring.security.oauth2.client.registration.odc.redirect-uri';\n"
                + "UPDATE config_system_configuration SET `value`='authorization_code' where `key`='spring.security.oauth2.client.registration.odc.authorization-grant-type';\n"
                + "UPDATE config_system_configuration SET `value`='profile' where `key`='spring.security.oauth2.client.registration.odc.scope';\n"
                + "UPDATE config_system_configuration SET `value`='post' where `key`='spring.security.oauth2.client.registration.odc.clientAuthenticationMethod';\n"
                + "\n"
                + "UPDATE config_system_configuration SET `value`='https://127.0.0.1:80/cas/oauth2.0/authorize' where `key`='spring.security.oauth2.client.provider.cas.authorization-uri';\n"
                + "UPDATE config_system_configuration SET `value`='https://127.0.0.1:80/cas/oauth2.0/accessToken' where `key`='spring.security.oauth2.client.provider.cas.token-uri';\n"
                + "UPDATE config_system_configuration SET `value`='https://127.0.0.1:80/cas/oauth2.0/profile' where `key`='spring.security.oauth2.client.provider.cas.user-info-uri';\n"
                + "UPDATE config_system_configuration SET `value`='header' where `key`='spring.security.oauth2.client.provider.cas.userInfoAuthenticationMethod';";
        jdbcTemplate.update(sql);
    }
}
