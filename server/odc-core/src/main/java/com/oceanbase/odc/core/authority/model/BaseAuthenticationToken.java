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
package com.oceanbase.odc.core.authority.model;

import java.security.Principal;

import lombok.NonNull;

/**
 * If you want to login system, you have to need some tokens to complete this operation.
 *
 * For example:
 *
 * you may need username and password to generate a token to pass the validation of the business
 * system. This object {@link BaseAuthenticationToken} is used to package these information
 *
 * @author yh263208
 * @date 2021-07-12 14:43
 * @since ODC_release_3.2.0
 */
public abstract class BaseAuthenticationToken<T extends Principal, V> implements AuthenticationInfo<T, V> {
    /**
     * Principal object
     */
    private final T principal;
    /**
     * If you want to login a system, you have to get some <code>credentials</code> to verify yourself
     */
    private final V credential;

    public BaseAuthenticationToken(@NonNull T principal, @NonNull V credential) {
        this.principal = principal;
        this.credential = credential;
    }

    @Override
    public T getPrincipal() {
        return this.principal;
    }

    @Override
    public V getCredential() {
        return this.credential;
    }

    @Override
    public String getName() {
        return this.principal == null ? "" : this.principal.getName();
    }

    @Override
    public boolean equals(Object another) {
        if (another == null) {
            return false;
        }
        if (!(another instanceof BaseAuthenticationToken)) {
            return false;
        }
        BaseAuthenticationToken<? extends Principal, ?> that =
                (BaseAuthenticationToken<? extends Principal, ?>) another;
        if ((this.getCredential() == null) && (that.getCredential() != null)) {
            return false;
        }
        if ((this.getCredential() != null) && !this.getCredential().equals(that.getCredential())) {
            return false;
        }
        if (this.getPrincipal() == null && that.getPrincipal() != null) {
            return false;
        }
        return this.getPrincipal() == null || this.getPrincipal().equals(that.getPrincipal());
    }

    @Override
    public int hashCode() {
        int code = 31;
        if (this.getPrincipal() != null) {
            code ^= this.getPrincipal().hashCode();
        }
        if (this.getCredential() != null) {
            code ^= this.getCredential().hashCode();
        }
        return code;
    }

    @Override
    public String toString() {
        return super.toString() + ": "
                + "Principal: " + this.getPrincipal() + "; "
                + "Credentials: [PROTECTED]; ";
    }

}
