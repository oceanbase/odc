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

import static com.oceanbase.odc.service.automation.model.TriggerEvent.LOGIN_SUCCESS;

import java.io.IOException;
import java.security.Principal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

import javax.security.auth.Subject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.common.trace.TraceContextHolder;
import com.oceanbase.odc.core.authority.SecurityManager;
import com.oceanbase.odc.core.authority.exception.AuthenticationException;
import com.oceanbase.odc.core.shared.constant.OdcConstants;
import com.oceanbase.odc.core.shared.constant.OrganizationType;
import com.oceanbase.odc.metadb.iam.OrganizationRepository;
import com.oceanbase.odc.service.automation.model.TriggerEvent;
import com.oceanbase.odc.service.collaboration.OrganizationResourceMigrator;
import com.oceanbase.odc.service.collaboration.project.ProjectService;
import com.oceanbase.odc.service.common.response.Responses;
import com.oceanbase.odc.service.common.response.SuccessResponse;
import com.oceanbase.odc.service.common.util.SpringContextUtil;
import com.oceanbase.odc.service.common.util.WebRequestUtils;
import com.oceanbase.odc.service.common.util.WebResponseUtils;
import com.oceanbase.odc.service.iam.LoginHistoryService;
import com.oceanbase.odc.service.iam.OrganizationMapper;
import com.oceanbase.odc.service.iam.model.LoginHistory;
import com.oceanbase.odc.service.iam.model.Organization;
import com.oceanbase.odc.service.iam.model.User;
import com.oceanbase.odc.service.iam.util.SecurityContextUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * @author wenniu.ly
 * @date 2021/8/4
 */

@Slf4j
@Component
@ConditionalOnProperty(value = "odc.iam.auth.method", havingValue = "jsession")
public class CustomAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {
    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    @Qualifier("organizationResourceMigrator")
    private OrganizationResourceMigrator organizationResourceMigrator;

    @Autowired
    private ProjectService projectService;

    @Value("${odc.integration.bastion.enabled:false}")
    private boolean bastionEnabled;

    private final OrganizationMapper organizationMapper = OrganizationMapper.INSTANCE;
    private final SecurityManager securityManager;
    private final LoginHistoryService loginHistoryService;

    public CustomAuthenticationSuccessHandler(SecurityManager securityManager,
            LoginHistoryService loginHistoryService) {
        Validate.notNull(securityManager, "securityManager");
        Validate.notNull(loginHistoryService, "loginHistoryService");
        this.securityManager = securityManager;
        this.loginHistoryService = loginHistoryService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse,
            Authentication authentication) throws IOException, ServletException {
        log.info("Authentication successful for {}", httpServletRequest.getRequestURI());
        LoginHistory loginHistory = new LoginHistory();
        loginHistory.setSuccess(true);
        loginHistory.setUserId(TraceContextHolder.getUserId());
        loginHistory.setOrganizationId(TraceContextHolder.getOrganizationId());
        loginHistory.setAccountName(TraceContextHolder.getAccountName());
        loginHistory.setLoginTime(OffsetDateTime.now());
        loginHistory.setSuccess(true);
        boolean recorded = loginHistoryService.record(loginHistory);
        if (recorded) {
            SpringContextUtil.publishEvent(new TriggerEvent(LOGIN_SUCCESS, authentication.getPrincipal()));
        }

        if (authentication.getPrincipal() instanceof User) {
            User user = (User) authentication.getPrincipal();
            organizationResourceMigrator.migrate(user);
            List<Organization> belongedOrganizations = organizationRepository.findByUserId(user.getId()).stream()
                    .map(organizationMapper::entityToModel).collect(Collectors.toList());
            Organization team = belongedOrganizations.stream()
                    .filter(organization -> organization.getType() == OrganizationType.TEAM)
                    .findFirst().orElseThrow(() -> new RuntimeException(
                            "User doesn't belong to any TEAM organization, userId=" + user.getId()));
            user.setOrganizationId(team.getId());
            user.setOrganizationType(OrganizationType.TEAM);
            SecurityContextUtils.switchCurrentUserOrganization(user, team, httpServletRequest, true);
            // If bastion is enabled, every user must hold a built-in project for create temporary SQL console
            if (bastionEnabled) {
                projectService.createProjectIfNotExists(user);
            }
        }

        // Login logic for Security Framework
        if (log.isDebugEnabled()) {
            log.debug("Not login for security framework, begin to login...");
        }

        Subject subject;
        try {
            subject = securityManager.login(null, null);
        } catch (AuthenticationException e) {
            log.error("Fail to login for security framework", e);
            throw new IOException(e);
        }
        if (log.isDebugEnabled()) {
            log.debug("Login successfully, principals={}",
                    subject.getPrincipals().stream().map(Principal::toString).collect(Collectors.joining(",")));
        }
        // if odc data api, use json response
        String requestURI = httpServletRequest.getRequestURI();
        if (requestURI.startsWith("/api/v")) {
            // if login api, set response to OK
            // not affect BUC(/login/oauth2/code/buc)
            if (requestURI.contains("/login")) {
                handleAfterSucceed(httpServletRequest, httpServletResponse, authentication);
            } else {
                log.info("Login from non-login API, requestURI={}", requestURI);
            }
            return;
        }
        // if contains odc_back_url, redirect to it
        String odcBackUrl = httpServletRequest.getParameter(OdcConstants.ODC_BACK_URL_PARAM);
        if (WebRequestUtils.isRedirectUrlValid(httpServletRequest, odcBackUrl)) {
            getRedirectStrategy().sendRedirect(httpServletRequest, httpServletResponse, odcBackUrl);
            return;
        }
        // or else, try redirect to original url, will redirect to '/' if no original url detected
        super.onAuthenticationSuccess(httpServletRequest, httpServletResponse, authentication);
    }

    protected void handleAfterSucceed(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse,
            Authentication authentication) throws IOException {
        SuccessResponse<String> successResponse = Responses.success("ok");
        WebResponseUtils.writeJsonObjectWithOkStatus(successResponse, httpServletRequest, httpServletResponse);
    }
}
