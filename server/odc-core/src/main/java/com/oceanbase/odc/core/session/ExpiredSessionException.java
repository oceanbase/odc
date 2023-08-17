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
package com.oceanbase.odc.core.session;

import java.util.concurrent.TimeUnit;

import org.springframework.http.HttpStatus;

import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.exception.HttpException;

/**
 * Database session expired exception
 *
 * @author yh263208
 * @date 2021-11-15 16:34
 * @since ODC_release_3.2.2
 * @see HttpException
 */
public class ExpiredSessionException extends HttpException {
    /**
     * Exception constructor
     *
     * @param session invalid session
     */
    public ExpiredSessionException(ConnectionSession session) {
        super(ErrorCodes.ConnectionExpired, new Object[] {},
                "Session: " + session.getId() + " invalid, lastAccessTime: " + session.getLastAccessTime().toString()
                        + ", timeout: " + TimeUnit.SECONDS.convert(session.getTimeoutMillis(), TimeUnit.MILLISECONDS)
                        + " s");
    }

    /**
     * Default Exception constructor
     */
    public ExpiredSessionException() {
        super(ErrorCodes.ConnectionExpired, new Object[] {}, "Invalid Session");
    }

    /**
     * Exception constructor
     *
     * @param cause The original exception throwing class
     */
    public ExpiredSessionException(Throwable cause) {
        super(ErrorCodes.ConnectionExpired, new Object[] {}, "Invalid Session", cause);
        this.initCause(cause);
    }

    @Override
    public HttpStatus httpStatus() {
        return HttpStatus.NOT_FOUND;
    }

}
