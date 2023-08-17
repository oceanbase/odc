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

import javax.security.auth.Subject;

import com.oceanbase.odc.core.authority.exception.AccessDeniedException;

/**
 * Used to filter the return value after the method is executed, semantically speaking, the return
 * value here all corresponds to the query operation authority
 *
 * @author yh263208
 * @date 2021-07-12 20：52
 * @since ODC_release_3.2.0
 */
public interface ReturnValueProvider {
    /**
     * Judgment method by which to determine whether an operating subject has operating authority for
     * the target resource. If there is no operation permission, the user decides whether to return a
     * null value or throw an {@link AccessDeniedException}
     *
     * @param subject Authentication subject
     * @param returnValue return value for method invocation
     * @param context Authentication context
     * @exception AccessDeniedException exception will be thrown when caller throw it
     */
    Object decide(Subject subject, Object returnValue, SecurityContext context) throws AccessDeniedException;

}
