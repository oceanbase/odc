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
package com.oceanbase.odc.core.authority;

import java.util.Arrays;
import java.util.Collections;

import javax.security.auth.Subject;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.oceanbase.odc.core.authority.auth.Authorizer;
import com.oceanbase.odc.core.authority.auth.DefaultAuthenticatorManager;
import com.oceanbase.odc.core.authority.auth.DefaultAuthorizerManager;
import com.oceanbase.odc.core.authority.auth.EmptyAuthenticator;
import com.oceanbase.odc.core.authority.auth.PermissionStrategy;
import com.oceanbase.odc.core.authority.auth.SecurityContext;
import com.oceanbase.odc.core.authority.exception.AccessDeniedException;
import com.oceanbase.odc.core.authority.exception.AuthenticationException;
import com.oceanbase.odc.core.authority.model.BasicAuthenticationToken;
import com.oceanbase.odc.core.authority.permission.AllPermission;
import com.oceanbase.odc.core.authority.tool.FirstTestAuthorizer;
import com.oceanbase.odc.core.authority.tool.TestPermissionStrategy;

/**
 * Test object for {@link Authorizer}
 *
 * @author yh263208
 * @date 2021-07-20 17:09
 * @since ODC_release_3.2.0
 */
public class DefaultAuthorizerManagerTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void permit_defaultAuthorizerManager_allGranted() throws AuthenticationException {
        Subject subject = login();
        DefaultAuthorizerManager manager = new DefaultAuthorizerManager(Collections.emptyList());
        manager.addAuthorizer(new FirstTestAuthorizer(true, false, false));
        SecurityContext result = manager.permit(subject, Arrays.asList(new AllPermission(), new AllPermission()));
        Assert.assertTrue((new TestPermissionStrategy(false)).decide(result));
    }

    @Test
    public void permit_defaultAuthorizerManager_allDenied() throws AuthenticationException {
        Subject subject = login();
        DefaultAuthorizerManager manager = new DefaultAuthorizerManager(Collections.emptyList());
        manager.addAuthorizer(new FirstTestAuthorizer(false, false, false));
        SecurityContext result = manager.permit(subject, Arrays.asList(new AllPermission(), new AllPermission()));
        Assert.assertFalse((new TestPermissionStrategy(true)).decide(result));
    }

    @Test
    public void permit_defaultAuthorizerManager_permitAbortted() throws AuthenticationException {
        Subject subject = login();
        DefaultAuthorizerManager manager = new DefaultAuthorizerManager(Collections.emptyList());
        manager.addAuthorizer(new FirstTestAuthorizer(false, true, false));
        manager.addAuthorizer(new FirstTestAuthorizer(true, false, false));
        SecurityContext result = manager.permit(subject, Arrays.asList(new AllPermission(), new AllPermission()));
        Assert.assertFalse((new TestPermissionStrategy(true)).decide(result));
    }

    @Test
    public void permit_defaultAuthorizerManagerWithPrivileged_allGranted() throws AuthenticationException {
        Subject subject = login();
        DefaultAuthorizerManager manager = new DefaultAuthorizerManager(Collections.emptyList());
        manager.addAuthorizer(new FirstTestAuthorizer(false, false, true));
        manager.addAuthorizer(new FirstTestAuthorizer(true, false, false));
        SecurityContext result = manager.permit(subject, Arrays.asList(new AllPermission(), new AllPermission()));
        Assert.assertTrue((new TestPermissionStrategy(true)).decide(result));
    }

    @Test
    public void permit_defaultAuthorizerManagerWithStrategy_accessDenied() throws AuthenticationException {
        Subject subject = login();
        Assert.assertNotNull(subject);
        Assert.assertEquals(subject.getPrincipals().size(), 2);
        DefaultAuthorizerManager manager = new DefaultAuthorizerManager(Collections.emptyList());
        manager.addAuthorizer(new FirstTestAuthorizer(false, false, false));
        manager.addAuthorizer(new FirstTestAuthorizer(true, false, false));
        SecurityContext result = manager.permit(subject, Arrays.asList(new AllPermission(), new AllPermission()));
        PermissionStrategy strategy = context -> {
            if (context.isPrivileged()) {
                return true;
            } else if (context.isAllPermitted()) {
                return true;
            } else if (context.isAllDenied()) {
                return false;
            }
            return context.permit(new AllPermission());
        };
        Assert.assertTrue(strategy.decide(result));
        thrown.expect(AccessDeniedException.class);
        thrown.expectMessage("Access Denied");
        manager.checkPermission(subject, Arrays.asList(new AllPermission(), new AllPermission()), context -> false);
    }

    private Subject login() throws AuthenticationException {
        DefaultAuthenticatorManager manager = new DefaultAuthenticatorManager(Collections.emptyList());
        manager.addAuthenticator(new EmptyAuthenticator());
        Subject subject = manager.authenticate(Arrays.asList(
                new BasicAuthenticationToken("David", "123455"),
                new BasicAuthenticationToken("Marry", "123456")));
        Assert.assertNotNull(subject);
        Assert.assertEquals(subject.getPrincipals().size(), 2);
        return subject;
    }

}
