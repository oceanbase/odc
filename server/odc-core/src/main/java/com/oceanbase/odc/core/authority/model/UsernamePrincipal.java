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

import java.io.Serializable;
import java.security.Principal;

import lombok.Getter;
import lombok.NonNull;

/**
 * Common principal object for login operation. User need username to identify himself. Username can
 * be treated as a kind of principal. This object is used to package the username for a user in a
 * word
 *
 * @author yh263208
 * @date 2021-07-12 14:58
 * @since ODC_release_3.2.0
 */
public class UsernamePrincipal implements Principal, Serializable {

    private static final long serialVersionUID = -4184480109994577411L;
    @Getter
    private final String username;

    public UsernamePrincipal(@NonNull String username) {
        this.username = username;
    }

    @Override
    public boolean equals(Object another) {
        if (another == null) {
            return false;
        }
        if (this == another) {
            return true;
        }
        if (!(another instanceof UsernamePrincipal)) {
            return false;
        }
        UsernamePrincipal that = (UsernamePrincipal) another;
        return (this.getName().equals(that.getName()));
    }

    @Override
    public String toString() {
        return "UsernamePrincipal: " + this.username;
    }

    @Override
    public int hashCode() {
        return this.username.hashCode();
    }

    @Override
    public String getName() {
        return this.username;
    }

}
