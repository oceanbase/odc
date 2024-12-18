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
package com.oceanbase.odc.service.iam.auth;

import static com.oceanbase.odc.service.integration.model.SSOIntegrationConfig.parseOrganizationId;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.naming.NamingEnumeration;
import javax.naming.directory.Attribute;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.saml2.provider.service.authentication.DefaultSaml2AuthenticatedPrincipal;
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication;
import org.springframework.stereotype.Component;

import com.google.common.base.MoreObjects;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.service.automation.util.EventParseUtil;
import com.oceanbase.odc.service.iam.auth.oauth2.MappingResult;
import com.oceanbase.odc.service.integration.ldap.LdapConfigRegistrationManager;
import com.oceanbase.odc.service.integration.model.LdapContextHolder;
import com.oceanbase.odc.service.integration.model.LdapContextHolder.LdapContext;
import com.oceanbase.odc.service.integration.model.SSOIntegrationConfig;
import com.oceanbase.odc.service.integration.model.SSOIntegrationConfig.CustomAttribute;
import com.oceanbase.odc.service.integration.model.SSOIntegrationConfig.MappingRule;
import com.oceanbase.odc.service.integration.oauth2.AddableClientRegistrationManager;
import com.oceanbase.odc.service.integration.oauth2.TestLoginManager;
import com.oceanbase.odc.service.integration.saml.AddableRelyingPartyRegistrationRepository;

import lombok.NonNull;
import lombok.SneakyThrows;

@Component
@Profile("alipay")
@ConditionalOnProperty(value = {"odc.iam.auth.type"}, havingValue = "local")
public class MappingRuleConvert {

    @Autowired
    private AddableClientRegistrationManager addableClientRegistrationManager;

    @Autowired
    private LdapConfigRegistrationManager ldapConfigRegistrationManager;

    @Autowired
    private TestLoginManager testLoginManager;

    @Autowired
    private AddableRelyingPartyRegistrationRepository addableRelyingPartyRegistrationRepository;

    public MappingResult resolveOAuthMappingResult(OAuth2UserRequest userRequest, Map<String, Object> userInfoMap) {
        MappingRule mappingRule = resolveMappingRule(userRequest);
        Verify.notNull(mappingRule, "mappingRule");
        userInfoMap = getUserInfoMapFromResponse(userInfoMap, mappingRule);
        testLoginManager.saveOauth2InfoIfNeed(JsonUtils.toJson(userInfoMap));
        testLoginManager.abortIfOAuthTestLoginInfo();
        Long organizationId = parseOrganizationId(userRequest.getClientRegistration().getRegistrationId());
        String userAccountName = String.valueOf(userInfoMap.get(mappingRule.getUserAccountNameField()));
        String parseExtraInfo = parseExtraInfo(userInfoMap, mappingRule);
        String name = getName(userInfoMap, mappingRule);
        return MappingResult.builder()
                .organizationId(organizationId)
                .userAccountName(userAccountName)
                .userNickName(name)
                .extraInfo(parseExtraInfo)
                .isAdmin(false)
                .sourceUserInfoMap(userInfoMap)
                .build();
    }


    public MappingResult resolveLdapMappingResult(DirContextOperations ctx, String username) {
        Map<String, Object> userInfoMap = getUserInfoMap(ctx);
        testLoginManager.saveLdapTestIdIfNeed(JsonUtils.toJson(userInfoMap));
        testLoginManager.abortIfLdapTestLogin();
        LdapContext context = LdapContextHolder.getContext();
        SSOIntegrationConfig ssoIntegrationConfig = ldapConfigRegistrationManager.findByRegistrationId(
                context.getRegistrationId());
        MappingRule mappingRule = ssoIntegrationConfig.getMappingRule();
        String name = getName(userInfoMap, mappingRule);
        String parseExtraInfo = parseExtraInfo(userInfoMap, mappingRule);
        return MappingResult.builder()
                .organizationId(ssoIntegrationConfig.resolveOrganizationId())
                .userAccountName(username)
                .userNickName(name)
                .extraInfo(parseExtraInfo)
                .isAdmin(false)
                .sourceUserInfoMap(userInfoMap)
                .build();
    }

    public MappingResult resolveSamlMappingResult(Saml2Authentication saml2Authentication) {
        DefaultSaml2AuthenticatedPrincipal principal =
                (DefaultSaml2AuthenticatedPrincipal) saml2Authentication.getPrincipal();
        Map<String, Object> userInfoMap = getUserInfoMap(principal);
        testLoginManager.saveSamlInfoIfNeed(JsonUtils.toJson(userInfoMap));
        testLoginManager.abortIfSamlTestLogin();

        String relyingPartyRegistrationId = principal.getRelyingPartyRegistrationId();

        SSOIntegrationConfig ssoIntegrationConfig =
                addableRelyingPartyRegistrationRepository.findConfigByRegistrationId(relyingPartyRegistrationId);
        com.google.common.base.Verify.verifyNotNull(ssoIntegrationConfig, "ssoIntegrationConfig");
        MappingRule mappingRule = ssoIntegrationConfig.getMappingRule();
        String name = principal.getName();
        String parseExtraInfo = parseExtraInfo(userInfoMap, mappingRule);

        return MappingResult.builder()
                .organizationId(SSOIntegrationConfig.parseOrganizationId(relyingPartyRegistrationId))
                .userAccountName(name)
                .userNickName(getName(userInfoMap, mappingRule))
                .isAdmin(false)
                .extraInfo(parseExtraInfo)
                .sourceUserInfoMap(userInfoMap)
                .build();
    }

    private Map<String, Object> getUserInfoMap(DefaultSaml2AuthenticatedPrincipal principal) {
        return principal.getAttributes().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().size() == 1 ? entry.getValue().get(0) : entry.getValue()));
    }


    @SneakyThrows
    private Map<String, Object> getUserInfoMap(DirContextOperations ctx) {
        NamingEnumeration<? extends Attribute> all = ctx.getAttributes().getAll();
        Map<String, Object> userInfoMap = new HashMap<>();
        while (all.hasMore()) {
            Attribute next = all.next();
            String attribute = next.getID();
            if (!attribute.toLowerCase().contains("password")) {
                userInfoMap.put(attribute, next.get());
            }
        }
        return userInfoMap;
    }

    private Map<String, Object> getUserInfoMapFromResponse(Map<String, Object> body,
            MappingRule mappingRule) {
        PreConditions.notNull(body, "oauth2 userProfile");

        if (mappingRule != null && MappingRule.USER_PROFILE_NESTED.equals(mappingRule.getUserProfileViewType())) {
            Object attributes = body.get(mappingRule.getNestedAttributeField());
            if (Objects.nonNull(attributes) && attributes instanceof Map<?, ?>) {
                return (Map<String, Object>) attributes;
            } else {
                throw new InternalAuthenticationServiceException(
                        "can't get attributes from response, please check your [nestedAttributeField] correct configuration. or you can set [userProfileViewType] = FLAT");
            }
        }
        return body;
    }

    private MappingRule resolveMappingRule(OAuth2UserRequest userRequest) {
        PreConditions.notNull(addableClientRegistrationManager, "addableClientRegistrationManager");
        SSOIntegrationConfig ssoIntegrationConfig = addableClientRegistrationManager.findConfigByRegistrationId(
                userRequest.getClientRegistration().getRegistrationId());
        return ssoIntegrationConfig.getMappingRule();
    }


    @Nullable
    private String getName(Map<String, Object> userInfoMap, MappingRule mappingRule) {
        Set<String> nameFields = MoreObjects.firstNonNull(mappingRule.getUserNickNameField(), new HashSet<>());
        Object o = nameFields.stream().map(userInfoMap::get).filter(Objects::nonNull).findFirst().orElse(null);
        return o == null ? null : String.valueOf(o);
    }

    private String parseExtraInfo(@NonNull Map<String, Object> userInfoMap,
            MappingRule mappingRule) {
        List<CustomAttribute> extraInfo = mappingRule.getExtraInfo();
        Map<String, Object> extraInfoMap = new HashMap<>();
        if (CollectionUtils.isEmpty(extraInfo)) {
            return "{}";
        }
        extraInfo.forEach(rule -> {
            try {
                Object root = EventParseUtil.parseObject(userInfoMap, rule.getExpression());
                extraInfoMap.putIfAbsent(rule.getAttributeName(), root);
            } catch (Exception e) {
                throw new InternalAuthenticationServiceException(
                        "parse extra info fail, attribute=" + rule.getAttributeName() + " expression="
                                + rule.getAttributeName(),
                        e);
            }
        });
        return JsonUtils.toJson(extraInfoMap);
    }
}
