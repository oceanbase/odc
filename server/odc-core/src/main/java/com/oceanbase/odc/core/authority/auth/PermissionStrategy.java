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
package com.oceanbase.odc.core.authority.auth;

/**
 * During the authentication process, multiple {@link Authorizer} may have different authentication
 * results for different {@link java.security.Principal}.
 *
 * A strategy is needed to decide how to choose when different {@link java.security.Principal} have
 * different authentication results for different {@link java.security.Principal}. This interface is
 * to solve this problem
 *
 * @author yh263208
 * @date 2021-07-20 17:06
 * @since ODC_release_3.2.0
 */
public interface PermissionStrategy {
    /**
     * Method to decide whether grant the operation
     *
     * @param context {@link SecurityContext}
     * @return checkResult
     */
    boolean decide(SecurityContext context);

}
