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
package com.oceanbase.odc.service.iam.util;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.authority.DefaultLoginSecurityManager;
import com.oceanbase.odc.core.authority.auth.SecurityContext;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.OdcConstants;
import com.oceanbase.odc.core.shared.constant.OrganizationType;
import com.oceanbase.odc.metadb.iam.OrganizationEntity;
import com.oceanbase.odc.metadb.iam.OrganizationRepository;
import com.oceanbase.odc.service.common.util.SpringContextUtil;
import com.oceanbase.odc.service.connection.CloudMetadataClient;
import com.oceanbase.odc.service.iam.model.Organization;
import com.oceanbase.odc.service.iam.model.User;

public class SecurityContextUtils {

    /**
     * set current user for simulate login user in backend schedule
     */
    public static void setCurrentUser(@NotNull Long userId, Long organizationId, String username) {
        if (Objects.isNull(organizationId)) {
            organizationId = OdcConstants.DEFAULT_ORGANIZATION_ID;
        }
        if (Objects.isNull(username)) {
            username = "context-username-who-with-id-" + userId;
        }
        User user = new User();
        user.setId(userId);
        user.setOrganizationId(organizationId);
        user.setAccountName(username);

        SecurityContextUtils.setCurrentUser(user);
    }

    public static void setCurrentUser(@NotNull User user) {
        setCloudUid(user);

        // Spring Security Context
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, ""));

        // ODC Security Context
        setOdcSecurityContext(user);
    }

    public static void switchCurrentUserOrganization(@NotNull User user, @NotNull Organization organization,
            HttpServletRequest request,
            boolean createSession) {
        setCloudUid(user);

        // Spring Security Context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();
        currentUser.setOrganizationId(user.getOrganizationId());
        currentUser.setOrganizationType(user.getOrganizationType());
        if (!(authentication instanceof PreAuthenticatedAuthenticationToken)) {
            request.getSession(createSession).setAttribute(
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                    SecurityContextHolder.getContext());
        }
        // ODC Security Context
        setOdcSecurityContext(user);
    }

    private static void setCloudUid(User user) {
        CloudMetadataClient cloudMetadataClient = SpringContextUtil.getBean(CloudMetadataClient.class);
        if (cloudMetadataClient.supportsCloudMetadata() && StringUtils.isEmpty(user.getParentUid())) {
            OrganizationRepository repository =
                    (OrganizationRepository) SpringContextUtil.getBean("organizationRepository");
            List<OrganizationEntity> organizationEntities = repository.findByTypeAndUserId(OrganizationType.TEAM,
                    user.getId());
            PreConditions.validSingleton(organizationEntities, "organizationEntities");
            user.setParentUid(organizationEntities.get(0).getName());
        }
    }

    private static void setOdcSecurityContext(User user) {
        Subject subject = new Subject(true, new HashSet<>(Collections.singletonList(user)),
                Collections.emptySet(), Collections.emptySet());
        DefaultLoginSecurityManager.setContext(subject);
        SecurityContext securityContext = new SecurityContext(subject);
        DefaultLoginSecurityManager.setSecurityContext(securityContext);
    }

    public static void clear() {
        // Spring Security Context
        SecurityContextHolder.clearContext();

        // ODC Security Context
        DefaultLoginSecurityManager.removeSecurityContext();
        DefaultLoginSecurityManager.removeContext();
    }

}
