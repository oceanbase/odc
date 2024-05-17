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

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.github.benmanes.caffeine.cache.LoadingCache;
import com.oceanbase.odc.config.CommonSecurityProperties;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.constant.OrganizationType;
import com.oceanbase.odc.core.shared.exception.AccessDeniedException;
import com.oceanbase.odc.service.common.util.SpringContextUtil;
import com.oceanbase.odc.service.iam.OrganizationService;
import com.oceanbase.odc.service.iam.VerticalPermissionValidator;
import com.oceanbase.odc.service.iam.model.Organization;
import com.oceanbase.odc.service.iam.model.User;
import com.oceanbase.odc.service.iam.util.SecurityContextUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2023/7/14 14:15
 * @Description: []
 */
@Component
@Slf4j
public class OrganizationAuthenticationInterceptor implements HandlerInterceptor {
    private final String[] ORGANIZATION_AUTH_WHITE_LIST = new String[] {
            "/api/v1/user/csrfToken",
            "/api/v2/iam/users/me/organizations",
            "/api/v2/iam/login",
            "/api/v2/flow/flowInstances/*/tasks/download",
            "/api/v2/flow/flowInstances/*/tasks/log/download",
            "/api/v2/flow/flowInstances/*/tasks/rollbackPlan/download",
            "/api/v2/aliyun/generic/**",
            "/api/v2/objectstorage/**",
            "/api/v2/connect/sessions/*/sqls/*/download",
            "/api/v2/datasource/sessions/*/sqls/*/download",
            "/api/v2/config/**",
            "/api/v2/snippet/builtinSnippets"
    };

    @Autowired
    private CommonSecurityProperties commonSecurityProperties;

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private AuthenticationFacade authenticationFacade;

    @Autowired
    private LoadingCache<Long, List<Organization>> userId2OrganizationsCache;

    @Autowired
    private VerticalPermissionValidator verticalPermissionValidator;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (SpringContextUtil.isActive("clientMode")) {
            return true;
        }

        boolean urlInAuthWhiteList = Arrays.stream(commonSecurityProperties.getAuthWhitelist())
                .anyMatch(url -> new AntPathRequestMatcher(url).matches(request));
        if (urlInAuthWhiteList) {
            return true;
        }
        boolean urlInWhiteList = Arrays.stream(ORGANIZATION_AUTH_WHITE_LIST)
                .anyMatch(url -> new AntPathRequestMatcher(url).matches(request));
        if (urlInWhiteList) {
            return true;
        }

        String actual = request.getParameter("currentOrganizationId");
        if (StringUtils.isEmpty(actual)) {
            actual = request.getHeader("currentOrganizationId");
        }
        if (StringUtils.isEmpty(actual)) {
            log.info(
                    "Null or empty 'currentOrganizationId' value from http request parameter and header, request url={}",
                    request.getRequestURL());
            throw new AccessDeniedException(ErrorCodes.BadRequest,
                    "please set currentOrganizationId parameter or header in request");
        }
        long actualOrganizationId = Long.valueOf(actual).longValue();
        if (authenticationFacade.currentOrganizationId() == actualOrganizationId
                && authenticationFacade.currentOrganization().getType() == OrganizationType.TEAM) {
            return true;
        }
        List<Organization> belongedToOrganizations =
                userId2OrganizationsCache.get(authenticationFacade.currentUserId());
        Organization team = belongedToOrganizations.stream()
                .filter(organization -> organization.getType() == OrganizationType.TEAM)
                .findFirst().orElseThrow(() -> new RuntimeException("User doesn't belong to any TEAM organization"));

        belongedToOrganizations = belongedToOrganizations.stream().filter(o -> {
            if (o.getType() == OrganizationType.TEAM) {
                return true;
            }
            return verticalPermissionValidator.implies(o, Arrays.asList("read", "update"), team.getId());
        }).collect(Collectors.toList());

        if (Objects.isNull(belongedToOrganizations) || !(belongedToOrganizations.stream()
                .map(Organization::getId).collect(Collectors.toSet()).contains(actualOrganizationId))) {
            throw new AccessDeniedException();
        }
        String finalActual = actual;
        Organization target = belongedToOrganizations.stream()
                .filter(organization -> organization.getId().longValue() == Long.valueOf(
                        finalActual).longValue())
                .findFirst().get();

        User user = authenticationFacade.currentUser();
        user.setOrganizationId(target.getId());
        user.setOrganizationType(target.getType());
        SecurityContextUtils.switchCurrentUserOrganization(user, target, request, true);
        return true;
    }

}
