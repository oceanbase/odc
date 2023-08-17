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
package com.oceanbase.odc.core.authority.exception;

import java.security.Principal;
import java.util.Collection;
import java.util.stream.Collectors;

import org.apache.http.HttpStatus;

import com.oceanbase.odc.core.authority.permission.Permission;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;

/**
 * This non-checked exception will be thrown when the authentication operation for the method fails,
 * indicating that the authentication failed
 *
 * @author yh263208
 * @date 2021-07-12 17:31
 * @since ODC_release_3.2.0
 */
public class AccessDeniedException extends RuntimeException implements BaseHttpException {
    /**
     * Exception constructor
     *
     * @param principal Operating subject
     * @param permission Permission for authentication failure
     */
    public AccessDeniedException(Principal principal, Permission permission) {
        super("Principal: " + principal.getName() + " access denied (" + permission + ")");
    }

    /**
     * Exception constructor
     *
     * @param principal Operating subject
     * @param permissions Permission for authentication failure
     */
    public AccessDeniedException(Principal principal, Collection<Permission> permissions) {
        super("Principal: " + principal.getName() + " access denied ("
                + permissions.stream().map(Object::toString).collect(Collectors.joining(",")) + ")");
    }

    /**
     * Exception constructor
     *
     * @param permissions Permission for authentication failure
     */
    public AccessDeniedException(Collection<Permission> permissions) {
        super("Access Denied (" + permissions.stream().map(Object::toString).collect(Collectors.joining(",")) + ")");
    }

    /**
     * Default Exception constructor
     */
    public AccessDeniedException() {
        super("Access Denied");
    }

    /**
     * Exception constructor
     *
     * @param cause The original exception throwing class
     */
    public AccessDeniedException(Throwable cause) {
        super(cause.getMessage());
        this.initCause(cause);
    }

    @Override
    public int httpCode() {
        return HttpStatus.SC_FORBIDDEN;
    }

    @Override
    public String getLocalizedMessage() {
        return ErrorCodes.AccessDenied.getLocalizedMessage(null);
    }

}
