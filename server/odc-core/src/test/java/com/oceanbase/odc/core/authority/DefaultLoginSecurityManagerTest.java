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

import java.security.Principal;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.security.auth.Subject;

import org.junit.After;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.authority.SecurityManager.DelegateSessionManager;
import com.oceanbase.odc.core.authority.auth.Authorizer;
import com.oceanbase.odc.core.authority.auth.DefaultAuthenticatorManager;
import com.oceanbase.odc.core.authority.auth.DefaultAuthorizerManager;
import com.oceanbase.odc.core.authority.auth.DefaultPermissionStrategy;
import com.oceanbase.odc.core.authority.auth.EmptyAuthenticator;
import com.oceanbase.odc.core.authority.auth.PermissionStrategy;
import com.oceanbase.odc.core.authority.auth.ReturnValueProvider;
import com.oceanbase.odc.core.authority.auth.SecurityContext;
import com.oceanbase.odc.core.authority.exception.AccessDeniedException;
import com.oceanbase.odc.core.authority.exception.AuthenticationException;
import com.oceanbase.odc.core.authority.model.BaseAuthenticationToken;
import com.oceanbase.odc.core.authority.model.BasicAuthenticationToken;
import com.oceanbase.odc.core.authority.model.LoginSecurityManagerConfig;
import com.oceanbase.odc.core.authority.model.SecurityResource;
import com.oceanbase.odc.core.authority.permission.AllPermission;
import com.oceanbase.odc.core.authority.session.InMemorySecuritySessionRepository;
import com.oceanbase.odc.core.authority.session.SecuritySession;
import com.oceanbase.odc.core.authority.session.factory.DefaultSecuritySessionFactory;
import com.oceanbase.odc.core.authority.session.manager.DefaultSecuritySessionManager;
import com.oceanbase.odc.core.authority.tool.FirstTestAuthorizer;
import com.oceanbase.odc.core.authority.tool.SecondTestAuthorizer;
import com.oceanbase.odc.core.authority.tool.TestPermissionProvider;
import com.oceanbase.odc.core.authority.util.SecurityConstants;

/**
 * Test object for {@link DefaultLoginSecurityManager}
 *
 * @author yh263208
 * @date 2021-07-22 19:29
 * @since ODC_release_3.2.0
 */
public class DefaultLoginSecurityManagerTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @After
    public void clear() {
        DefaultLoginSecurityManager.removeContext();
        DefaultLoginSecurityManager.removeSecurityContext();
    }

    @Test
    public void login_normalUser_loginSucceed() {
        SecurityManager manager = getSecurityManager(context -> true);
        String sessionId = StringUtils.uuidNoHyphen();
        Subject subject = login(Arrays.asList(getToken("David", "123456"),
                getToken("Marry", "abcde")), sessionId, manager);
        Set<String> principals = subject.getPrincipals().stream().map(Principal::getName).collect(Collectors.toSet());
        Assert.assertEquals(principals.size(), 2);
        Assert.assertTrue(principals.contains("David"));
        Assert.assertTrue(principals.contains("Marry"));
        SecuritySession session = manager.getSession(sessionId);
        Assert.assertNotNull(session);
        Assert.assertEquals(sessionId, session.getId());
    }

    @Test
    public void login_multiLogin_loginSucceed() {
        SecurityManager manager = getSecurityManager(context -> true);
        String sessionId = StringUtils.uuidNoHyphen();
        Subject subject = login(Arrays.asList(getToken("David", "123456"),
                getToken("Marry", "abcde")), sessionId, manager);
        Set<String> principals = subject.getPrincipals().stream().map(Principal::getName).collect(Collectors.toSet());
        Assert.assertEquals(principals.size(), 2);
        subject = login(Arrays.asList(getToken("David", "123456"),
                getToken("Marry", "abcde")), sessionId, manager);
        principals = subject.getPrincipals().stream().map(Principal::getName).collect(Collectors.toSet());
        Assert.assertEquals(principals.size(), 2);
        Assert.assertTrue(principals.contains("David"));
        Assert.assertTrue(principals.contains("Marry"));
    }

    @Test
    public void logout_normalUser_logoutSucceed() {
        SecurityManager manager = getSecurityManager(context -> true);
        String sessionId = StringUtils.uuidNoHyphen();
        login(Arrays.asList(getToken("David", "123456"), getToken("Marry", "abcde")), sessionId, manager);
        SecuritySession session = manager.getSession(sessionId);
        Assert.assertNotNull(session);
        Assert.assertEquals(sessionId, session.getId());
        manager.logout(session);
        session = manager.getSession(sessionId);
        Assert.assertNull(session);
    }

    @Test
    public void getContext_userHasBeenLoggedin_getSucceed() {
        SecurityManager manager = getSecurityManager(context -> true);
        String sessionId = StringUtils.uuidNoHyphen();
        Assert.assertNull(DefaultLoginSecurityManager.getContext());
        Subject subject = login(Arrays.asList(getToken("David", "123456"),
                getToken("Marry", "abcde")), sessionId, manager);
        Subject subject1 = DefaultLoginSecurityManager.getContext();
        Assert.assertEquals(subject, subject1);
    }

    @Test
    public void isPermitted_securityManager_returnTrue() throws AuthenticationException {
        Assert.assertTrue(isPermitted(context -> true,
                new FirstTestAuthorizer(true, false, false),
                new SecondTestAuthorizer(false, false, false)));
    }

    @Test
    public void isPermitted_securityManager_returnFalse() throws AuthenticationException {
        Assert.assertFalse(isPermitted(context -> false,
                new FirstTestAuthorizer(true, false, false),
                new SecondTestAuthorizer(false, false, false)));
    }

    @Test
    public void isPermitted_privilegedMode_returnTrue() throws AuthenticationException {
        Assert.assertTrue(isPermitted(new DefaultPermissionStrategy(),
                new FirstTestAuthorizer(true, false, true),
                new SecondTestAuthorizer(false, false, false)));
    }

    @Test
    public void isPermitted_allAuthorizerDenied_returnFalse() throws AuthenticationException {
        Assert.assertFalse(isPermitted(new DefaultPermissionStrategy(),
                new FirstTestAuthorizer(false, false, false),
                new SecondTestAuthorizer(false, false, false)));
    }

    @Test
    public void isPermitted_allAuthorizerGranted_returnTrue() throws AuthenticationException {
        Assert.assertTrue(isPermitted(new DefaultPermissionStrategy(),
                new FirstTestAuthorizer(true, false, false),
                new SecondTestAuthorizer(true, false, false)));
    }

    @Test
    public void isPermitted_noUserLogin_accessDenied() {
        DefaultLoginSecurityManager manager = getSecurityManager(context -> true);
        thrown.expectMessage("Subject is null, not login");
        thrown.expect(AccessDeniedException.class);
        manager.isPermitted(Arrays.asList(new AllPermission(), new AllPermission()));
    }

    @Test
    public void checkPermission_normal_noExpThrown() throws AuthenticationException {
        checkPermission(context -> true,
                new FirstTestAuthorizer(true, false, false), new SecondTestAuthorizer(false, false, false));
    }

    @Test
    public void checkPermission_normal_accessDenied() throws AuthenticationException {
        thrown.expect(AccessDeniedException.class);
        thrown.expectMessage("Access Denied");
        checkPermission(context -> false,
                new FirstTestAuthorizer(true, false, false), new SecondTestAuthorizer(false, false, false));
    }

    @Test
    public void checkPermission_privileged_pass() throws AuthenticationException {
        checkPermission(new DefaultPermissionStrategy(), new FirstTestAuthorizer(true, false, true),
                new SecondTestAuthorizer(false, false, false));
    }

    @Test
    public void checkPermission_allAuthorizerDenied_accessDenied() throws AuthenticationException {
        thrown.expect(AccessDeniedException.class);
        thrown.expectMessage("Access Denied");
        checkPermission(new DefaultPermissionStrategy(), new FirstTestAuthorizer(false, false, false),
                new SecondTestAuthorizer(false, false, false));
    }

    @Test
    public void checkPermission_allAuthorizerGranted_pass() throws AuthenticationException {
        checkPermission(new DefaultPermissionStrategy(),
                new FirstTestAuthorizer(true, false, false), new SecondTestAuthorizer(true, false, false));
    }

    @Test
    public void checkPermission_noUserLoggedin_accessDenied() {
        DefaultLoginSecurityManager manager = getSecurityManager(context -> true);
        thrown.expectMessage("Subject is null, not login");
        thrown.expect(AccessDeniedException.class);
        manager.checkPermission(Arrays.asList(new AllPermission(), new AllPermission()));
    }

    @Test
    public void decide_noUserLoggedin_accessDenied() {
        DefaultLoginSecurityManager manager = getSecurityManager(context -> true);
        thrown.expectMessage("Failed to get the subject from the cache");
        thrown.expect(AccessDeniedException.class);
        manager.decide(new SecurityResource() {
            @Override
            public String resourceId() {
                return StringUtils.uuidNoHyphen();
            }

            @Override
            public String resourceType() {
                return "CONNECTION";
            }
        });
    }

    @Test
    public void decide_withoutContext_decideSucceed() {
        DefaultLoginSecurityManager manager = getSecurityManager(context -> true);
        login(Arrays.asList(getToken("David", "123456"), getToken("Marry", "abcde")),
                StringUtils.uuidNoHyphen(), manager);
        String id = StringUtils.uuidNoHyphen();
        Object returnValue = manager.decide(new SecurityResource() {
            @Override
            public String resourceId() {
                return id;
            }

            @Override
            public String resourceType() {
                return "CONNECTION";
            }
        });
        Assert.assertTrue(returnValue instanceof SecurityResource);
        Assert.assertEquals(((SecurityResource) returnValue).resourceType(), "CONNECTION");
        Assert.assertEquals(((SecurityResource) returnValue).resourceId(), id);
    }

    @Test
    public void decide_withContext_decideSucceed() {
        DefaultLoginSecurityManager manager = getSecurityManager(context -> true);
        checkPermission(context -> true,
                new FirstTestAuthorizer(true, false, false),
                new SecondTestAuthorizer(false, false, false));
        String id = StringUtils.uuidNoHyphen();
        Object returnValue = manager.decide(new SecurityResource() {
            @Override
            public String resourceId() {
                return id;
            }

            @Override
            public String resourceType() {
                return "CONNECTION";
            }
        });
        Assert.assertTrue(returnValue instanceof SecurityResource);
        Assert.assertEquals(((SecurityResource) returnValue).resourceType(), "CONNECTION");
        Assert.assertEquals(((SecurityResource) returnValue).resourceId(), id);
    }

    @Test
    public void decide_wrongUser_accesDenied() {
        DefaultLoginSecurityManager manager = getSecurityManager(e -> true, (principal, returnValue, context) -> {
            throw new AccessDeniedException();
        }, new FirstTestAuthorizer(true, false, false), new SecondTestAuthorizer(false, false, false));
        login(Arrays.asList(getToken("David", "123456"), getToken("Marry", "abcde")),
                StringUtils.uuidNoHyphen(), manager);
        String id = StringUtils.uuidNoHyphen();
        thrown.expectMessage("Access Denied");
        thrown.expect(AccessDeniedException.class);
        manager.decide(new SecurityResource() {
            @Override
            public String resourceId() {
                return id;
            }

            @Override
            public String resourceType() {
                return "CONNECTION";
            }
        });
    }

    private DefaultLoginSecurityManager getSecurityManager(PermissionStrategy strategy) {
        return getSecurityManager(strategy, new FirstTestAuthorizer(true, false, false),
                new SecondTestAuthorizer(false, false, false));
    }

    private DefaultLoginSecurityManager getSecurityManager(PermissionStrategy strategy, Authorizer... authorizers) {
        DefaultAuthenticatorManager authenticatorManager = new DefaultAuthenticatorManager(
                Arrays.asList(new EmptyAuthenticator(), new EmptyAuthenticator()));
        DefaultAuthorizerManager authorizerManager = new DefaultAuthorizerManager(Arrays.asList(authorizers));
        LoginSecurityManagerConfig config = LoginSecurityManagerConfig.builder()
                .authenticatorManager(authenticatorManager)
                .authorizerManager(authorizerManager)
                .permissionProvider(new TestPermissionProvider())
                .permissionStrategy(strategy)
                .returnValueProvider(new TempReturnValueProvider())
                .sessionManager(new DefaultSecuritySessionManager(
                        new DefaultSecuritySessionFactory(), new InMemorySecuritySessionRepository()))
                .build();
        return new DefaultLoginSecurityManager(config);
    }

    private DefaultLoginSecurityManager getSecurityManager(PermissionStrategy strategy,
            ReturnValueProvider provider, Authorizer... authorizers) {
        DefaultAuthenticatorManager authenticatorManager = new DefaultAuthenticatorManager(
                Arrays.asList(new EmptyAuthenticator(), new EmptyAuthenticator()));
        DefaultAuthorizerManager authorizerManager = new DefaultAuthorizerManager(Arrays.asList(authorizers));
        LoginSecurityManagerConfig config = LoginSecurityManagerConfig.builder()
                .authenticatorManager(authenticatorManager)
                .authorizerManager(authorizerManager)
                .permissionProvider(new TestPermissionProvider())
                .permissionStrategy(strategy)
                .returnValueProvider(provider)
                .sessionManager(
                        new DefaultSecuritySessionManager(new DefaultSecuritySessionFactory(),
                                new InMemorySecuritySessionRepository()))
                .build();
        return new DefaultLoginSecurityManager(config);
    }

    private BaseAuthenticationToken<? extends Principal, ?> getToken(String username, String password) {
        return new BasicAuthenticationToken(username, password);
    }

    private Subject login(Collection<BaseAuthenticationToken<? extends Principal, ?>> tokens, String sessionId,
            SecurityManager manager) throws AuthenticationException {
        return manager.login(tokens, new DelegateSessionManager() {
            @Override
            public SecuritySession startSession() {
                Map<String, Object> context = new HashMap<>();
                context.putIfAbsent(SecurityConstants.CONTEXT_SESSION_ID_KEY, sessionId);
                return manager.start(context);
            }

            @Override
            public SecuritySession getSession() {
                return manager.getSession(sessionId);
            }
        });
    }

    private boolean isPermitted(PermissionStrategy strategy, Authorizer... authorizers) throws AuthenticationException {
        DefaultLoginSecurityManager manager = getSecurityManager(strategy, authorizers);
        String sessionId = StringUtils.uuidNoHyphen();
        Assert.assertNull(DefaultLoginSecurityManager.getContext());
        login(Arrays.asList(getToken("David", "123456"), getToken("Marry", "abcde")), sessionId, manager);
        boolean result = manager.isPermitted(Arrays.asList(new AllPermission(), new AllPermission()));
        try {
            manager.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    private void checkPermission(PermissionStrategy strategy, Authorizer... authorizers)
            throws AuthenticationException {
        DefaultLoginSecurityManager manager = getSecurityManager(strategy, authorizers);
        String sessionId = StringUtils.uuidNoHyphen();
        Assert.assertNull(DefaultLoginSecurityManager.getContext());
        login(Arrays.asList(getToken("David", "123456"), getToken("Marry", "abcde")), sessionId, manager);
        manager.checkPermission(Arrays.asList(new AllPermission(), new AllPermission()));
        try {
            manager.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class TempReturnValueProvider implements ReturnValueProvider {
        @Override
        public Object decide(Subject principal, Object returnValue,
                SecurityContext context) throws AccessDeniedException {
            return returnValue;
        }
    }

}

