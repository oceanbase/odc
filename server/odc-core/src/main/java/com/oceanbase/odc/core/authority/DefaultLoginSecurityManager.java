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

import java.io.Serializable;
import java.security.Principal;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.security.auth.Subject;

import com.oceanbase.odc.core.authority.auth.SecurityContext;
import com.oceanbase.odc.core.authority.exception.AccessDeniedException;
import com.oceanbase.odc.core.authority.exception.AuthenticationException;
import com.oceanbase.odc.core.authority.exception.InvalidSessionException;
import com.oceanbase.odc.core.authority.model.BaseAuthenticationToken;
import com.oceanbase.odc.core.authority.model.LoginSecurityManagerConfig;
import com.oceanbase.odc.core.authority.model.SecurityResource;
import com.oceanbase.odc.core.authority.permission.Permission;
import com.oceanbase.odc.core.authority.permission.PermissionProvider;
import com.oceanbase.odc.core.authority.session.SecuritySession;
import com.oceanbase.odc.core.authority.session.SecuritySessionManager;
import com.oceanbase.odc.core.authority.util.SecurityConstants;

import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Login security manager, used to process logic related to login
 *
 * @author yh263208
 * @date 2021-07-21 17:24
 * @since ODC_release_3.2.0
 * @see BaseValueFilterSecurityManager
 */
@Slf4j
public class DefaultLoginSecurityManager extends BaseValueFilterSecurityManager {
    /**
     * Local cache to store {@link Subject}
     */
    private static final ThreadLocal<Subject> SUBJECT_CONTEXT = new ThreadLocal<>();
    @Getter
    private final SecuritySessionManager sessionManager;
    private final PermissionProvider permissionProvider;

    public DefaultLoginSecurityManager(@NonNull LoginSecurityManagerConfig config) {
        super(config.getAuthenticatorManager(), config.getAuthorizerManager(),
                config.getPermissionStrategy(), config.getReturnValueProvider());
        this.sessionManager = config.getSessionManager();
        this.permissionProvider = config.getPermissionProvider();
    }

    @Override
    public Subject login(@NonNull Collection<BaseAuthenticationToken<? extends Principal, ?>> tokens,
            @NonNull DelegateSessionManager delegate) throws AuthenticationException {
        SUBJECT_CONTEXT.remove();
        SecuritySession session = delegate.getSession();
        if (session != null) {
            if (log.isDebugEnabled()) {
                log.debug("Subject has been logged in, and old session needs to be logged out, sessionId={}",
                        session.getId());
            }
            logout(session);
        }
        Subject subject = authenticate(tokens);
        session = delegate.startSession();
        if (session == null) {
            throw new AuthenticationException("Failed to start a session");
        }
        try {
            session.setAttribute(SecurityConstants.SECURITY_SESSION_SUBJECT_KEY, subject);
        } catch (InvalidSessionException e) {
            log.warn("Failed to set attribute to session", e);
            throw new AuthenticationException(e);
        }
        SUBJECT_CONTEXT.set(subject);
        if (log.isDebugEnabled()) {
            log.debug("Subject has been logged in successfully, subject={}",
                    subject.getPrincipals().stream().map(Principal::getName).collect(Collectors.joining(",")));
        }
        return subject;
    }

    @Override
    public boolean isPermitted(Collection<Permission> permissions) {
        return isPermitted(getContext(), permissions);
    }

    /**
     * Permission verification interface, used to verify whether the user has the right to operate on a
     * certain resource
     *
     * @param permissions permission collection
     * @exception AccessDeniedException exception will be thrown when authentication failed
     */
    @Override
    public void checkPermission(Collection<Permission> permissions) throws AccessDeniedException {
        checkPermission(getContext(), permissions);
    }

    /**
     * Judgment method by which to determine whether an operating subject has operating authority for
     * the target resource. If there is no operation permission, the user decides whether to return a
     * null value or throw an {@link AccessDeniedException}
     *
     * @param returnValue return value for method invocation
     * @exception AccessDeniedException exception will be thrown when caller throw it
     */
    @Override
    public Object decide(Object returnValue) throws AccessDeniedException {
        Subject subject = getContext();
        SecurityContext context = getSecurityContext();
        if (subject == null) {
            throw new AccessDeniedException(new AuthenticationException("Failed to get the subject from the cache"));
        }
        if (context == null) {
            try {
                context = new SecurityContext(subject);
            } catch (AuthenticationException e) {
                log.warn("Failed to init a SecurityContext", e);
            }
        }
        Set<Principal> principalSet = subject.getPrincipals();
        if (principalSet == null) {
            throw new AccessDeniedException(new AuthenticationException("No principal exists"));
        }
        return decide(subject, returnValue, context);
    }

    /**
     * Start a session and use a keyword to persist the session into a buffer
     *
     * @param context context for {@link SecuritySession} creation
     * @return session object
     */
    @Override
    public SecuritySession start(Map<String, Object> context) {
        return this.sessionManager.start(context);
    }

    /**
     * Get a session from the session manager
     *
     * @param key Keyword used to store session objects
     * @return session object
     */
    @Override
    public SecuritySession getSession(Serializable key) {
        SecuritySession session = this.sessionManager.getSession(key);
        if (session == null) {
            return null;
        }
        try {
            Subject subject = (Subject) session.getAttribute(SecurityConstants.SECURITY_SESSION_SUBJECT_KEY);
            setContext(subject);
        } catch (InvalidSessionException e) {
            log.warn("Failed to get subject from the session, sessionId={}, sessionHost={}", session.getId(),
                    session.getHost(), e);
        }
        return session;
    }

    public static Subject getContext() {
        return SUBJECT_CONTEXT.get();
    }

    public static Subject setContext(@NonNull Subject subject) {
        SUBJECT_CONTEXT.remove();
        SUBJECT_CONTEXT.set(subject);
        return subject;
    }

    public static void removeContext() {
        SUBJECT_CONTEXT.remove();
    }

    @Override
    public void close() throws Exception {
        Exception thrown = null;
        try {
            super.close();
        } catch (Exception e) {
            thrown = e;
        }
        if (this.sessionManager instanceof AutoCloseable) {
            ((AutoCloseable) this.sessionManager).close();
        }
        if (thrown != null) {
            throw thrown;
        }
    }

    @Override
    public Permission getPermissionByActions(SecurityResource resource, Collection<String> actions) {
        return this.permissionProvider.getPermissionByActions(resource, actions);
    }

    @Override
    public Permission getPermissionByResourceRoles(SecurityResource resource, Collection<String> resourceRoles) {
        return this.permissionProvider.getPermissionByResourceRoles(resource, resourceRoles);
    }

    @Override
    public Permission getPermissionByActionsAndResourceRoles(SecurityResource resource, Collection<String> actions,
            Collection<String> resourceRoles) {
        return this.permissionProvider.getPermissionByActionsAndResourceRoles(resource, actions, resourceRoles);
    }

}
