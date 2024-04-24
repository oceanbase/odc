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
package com.oceanbase.odc.service.session.interceptor;

import org.springframework.core.Ordered;

import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.service.session.model.AsyncExecuteContext;
import com.oceanbase.odc.service.session.model.SqlAsyncExecuteReq;
import com.oceanbase.odc.service.session.model.SqlAsyncExecuteResp;
import com.oceanbase.odc.service.session.model.SqlExecuteResult;

import lombok.NonNull;

/**
 * {@link SqlExecuteInterceptor}
 *
 * @author yh263208
 * @date 2022-09-06 10:42
 * @since ODC_release_3.4.0
 */
public interface SqlExecuteInterceptor extends Ordered {
    /**
     * method will be called before sql executed
     *
     * @param request sql to be executed
     * @param response
     * @param context context
     * @param session {@link ConnectionSession}
     * @return whether to execute this sql
     */
    default boolean preHandle(@NonNull SqlAsyncExecuteReq request, @NonNull SqlAsyncExecuteResp response,
            @NonNull ConnectionSession session, @NonNull AsyncExecuteContext context) throws Exception {
        return true;
    }

    default void afterCompletion(@NonNull SqlExecuteResult response,
            @NonNull ConnectionSession session, @NonNull AsyncExecuteContext context) throws Exception {}

}
