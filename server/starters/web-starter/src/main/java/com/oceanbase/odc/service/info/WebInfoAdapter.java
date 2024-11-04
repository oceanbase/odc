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
package com.oceanbase.odc.service.info;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.service.common.util.ConditionalOnProperty;
import com.oceanbase.odc.service.iam.auth.play.PlaysiteOpenApiProperties;
import com.oceanbase.odc.service.integration.IntegrationService;
import com.oceanbase.odc.service.integration.model.SSOIntegrationConfig;

@Service
@Profile("alipay")
@ConditionalOnProperty(value = "odc.iam.auth.type", havingValues = {"local", "alipay", "buc", "oauth2"})
@SkipAuthorize("odc internal usage")
public class WebInfoAdapter implements InfoAdapter {

    @Value("${odc.iam.auth.type}")
    protected Set<String> authType;
    @Autowired
    protected IntegrationService integrationService;
    @Value("${odc.iam.password-login-enabled:true}")
    private Boolean passwordLoginEnabled;
    @Value("${odc.help.supportGroupQRCodeUrl:#{null}}")
    private String supportGroupQRCodeUrl;
    @Autowired
    private PlaysiteOpenApiProperties alipayOpenApiProperties;
    @Autowired
    private BuildProperties buildProperties;

    @Override
    public boolean isPasswordLoginEnabled() {
        if (!authType.contains("local")) {
            return false;
        }
        return passwordLoginEnabled;
    }

    @Override
    public String getLoginUrl(HttpServletRequest request) {
        if (authType.contains("alipay")) {
            return alipayOpenApiProperties.getObOfficialLoginUrl();
        } else if (authType.contains("local")) {
            SSOIntegrationConfig sSoClientRegistration = integrationService.getSSoIntegrationConfig();
            if (sSoClientRegistration == null) {
                return null;
            }
            return sSoClientRegistration.resolveLoginRedirectUrl();
        }
        return null;
    }

    @Override
    public String getLogoutUrl(HttpServletRequest request) {
        if (authType.contains("alipay")) {
            return alipayOpenApiProperties.getObOfficialLogoutUrl();
        } else if (authType.contains("local")) {
            SSOIntegrationConfig sSoClientRegistration = integrationService.getSSoIntegrationConfig();
            if (sSoClientRegistration == null || !sSoClientRegistration.isOauth2OrOidc()) {
                return null;
            }
            return sSoClientRegistration.resolveLogoutUrl();
        }
        return null;
    }

    @Override
    public boolean isSSoLoginEnabled(HttpServletRequest request) {
        if (authType.contains("local")) {
            SSOIntegrationConfig ssoIntegrationConfig = integrationService.getSSoIntegrationConfig();
            return ssoIntegrationConfig != null;
        }
        return getLoginUrl(request) != null;
    }

    @Override
    public String ssoLoginName() {
        SSOIntegrationConfig sSoIntegrationConfig = integrationService.getSSoIntegrationConfig();
        if (sSoIntegrationConfig == null) {
            return "";
        }
        return sSoIntegrationConfig.getName();
    }

    @Override
    public String ssoLoginType() {
        SSOIntegrationConfig sSoIntegrationConfig = integrationService.getSSoIntegrationConfig();
        if (sSoIntegrationConfig == null) {
            return "";
        }
        return sSoIntegrationConfig.getType();
    }

    @Override
    public String getSupportGroupQRCodeUrl() {
        return supportGroupQRCodeUrl;
    }

    @Override
    public String getBuildVersion() {
        return buildProperties.getVersion();
    }

    @Override
    public OffsetDateTime getBuildTime() {
        return OffsetDateTime.ofInstant(buildProperties.getTime(), ZoneId.systemDefault());
    }

}
