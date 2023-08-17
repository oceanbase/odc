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

/**
 * Token for username and password combination, object to package the username info and password
 * info
 *
 * @author yh263208
 * @date 2021-07-12 15:17
 * @since ODC_release_3.2.0
 */
public class BasicAuthenticationToken extends BaseAuthenticationToken<UsernamePrincipal, String> {
    private static final long serialVersionUID = -4184411334994577411L;

    public BasicAuthenticationToken(String username, String password) {
        super(new UsernamePrincipal(username), password);
    }

}
