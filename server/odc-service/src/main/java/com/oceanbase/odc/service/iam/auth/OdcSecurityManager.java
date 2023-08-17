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

import java.security.Principal;
import java.util.Collection;
import java.util.Collections;

import javax.security.auth.Subject;

import com.oceanbase.odc.core.authority.DefaultLoginSecurityManager;
import com.oceanbase.odc.core.authority.exception.AuthenticationException;
import com.oceanbase.odc.core.authority.model.BaseAuthenticationToken;
import com.oceanbase.odc.core.authority.model.LoginSecurityManagerConfig;
import com.oceanbase.odc.core.authority.session.SecuritySession;
import com.oceanbase.odc.service.iam.model.User;

import lombok.NonNull;

/**
 * Implements for <code>SecurityManager</code>, just for ODC application
 *
 * @author yh263208
 * @date 2021-08-02 11:08
 * @since ODC-release_3.2.0
 * @see DefaultLoginSecurityManager
 */
public class OdcSecurityManager extends DefaultLoginSecurityManager {

    private final AuthenticationFacade authenticationFacade;

    public OdcSecurityManager(LoginSecurityManagerConfig config,
            @NonNull AuthenticationFacade authenticationFacade) {
        super(config);
        this.authenticationFacade = authenticationFacade;
    }

    @Override
    public Subject login(Collection<BaseAuthenticationToken<? extends Principal, ?>> tokens,
            DelegateSessionManager delegate) throws AuthenticationException {
        User user = authenticationFacade.currentUser();
        OdcSecurityManager that = this;
        return super.login(Collections.singletonList(new OdcAuthenticationToken(user, "")),
                new DelegateSessionManager() {
                    @Override
                    public SecuritySession startSession() {
                        return that.start(null);
                    }

                    @Override
                    public SecuritySession getSession() {
                        return that.getSession(null);
                    }
                });
    }

}
