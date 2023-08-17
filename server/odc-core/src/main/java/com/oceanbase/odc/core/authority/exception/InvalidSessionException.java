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

import java.util.concurrent.TimeUnit;

import org.apache.http.HttpStatus;

import com.oceanbase.odc.core.authority.session.SecuritySession;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;

/**
 * Session expiration exception, if a session does not exist or has expired, this exception will be
 * thrown
 *
 * @author yh263208
 * @date 2021-07-12 18:06
 * @since ODC_release_3.2.0
 */
public class InvalidSessionException extends Exception implements BaseHttpException {
    /**
     * Exception constructor
     *
     * @param session invalid session
     */
    public InvalidSessionException(SecuritySession session) {
        super("Session: " + session.getId() + " invalid, lastAccessTime: " + session.getLastAccessTime().toString()
                + ", timeout: " + TimeUnit.SECONDS.convert(session.getTimeoutMillis(), TimeUnit.MILLISECONDS)
                + " s");
    }

    /**
     * Default Exception constructor
     */
    public InvalidSessionException() {
        super("Invalid Session");
    }

    /**
     * Exception constructor
     *
     * @param cause The original exception throwing class
     */
    public InvalidSessionException(Throwable cause) {
        super(cause.getMessage());
        this.initCause(cause);
    }

    @Override
    public int httpCode() {
        return HttpStatus.SC_UNAUTHORIZED;
    }

    @Override
    public String getLocalizedMessage() {
        return ErrorCodes.LoginExpired.getLocalizedMessage(null);
    }

}
