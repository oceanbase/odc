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

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

import javax.security.auth.Subject;

import com.oceanbase.odc.core.authority.auth.AuthenticatorManager;
import com.oceanbase.odc.core.authority.exception.AuthenticationException;
import com.oceanbase.odc.core.authority.exception.InvalidSessionException;
import com.oceanbase.odc.core.authority.session.SecuritySession;
import com.oceanbase.odc.core.authority.session.factory.SecuritySessionFactory;
import com.oceanbase.odc.core.authority.session.manager.BaseSecuritySessionManager;
import com.oceanbase.odc.core.authority.util.SecurityConstants;
import com.oceanbase.odc.service.iam.model.User;

import lombok.NonNull;

/**
 * {@link EmptySessionManager} 在 odc 的场景下，安全框架不需要处理会话管理的逻辑，因此这里做一个空实现
 *
 * @author yh263208
 * @date 2022-10-28 18:17
 * @since ODC_release_4.0.1
 */
public class EmptySessionManager extends BaseSecuritySessionManager {

    private final AuthenticationFacade authenticationFacade;
    private final AuthenticatorManager authenticatorManager;

    public EmptySessionManager(@NonNull AuthenticationFacade authenticationFacade,
            @NonNull AuthenticatorManager authenticatorManager, @NonNull SecuritySessionFactory sessionFactory) {
        super(sessionFactory);
        this.authenticatorManager = authenticatorManager;
        this.authenticationFacade = authenticationFacade;
    }

    @Override
    protected SecuritySession doGetSession(Serializable key) {
        SecuritySession session = doStartSession(null);
        // 需要在 session 中将当前的登录用户放进去
        User user = authenticationFacade.currentUser();
        Subject subject =
                authenticatorManager.authenticate(Collections.singletonList(new OdcAuthenticationToken(user, "")));
        try {
            session.setAttribute(SecurityConstants.SECURITY_SESSION_SUBJECT_KEY, subject);
        } catch (InvalidSessionException e) {
            throw new AuthenticationException(e);
        }
        return session;
    }

    @Override
    protected Serializable getSessionId(Serializable key) {
        return null;
    }

    @Override
    protected void doStoreSession(SecuritySession session, Map<String, Object> context) {
        // 空实现不用关注会话的存储
    }

}
