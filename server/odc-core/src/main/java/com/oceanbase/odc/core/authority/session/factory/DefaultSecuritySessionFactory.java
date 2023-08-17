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
package com.oceanbase.odc.core.authority.session.factory;

import java.util.Map;

import com.oceanbase.odc.core.authority.session.DefaultSecuritySession;
import com.oceanbase.odc.core.authority.session.SecuritySession;
import com.oceanbase.odc.core.authority.util.SecurityConstants;

import lombok.NonNull;

/**
 * Implement for {@link SecuritySessionFactory}.
 *
 * {@link DefaultSecuritySessionFactory} can be used to create {@link DefaultSecuritySession} object
 *
 * @author yh263208
 * @date 2021-07-13 17:59
 * @see SecuritySessionFactory
 * @since ODC_release_3.2.0
 */
public class DefaultSecuritySessionFactory implements SecuritySessionFactory {

    @Override
    public SecuritySession createSession(@NonNull Map<String, Object> initParameter) {
        Object timeout = initParameter.get(SecurityConstants.CONTEXT_SESSION_TIMEOUT_KEY);
        Object host = initParameter.getOrDefault(SecurityConstants.CONTEXT_SESSION_HOST_KEY, "0.0.0.0");
        Object sessionId = initParameter.get(SecurityConstants.CONTEXT_SESSION_ID_KEY);
        if (timeout == null) {
            throw new IllegalArgumentException("Timeout can not be null");
        }
        return new DefaultSecuritySession(host.toString(), Long.parseLong(timeout.toString()),
                sessionId == null ? null : sessionId.toString());
    }

}
