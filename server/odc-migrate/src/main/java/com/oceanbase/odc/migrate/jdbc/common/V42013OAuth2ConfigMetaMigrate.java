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

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.sql.DataSource;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import com.google.common.base.Preconditions;
import com.oceanbase.odc.common.crypto.CryptoUtils;
import com.oceanbase.odc.common.crypto.Encryptors;
import com.oceanbase.odc.common.crypto.TextEncryptor;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.util.HashUtils;
import com.oceanbase.odc.common.util.JdbcOperationsUtil;
import com.oceanbase.odc.common.util.ObjectUtil;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.migrate.JdbcMigratable;
import com.oceanbase.odc.core.migrate.Migratable;
import com.oceanbase.odc.metadb.iam.OrganizationEntity;
import com.oceanbase.odc.service.integration.model.IntegrationType;
import com.oceanbase.odc.service.integration.model.Oauth2Parameter;
import com.oceanbase.odc.service.integration.model.SSOIntegrationConfig;
import com.oceanbase.odc.service.integration.model.SSOIntegrationConfig.MappingRule;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Migratable(version = "4.2.0.13", description = "migrate oauth2 properties to integration_integration")
public class V42013OAuth2ConfigMetaMigrate implements JdbcMigratable {

    private final String SECURITY_REGISTRATION_PREFIX = "spring.security.oauth2.client.registration.";
    private final String SECURITY_REGISTRATION_PREFIX_ODC = SECURITY_REGISTRATION_PREFIX + "odc";
    private final String SECURITY_REGISTRATION_PREFIX_BUC = SECURITY_REGISTRATION_PREFIX + "buc";
    private final String SECURITY_PROVIDER_PREFIX = "spring.security.oauth2.client.provider.";
    private final String REGISTRATION_PROVIDER_SUFFIX = "provider";
    private final String REGISTRATION_CLIENT_ID_SUFFIX = "clientId";
    private final String REGISTRATION_CLIENT_SECRET_SUFFIX = "clientSecret";
    private final String REGISTRATION_REDIRECT_URL_SUFFIX = "redirectUri";
    private final String REGISTRATION_AUTHORIZATION_GRANT_TYPE_SUFFIX = "authorizationGrantType";
    private final String REGISTRATION_SCOPE_SUFFIX = "scope";
    private final String REGISTRATION_CLIENT_AUTHENTICATION_METHOD_SUFFIX = "clientAuthenticationMethod";
    private final String PROVIDER_AUTHORIZATION_URI_SUFFIX = "authorizationUri";
    private final String PROVIDER_TOKEN_URI_SUFFIX = "tokenUri";
    private final String PROVIDER_USER_INFO_URI_SUFFIX = "userInfoUri";
    private final String PROVIDER_USER_INFO_AUTHENTICATION_METHOD_SUFFIX = "userInfoAuthenticationMethod";
    private final String OAUTH_PROPERTY_PREFIX = "odc.oauth2.";
    private final String OAUTH_PROPERTY_BUC_PREFIX = "odc.oauth2.buc";
    private final String OAUTH2_USER_ACCOUNT_NAME_FIELD = "userAccountNameField";
    private final String OAUTH2_USER_NICK_NAME_FILED = "userNickNameField";
    private final String OAUTH2_ORGANIZATION_NAME_FILED = "organizationNameField";
    private final String USER_PROFILE_VIEW_TYPE = "userProfileViewType";
    private final String NESTED_ATTRIBUTE_FIELD = "nestedAttributeField";
    private final String OAUTH2_ORGANIZATION_NAME = "organizationName";
    private final String OAUTH2_LOGIN_REDIRECT_URL = "loginRedirectUrl";
    private final String OAUTH2_LOGOUT_REDIRECT_URL = "logoutUrl";
    private final String BUC_EMPID = "empId";
    private final String SUPPORT_GROUP_QR_CODE_URL = "supportGroupQRCodeUrl";

    public static final String TO_BE_REPLACED = "TO_BE_REPLACED";


    private JdbcTemplate jdbcTemplate;

    private TransactionTemplate transactionTemplate;

    private static String replaceRegistrationId(String redirectUrl, String registrationId) {
        String[] split = redirectUrl.split("\\?");
        String target = split[0];
        String param = split.length > 1 ? "?" + split[1] : "";
        String regex = "/[^/]+$";
        return target.replaceAll(regex, "/" + registrationId) + param;
    }

    @Override
    public void migrate(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.transactionTemplate = JdbcOperationsUtil.getTransactionTemplate(dataSource);
        String authType = selectValueByKey("odc.iam.auth.type");
        boolean passwordLoginEnabled = authType.contains("local");
        if (authType.toLowerCase().contains("oauth2")) {
            migrateOauth2PropertyToIntegration(passwordLoginEnabled);
        }
        if (authType.toLowerCase().contains("buc")) {
            migrateBucPropertyToIntegration(passwordLoginEnabled);
        }
    }

    private void migrateOauth2PropertyToIntegration(boolean passwordLoginEnabled) {
        transactionTemplate.execute(status -> {
            try {
                setPasswdLoginEnabled(passwordLoginEnabled);

                List<KeyValue> keyValues = selectValueByKeyLike(SECURITY_REGISTRATION_PREFIX_ODC);

                String defaultName = "defaultOAUTH2config";

                Oauth2Parameter oauth2Parameter = new Oauth2Parameter();
                oauth2Parameter.setName(defaultName);
                String decryptSecret = getValueBySuffix(keyValues, REGISTRATION_CLIENT_SECRET_SUFFIX, 6);
                oauth2Parameter.setSecret(decryptSecret);
                String clientId = getValueBySuffix(keyValues, REGISTRATION_CLIENT_ID_SUFFIX, 6);
                Preconditions.checkNotNull(clientId, "client id migrate is null");
                oauth2Parameter.setClientId(clientId);

                String scopes = getValueBySuffix(keyValues, REGISTRATION_SCOPE_SUFFIX, 6);
                Preconditions.checkNotNull(scopes, "scopes can not be null");
                oauth2Parameter
                        .setScope(Arrays.stream(scopes.split("\\,"))
                                .collect(Collectors.toSet()));
                oauth2Parameter.setAuthorizationGrantType(
                        getValueBySuffix(keyValues, REGISTRATION_AUTHORIZATION_GRANT_TYPE_SUFFIX, 6));
                oauth2Parameter.setClientAuthenticationMethod(
                        getValueBySuffix(keyValues, REGISTRATION_CLIENT_AUTHENTICATION_METHOD_SUFFIX, 6));
                String provider = getValueBySuffix(keyValues, REGISTRATION_PROVIDER_SUFFIX, 6);
                Preconditions.checkArgument(provider != null, "provider can not be null");

                List<KeyValue> providerProperties = selectValueByKeyLike(SECURITY_PROVIDER_PREFIX + provider);
                oauth2Parameter.setAuthUrl(getValueBySuffix(providerProperties, PROVIDER_AUTHORIZATION_URI_SUFFIX, 6));
                oauth2Parameter.setUserInfoUrl(getValueBySuffix(providerProperties, PROVIDER_USER_INFO_URI_SUFFIX, 6));
                oauth2Parameter.setTokenUrl(getValueBySuffix(providerProperties, PROVIDER_TOKEN_URI_SUFFIX, 6));
                oauth2Parameter.setUserInfoAuthenticationMethod(
                        getValueBySuffix(providerProperties, PROVIDER_USER_INFO_AUTHENTICATION_METHOD_SUFFIX, 6));

                List<KeyValue> oauth2Properties = selectValueByKeyLike(OAUTH_PROPERTY_PREFIX);
                MappingRule mappingRule = new MappingRule();
                mappingRule
                        .setUserAccountNameField(getValueBySuffix(oauth2Properties, OAUTH2_USER_ACCOUNT_NAME_FIELD, 2));
                String userNickNameFile = getValueBySuffix(oauth2Properties, OAUTH2_USER_NICK_NAME_FILED, 2);
                Preconditions.checkNotNull(userNickNameFile, "userNickNameFile can not be null");
                mappingRule.setUserNickNameField(Arrays.stream(userNickNameFile.split("\\,"))
                        .collect(Collectors.toSet()));
                mappingRule.setUserProfileViewType(getValueBySuffix(oauth2Properties, USER_PROFILE_VIEW_TYPE, 2));
                mappingRule.setNestedAttributeField(getValueBySuffix(oauth2Properties, NESTED_ATTRIBUTE_FIELD, 2));

                String organizationName = getValueBySuffix(oauth2Properties, OAUTH2_ORGANIZATION_NAME, 2);
                oauth2Parameter.setLogoutUrl(getValueBySuffix(oauth2Properties, OAUTH2_LOGOUT_REDIRECT_URL, 2));


                List<OrganizationEntity> organizations = getOrganizations();
                String enabledOrgName = enabledOrgName(organizationName, organizations);

                organizations.forEach(org -> {
                    String loginRedirectUrl = getValueBySuffix(oauth2Properties, OAUTH2_LOGIN_REDIRECT_URL, 2);
                    Preconditions.checkNotNull(loginRedirectUrl, "loginRedirectUrl");

                    Oauth2Parameter currentOauth2Parameter =
                            ObjectUtil.deepCopy(oauth2Parameter, Oauth2Parameter.class);
                    currentOauth2Parameter.setLoginRedirectUrl(null);

                    String registrationId = org.getId() + "-" + HashUtils.md5(currentOauth2Parameter.getName());
                    currentOauth2Parameter.setRedirectUrl("{baseUrl}/login/oauth2/code/" + registrationId);
                    currentOauth2Parameter.setRegistrationId(registrationId);
                    currentOauth2Parameter.setLoginRedirectUrl(replaceRegistrationId(loginRedirectUrl, registrationId));
                    currentOauth2Parameter.fillParameter();

                    SSOIntegrationConfig ssoIntegrationConfig = new SSOIntegrationConfig();
                    ssoIntegrationConfig.setType("OAUTH2");
                    ssoIntegrationConfig.setName(defaultName);
                    ssoIntegrationConfig.setMappingRule(mappingRule);
                    ssoIntegrationConfig.setSsoParameter(currentOauth2Parameter);
                    // check whether it can convert to client registration
                    ssoIntegrationConfig.toClientRegistration();

                    // creator_id may be 0, means create by system
                    String insertIntegration = "insert into `integration_integration` ("
                            + " `type`,`name`,`creator_id`,`organization_id`,`is_enabled`,`is_builtin`,"
                            + " `create_time`,`update_time`,`configuration`,`secret`,`salt`,`description`)"
                            + " values (?,?,?,?,?,?,?,?,?,?,?,?)";
                    LocalDateTime now = LocalDateTime.now();
                    boolean enabled = org.getName().equals(enabledOrgName);

                    String salt = CryptoUtils.generateSalt();
                    TextEncryptor textEncryptor = Encryptors.aesBase64(org.getSecret(), salt);
                    jdbcTemplate.update(insertIntegration, IntegrationType.SSO.name(), defaultName, 0L,
                            org.getId(),
                            enabled, false, now, now,
                            JsonUtils.toJson(ssoIntegrationConfig),
                            textEncryptor.encrypt(decryptSecret), salt, "default OAUTH2 config");
                });

                migrateSupportGroupQRCodeUrl(getValueBySuffix(oauth2Properties, SUPPORT_GROUP_QR_CODE_URL, 2));
                updateAuthType("local");
                log.warn("You have used oauth2 for auth type, "
                        + "now you have upgraded to 4.2.0 successfully! It required restart odc application");
            } catch (Exception e) {
                // if can't convert to integration_integration, re config
                log.error("migrate oauth2 properties to integration failed", e);
                status.setRollbackOnly();
                throw new RuntimeException("migrate oauth2 properties to integration failed", e);
            }
            return null;
        });
    }

    private String enabledOrgName(String organizationName, List<OrganizationEntity> organizations) {
        if (StringUtils.isNotBlank(organizationName)) {
            return organizationName;
        }
        if (CollectionUtils.isEmpty(organizations)) {
            return "ODC-DEFAULT";
        }
        return organizations.iterator().next().getName();
    }

    private void migrateBucPropertyToIntegration(boolean passwordLoginEnabled) {
        transactionTemplate.execute(status -> {
            try {
                setPasswdLoginEnabled(passwordLoginEnabled);
                List<KeyValue> keyValues = selectValueByKeyLike(SECURITY_REGISTRATION_PREFIX_BUC);

                String defaultName = "defaultBucConfig";

                Oauth2Parameter oauth2Parameter = new Oauth2Parameter();
                oauth2Parameter.setName(defaultName);
                String decryptSecret = getValueBySuffix(keyValues, REGISTRATION_CLIENT_SECRET_SUFFIX, 6);
                oauth2Parameter.setSecret(decryptSecret);
                String clientId = getValueBySuffix(keyValues, REGISTRATION_CLIENT_ID_SUFFIX, 6);
                Preconditions.checkNotNull(clientId, "client id migrate is null");
                oauth2Parameter.setClientId(clientId);
                String scopes = getValueBySuffix(keyValues, REGISTRATION_SCOPE_SUFFIX, 6);
                Preconditions.checkNotNull(scopes, "scopes can not be null");
                oauth2Parameter.setScope(Arrays.stream(scopes.split("\\,"))
                        .collect(Collectors.toSet()));
                oauth2Parameter.setAuthorizationGrantType(
                        getValueBySuffix(keyValues, REGISTRATION_AUTHORIZATION_GRANT_TYPE_SUFFIX, 6));
                oauth2Parameter.setClientAuthenticationMethod(
                        getValueBySuffix(keyValues, REGISTRATION_CLIENT_AUTHENTICATION_METHOD_SUFFIX, 6));
                String provider = getValueBySuffix(keyValues, REGISTRATION_PROVIDER_SUFFIX, 6);
                Preconditions.checkArgument(provider != null, "provider can not be null");

                List<KeyValue> providerProperties = selectValueByKeyLike(SECURITY_PROVIDER_PREFIX + provider);
                oauth2Parameter.setAuthUrl(getValueBySuffix(providerProperties, PROVIDER_AUTHORIZATION_URI_SUFFIX, 6));
                oauth2Parameter.setUserInfoUrl(getValueBySuffix(providerProperties, PROVIDER_USER_INFO_URI_SUFFIX, 6));
                oauth2Parameter.setTokenUrl(getValueBySuffix(providerProperties, PROVIDER_TOKEN_URI_SUFFIX, 6));
                oauth2Parameter.setUserInfoAuthenticationMethod(
                        getValueBySuffix(providerProperties, PROVIDER_USER_INFO_AUTHENTICATION_METHOD_SUFFIX, 6));

                List<KeyValue> bucProperties = selectValueByKeyLike(OAUTH_PROPERTY_BUC_PREFIX);
                MappingRule mappingRule = new MappingRule();
                oauth2Parameter.setLogoutUrl(getValueBySuffix(bucProperties, OAUTH2_LOGOUT_REDIRECT_URL, 3));

                List<KeyValue> oauth2Properties = selectValueByKeyLike(OAUTH_PROPERTY_PREFIX);
                mappingRule
                        .setUserAccountNameField(getValueBySuffix(oauth2Properties, OAUTH2_USER_ACCOUNT_NAME_FIELD, 2));
                String userNickNameFile = getValueBySuffix(oauth2Properties, OAUTH2_USER_NICK_NAME_FILED, 2);
                Preconditions.checkNotNull(userNickNameFile, "userNickNameFile can not be null");
                mappingRule.setUserNickNameField(Arrays.stream(userNickNameFile.split("\\,"))
                        .collect(Collectors.toSet()));


                List<OrganizationEntity> organizations = getOrganizations();
                String enabledOrgName = enabledOrgName(null, organizations);

                organizations.forEach(org -> {
                    String loginRedirectUrl = getValueBySuffix(bucProperties, OAUTH2_LOGIN_REDIRECT_URL, 3);
                    Preconditions.checkNotNull(loginRedirectUrl, "loginRedirectUrl");
                    Oauth2Parameter currentOauth2Parameter =
                            ObjectUtil.deepCopy(oauth2Parameter, Oauth2Parameter.class);
                    currentOauth2Parameter.setLoginRedirectUrl(null);

                    String registrationId = org.getId() + "-" + HashUtils.md5(currentOauth2Parameter.getName());
                    currentOauth2Parameter.setRedirectUrl("{baseUrl}/login/oauth2/code/" + registrationId);
                    currentOauth2Parameter.setRegistrationId(registrationId);
                    currentOauth2Parameter.setLoginRedirectUrl(replaceRegistrationId(loginRedirectUrl, registrationId));
                    currentOauth2Parameter.fillParameter();

                    // 官网推荐配置方式为odc，这里迁移的时候只迁移odc的oauth2配置
                    SSOIntegrationConfig ssoIntegrationConfig = new SSOIntegrationConfig();
                    ssoIntegrationConfig.setType("OAUTH2");
                    ssoIntegrationConfig.setName(defaultName);
                    ssoIntegrationConfig.setMappingRule(mappingRule);
                    ssoIntegrationConfig.setSsoParameter(currentOauth2Parameter);

                    // check whether it can convert to client registration
                    ssoIntegrationConfig.toClientRegistration();

                    String insertIntegration = "insert into `integration_integration` ("
                            + " `type`,`name`,`creator_id`,`organization_id`,`is_enabled`,`is_builtin`, "
                            + " `create_time`,`update_time`,`configuration`,`secret`,`salt`,`description`) "
                            + " values (?,?,?,?,?,?,?,?,?,?,?,?)";
                    LocalDateTime now = LocalDateTime.now();
                    boolean enabled = org.getName().equals(enabledOrgName);
                    String salt = CryptoUtils.generateSalt();
                    TextEncryptor textEncryptor = Encryptors.aesBase64(org.getSecret(), salt);
                    String encrypt = textEncryptor.encrypt(decryptSecret);
                    jdbcTemplate.update(insertIntegration, IntegrationType.SSO.name(), defaultName, 0L,
                            org.getId(),
                            enabled, false, now, now,
                            JsonUtils.toJson(ssoIntegrationConfig),
                            encrypt, salt, "default OAUTH2 config");
                });

                migrateSupportGroupQRCodeUrl(getValueBySuffix(bucProperties, SUPPORT_GROUP_QR_CODE_URL, 3));
                updateAuthType("local");
                log.warn(
                        "You have used oauth2 for auth type, now you have upgraded to 4.2.0, please restart odc application");
            } catch (Exception e) {
                // if can't convert to integration_integration, re config
                log.error("migrate oauth2 properties to integration failed", e);
                status.setRollbackOnly();
                throw new RuntimeException("migrate oauth2 properties to integration failed failed", e);
            }
            return null;
        });
    }

    private List<OrganizationEntity> getOrganizations() {
        String sql = "select id, unique_identifier,`secret`,name,creator_id,"
                + " is_builtin as builtin,description,type,display_name"
                + " from iam_organization where is_builtin = 0 and type='TEAM'"
                + " order by id desc";
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(OrganizationEntity.class));
    }

    private void migrateSupportGroupQRCodeUrl(String supportGroupQRCodeUrl) {
        if (supportGroupQRCodeUrl == null || TO_BE_REPLACED.equals(supportGroupQRCodeUrl)) {
            return;
        }
        String sqlUpdateSupportGroupQRCodeUrl =
                "INSERT INTO config_system_configuration(`key`, `value`, `description`) "
                        + "VALUES('odc.help.supportGroupQRCodeUrl', ? , 'QRCode URL for support group') ON DUPLICATE KEY UPDATE `id`=`id`";
        jdbcTemplate.update(sqlUpdateSupportGroupQRCodeUrl, supportGroupQRCodeUrl);
    }

    private void setPasswdLoginEnabled(boolean passwordLoginEnabled) {
        String sqlUpdatePasswdLoginEnabled =
                "INSERT INTO config_system_configuration(`key`, `value`, `description`) "
                        + "VALUES('odc.iam.password-login-enabled', ? , 'local account password login enabled or not') ON DUPLICATE KEY UPDATE `id`=`id`";
        jdbcTemplate.update(sqlUpdatePasswdLoginEnabled, passwordLoginEnabled);
    }

    private void updateAuthType(String authType) {
        String updateAuthType =
                "update `config_system_configuration` set `value` = ? where `key` = 'odc.iam.auth.type'";
        jdbcTemplate.update(updateAuthType, authType);
    }

    private String selectValueByKey(String key) {
        String querySql = "select `value` from `config_system_configuration` where `key` = ?";
        return jdbcTemplate.queryForObject(querySql, String.class, key);
    }

    private List<KeyValue> selectValueByKeyLike(String key) {
        String querySql = "select `key`, `value` from `config_system_configuration` where `key` like ?";
        return jdbcTemplate.query(querySql, new BeanPropertyRowMapper<>(KeyValue.class), key + "%");
    }

    @Nullable
    public String getValueBySuffix(List<KeyValue> keyValues, String suffix, int pox) {
        String value = keyValues.stream().filter(e -> e.isConfig(suffix, pox)).findFirst().map(KeyValue::getValue)
                .orElseGet(() -> null);
        return TO_BE_REPLACED.equals(value) ? null : value;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    static class KeyValue {
        private String key;
        private String value;

        private static String toLooseBindingString(String target) {
            return target.trim().toLowerCase().replace("_", "")
                    .replace("-", "");

        }

        public boolean isConfig(String configSuffix, int pox) {
            String[] split = key.split("\\.");
            Preconditions.checkArgument(split.length > pox);
            return Objects.equals(toLooseBindingString(configSuffix), toLooseBindingString(split[pox]));
        }

    }


}
