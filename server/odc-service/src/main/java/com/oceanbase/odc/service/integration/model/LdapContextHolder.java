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
package com.oceanbase.odc.service.integration.model;

import static com.oceanbase.odc.service.integration.model.SSOIntegrationConfig.parseRegistrationName;

import com.oceanbase.odc.core.shared.Verify;

import lombok.Value;

public final class LdapContextHolder {

    private static final ThreadLocal<LdapContext> PARAMETER_THREAD_LOCAL = new ThreadLocal<>();


    public static void setParameter(LdapContext context) {
        PARAMETER_THREAD_LOCAL.set(context);
    }

    public static String getRegistrationId() {
        return PARAMETER_THREAD_LOCAL.get().getRegistrationId();
    }

    public static LdapContext getContext() {
        return PARAMETER_THREAD_LOCAL.get();
    }

    public static void clear() {
        PARAMETER_THREAD_LOCAL.remove();
    }


    @Value
    public static class LdapContext {
        String registrationId;
        String testId;
        String username;
        String password;

        public boolean isTest() {
            Verify.notBlank(registrationId, "registrationId can't be blank");
            return "test".equals(parseRegistrationName(registrationId));
        }
    }
}
