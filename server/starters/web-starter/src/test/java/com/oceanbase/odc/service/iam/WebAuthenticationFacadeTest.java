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
package com.oceanbase.odc.service.iam;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.core.authority.exception.AccessDeniedException;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.iam.auth.WebAuthenticationFacade;
import com.oceanbase.odc.service.iam.model.User;
import com.oceanbase.odc.service.iam.util.SecurityContextUtils;

public class WebAuthenticationFacadeTest extends ServiceTestEnv {
    private static final long USER_ID = 1L;
    private static final long ORGANIZATION_ID = 1L;

    private AuthenticationFacade authenticationFacade = new WebAuthenticationFacade();

    @Before
    public void setUp() throws Exception {
        SecurityContextUtils.clear();
    }

    @Test
    public void currentUser_Login_ReturnPresentTrue() {
        SecurityContextUtils.setCurrentUser(USER_ID, ORGANIZATION_ID, "mock user");

        User user = authenticationFacade.currentUser();

        Assert.assertNotNull(user);
    }

    @Test(expected = AccessDeniedException.class)
    public void currentUser_NotLogin_ReturnPresentFalse() {
        authenticationFacade.currentUser();
    }

    @Test(expected = AccessDeniedException.class)
    public void currentUserId_NotLogin_Exception() {
        authenticationFacade.currentUserId();
    }

    @Test
    public void currentUserId_Login_Match() {
        SecurityContextUtils.setCurrentUser(USER_ID, ORGANIZATION_ID, "mock user");

        long userId = authenticationFacade.currentUserId();

        Assert.assertEquals(USER_ID, userId);
    }
}
