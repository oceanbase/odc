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
package com.oceanbase.odc.service.integration.model;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.validation.constraints.NotBlank;

import org.springframework.security.oauth2.client.registration.ClientRegistration;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.service.integration.model.Encryption.EncryptionAlgorithm;
import com.oceanbase.odc.service.integration.saml.SamlParameter;
import com.oceanbase.odc.service.integration.saml.SamlParameter.SecretInfo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class SSOIntegrationConfig implements Serializable {

    private static final long serialVersionUID = 7526472295622776147L;
    /**
     * orgId-name, the separator character should be '-', however some system may not follow this rule,
     * * e.g. use ':' instead, here we support both.
     */
    private static final String REGISTRATION_ID_SEPARATOR = "[-:]";

    String name;
    String type;
    @JsonTypeInfo(use = Id.NAME, include = As.EXTERNAL_PROPERTY, property = "type")
    @JsonSubTypes(value = {
            @JsonSubTypes.Type(value = Oauth2Parameter.class, name = "OAUTH2"),
            @JsonSubTypes.Type(value = OidcParameter.class, names = "OIDC"),
            @JsonSubTypes.Type(value = LdapParameter.class, names = "LDAP"),
            @JsonSubTypes.Type(value = SamlParameter.class, names = "SAML"),
    })
    SSOParameter ssoParameter;

    MappingRule mappingRule;

    public static SSOIntegrationConfig of(IntegrationConfig integrationConfig, Long organizationId) {
        SSOIntegrationConfig ssoIntegrationConfig =
                JsonUtils.fromJson(integrationConfig.getConfiguration(), SSOIntegrationConfig.class);
        checkOrganizationId(ssoIntegrationConfig.resolveRegistrationId(), organizationId);
        switch (ssoIntegrationConfig.getType()) {
            case "OAUTH2":
            case "OIDC":
                Preconditions.checkArgument(integrationConfig.getEncryption().getEnabled()
                        && integrationConfig.getEncryption().getAlgorithm()
                                .equals(EncryptionAlgorithm.RAW));
                Oauth2Parameter parameter = (Oauth2Parameter) ssoIntegrationConfig.getSsoParameter();
                parameter.setName(integrationConfig.getName());
                parameter.fillParameter();
                parameter.setSecret(integrationConfig.getEncryption().getSecret());
                break;
            case "LDAP":
                Preconditions.checkArgument(integrationConfig.getEncryption().getEnabled()
                        && integrationConfig.getEncryption().getAlgorithm()
                                .equals(EncryptionAlgorithm.RAW));
                LdapParameter ldapParameter = (LdapParameter) ssoIntegrationConfig.getSsoParameter();
                ldapParameter.setManagerPassword(integrationConfig.getEncryption().getSecret());
                break;
            case "SAML":
                SamlParameter samlParameter = (SamlParameter) ssoIntegrationConfig.getSsoParameter();
                samlParameter.fillSecret(integrationConfig.getEncryption().getSecret());
                break;
            default:
                throw new UnsupportedOperationException("unknown type=" + ssoIntegrationConfig.getType());
        }
        return ssoIntegrationConfig;
    }

    public static void checkOrganizationId(String registrationId, Long organizationId) {
        Long parsedOrganizationId = parseOrganizationId(registrationId);
        Verify.verify(Objects.equals(parsedOrganizationId, organizationId), "check you organization");
    }

    public static Long parseOrganizationId(String registrationId) {
        String[] split = registrationId.split(REGISTRATION_ID_SEPARATOR);
        Preconditions.checkArgument(split.length > 1);
        return Long.valueOf(split[0]);
    }

    public static String parseRegistrationName(String registrationId) {
        String[] split = registrationId.split(REGISTRATION_ID_SEPARATOR);
        Preconditions.checkArgument(split.length > 1, "invalid registrationId#" + registrationId);
        return split[1];
    }

    public boolean isOauth2OrOidc() {
        return ImmutableSet.of("OAUTH2", "OIDC").contains(type);
    }

    public boolean isLdap() {
        return Objects.equals(type, "LDAP");
    }

    public boolean isSaml() {
        return Objects.equals(type, "SAML");
    }

    public String resolveRegistrationId() {
        if (isOauth2OrOidc()) {
            return ((Oauth2Parameter) ssoParameter).getRegistrationId();
        } else if (isLdap()) {
            return ((LdapParameter) ssoParameter).getRegistrationId();
        } else if (isSaml()) {
            return ((SamlParameter) ssoParameter).getRegistrationId();
        } else {
            throw new UnsupportedOperationException();

        }
    }

    public Long resolveOrganizationId() {
        return parseOrganizationId(resolveRegistrationId());
    }

    public String resolveLoginRedirectUrl() {
        switch (type) {
            case "OAUTH2":
            case "OIDC":
                return ((Oauth2Parameter) ssoParameter).getLoginRedirectUrl();
            case "SAML":
                return ((SamlParameter) ssoParameter).resolveLoginUrl();
            default:
                return null;
        }
    }

    public String resolveLogoutUrl() {
        switch (type) {
            case "OAUTH2":
            case "OIDC":
                return ((Oauth2Parameter) ssoParameter).getLogoutUrl();
            default:
                throw new UnsupportedOperationException("unknown type=" + type);
        }
    }

    public void fillDecryptSecret(String decryptSecret) {
        if (isOauth2OrOidc()) {
            ((Oauth2Parameter) ssoParameter).setSecret(decryptSecret);
        }
        if (isLdap()) {
            ((LdapParameter) ssoParameter).setManagerPassword(decryptSecret);
        }
        if (isSaml()) {
            SecretInfo secretInfo = JsonUtils.fromJson(decryptSecret, SecretInfo.class);
            SamlParameter samlParameter = (SamlParameter) ssoParameter;
            samlParameter.getSigning().setPrivateKey(secretInfo.getSigningPrivateKey());
            samlParameter.getDecryption().setPrivateKey(secretInfo.getDecryptionPrivateKey());
        }
    }

    public ClientRegistration toClientRegistration() {
        switch (type) {
            case "OAUTH2":
                return ((Oauth2Parameter) ssoParameter).toClientRegistration();
            case "OIDC":
                return ((OidcParameter) ssoParameter).toClientRegistration();
            default:
                throw new UnsupportedOperationException("unknown type=" + type);
        }
    }

    public ClientRegistration toTestClientRegistration(String testType) {
        switch (type) {
            case "OAUTH2":
                Oauth2Parameter oauth2 = (Oauth2Parameter) ssoParameter;
                oauth2.amendTestParameter(testType);
                return ((Oauth2Parameter) ssoParameter).toClientRegistration();
            case "OIDC":
                OidcParameter oidc = (OidcParameter) ssoParameter;
                oidc.amendTestParameter(testType);
                return ((OidcParameter) ssoParameter).toClientRegistration();
            default:
                throw new UnsupportedOperationException("unknown type=" + type);
        }
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MappingRule {
        public static final String USER_PROFILE_NESTED = "NESTED";

        public static final String TO_BE_REPLACED = "TO_BE_REPLACED";

        @NotBlank
        private String userAccountNameField;
        private Set<String> userNickNameField;
        private String userProfileViewType;
        private String nestedAttributeField;
        private List<CustomAttribute> extraInfo;
    }

    @Data
    public static class CustomAttribute {
        private String attributeName;
        private String expression;

        public String toAutomationExpression() {
            return "extra#" + attributeName;
        }
    }

}
