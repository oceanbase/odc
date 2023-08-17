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

import com.oceanbase.odc.core.authority.exception.AccessDeniedException;
import com.oceanbase.odc.service.iam.model.Organization;
import com.oceanbase.odc.service.iam.model.User;

/**
 * @author yizhou.xw
 * @version : AuthenticationFacade.java, v 0.1 2021-07-26 9:36
 */
public interface AuthenticationFacade {

    /**
     * current login user
     *
     * @return currentUser
     * @throws AccessDeniedException if not login
     */
    User currentUser() throws AccessDeniedException;

    /**
     * current userId of login user
     * 
     * @return currentUserId
     * @throws AccessDeniedException if not login
     */
    long currentUserId() throws AccessDeniedException;

    /**
     * current userId of login user
     *
     * @return currentUserIdStr
     * @throws AccessDeniedException if not login
     */
    String currentUserIdStr() throws AccessDeniedException;

    /**
     * current userAccountName of login user
     *
     * @return currentUserAccountName
     * @throws AccessDeniedException if not login
     */
    String currentUserAccountName() throws AccessDeniedException;

    /**
     * current username of login user
     *
     * @return currentUsername
     * @throws AccessDeniedException if not login
     */
    String currentUsername() throws AccessDeniedException;

    /**
     * current organizationId of login user
     * 
     * @return Organization ID
     * @throws AccessDeniedException if not login
     */
    long currentOrganizationId() throws AccessDeniedException;

    /**
     * current organizationId of login user
     *
     * @return Organization ID in String format
     * @throws AccessDeniedException if not login
     */
    String currentOrganizationIdStr() throws AccessDeniedException;

    /**
     * current Organization of login user
     *
     * @return Organization
     * @throws AccessDeniedException if not login
     */
    Organization currentOrganization() throws AccessDeniedException;
}
