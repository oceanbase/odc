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

import java.util.List;
import java.util.stream.Collectors;

import com.oceanbase.odc.common.util.TraceStage;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.service.session.model.AsyncExecuteContext;
import com.oceanbase.odc.service.session.model.SqlAsyncExecuteReq;
import com.oceanbase.odc.service.session.model.SqlAsyncExecuteResp;
import com.oceanbase.odc.service.session.model.SqlExecuteResult;

import lombok.NonNull;

public abstract class BaseTimeConsumingInterceptor implements SqlExecuteInterceptor {

    @Override
    public boolean preHandle(@NonNull SqlAsyncExecuteReq request, @NonNull SqlAsyncExecuteResp response,
            @NonNull ConnectionSession session, @NonNull AsyncExecuteContext context) throws Exception {
        List<TraceStage> stageList = response.getSqls().stream()
                .map(v -> v.getSqlTuple().getSqlWatch().start(getExecuteStageName()))
                .collect(Collectors.toList());
        try {
            return doPreHandle(request, response, session, context);
        } finally {
            for (TraceStage stage : stageList) {
                try {
                    stage.close();
                } catch (Exception e) {
                    // eat exception
                }
            }
        }
    }

    @Override
    public void afterCompletion(@NonNull SqlExecuteResult response,
            @NonNull ConnectionSession session, @NonNull AsyncExecuteContext context) throws Exception {
        try (TraceStage stage = response.getSqlTuple().getSqlWatch().start(getExecuteStageName())) {
            doAfterCompletion(response, session, context);
        }
    }

    protected boolean doPreHandle(@NonNull SqlAsyncExecuteReq request, @NonNull SqlAsyncExecuteResp response,
            @NonNull ConnectionSession session, @NonNull AsyncExecuteContext context) throws Exception {
        return true;
    }

    protected void doAfterCompletion(@NonNull SqlExecuteResult response,
            @NonNull ConnectionSession session, @NonNull AsyncExecuteContext context) throws Exception {}

    protected abstract String getExecuteStageName();

}
