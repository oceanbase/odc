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

import org.apache.http.HttpStatus;

import com.oceanbase.odc.core.shared.constant.ErrorCodes;

/**
 * This exception will be thrown when Authentication failed, this exception is a checked exception
 *
 * @author yh263208
 * @date 2021-07-12 15:38
 * @since ODC_release_3.2.0
 */
public class AuthenticationException extends RuntimeException implements BaseHttpException {
    /**
     * Exception constructor
     *
     * @param principal principal which authenticated failed
     */
    public AuthenticationException(Principal principal) {
        super("Principal: " + principal.getName() + " authentication failed");
    }

    /**
     * Default Exception constructor
     */
    public AuthenticationException() {
        super("Failed to authenticate a principal");
    }

    /**
     * Default Exception constructor
     */
    public AuthenticationException(String message) {
        super(message);
    }

    /**
     * Exception constructor
     *
     * @param cause The original exception throwing class
     */
    public AuthenticationException(Throwable cause) {
        super(cause.getMessage());
        this.initCause(cause);
    }

    @Override
    public int httpCode() {
        return HttpStatus.SC_UNAUTHORIZED;
    }

    @Override
    public String getLocalizedMessage() {
        return ErrorCodes.AuthNoToken.getLocalizedMessage(null);
    }

}
