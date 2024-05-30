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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Service;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.service.session.model.AsyncExecuteContext;
import com.oceanbase.odc.service.session.model.SqlAsyncExecuteReq;
import com.oceanbase.odc.service.session.model.SqlAsyncExecuteResp;
import com.oceanbase.odc.service.session.model.SqlExecuteResult;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link SqlExecuteInterceptorService}
 *
 * @author yh263208
 * @date 2022-09-06 11:49
 * @since ODC_release_3.4.0
 */
@Service
@Slf4j
@SkipAuthorize("odc internal usage")
public class SqlExecuteInterceptorService {

    @Autowired
    private ListableBeanFactory factory;
    private List<SqlExecuteInterceptor> interceptors;

    @PostConstruct
    public void init() {
        Map<String, SqlExecuteInterceptor> beans = factory.getBeansOfType(SqlExecuteInterceptor.class);
        List<SqlExecuteInterceptor> implementations = new ArrayList<>(beans.values());
        implementations.sort(Comparator.comparingInt(Ordered::getOrder));
        this.interceptors = implementations;
    }

    public boolean preHandle(@NonNull SqlAsyncExecuteReq request, @NonNull SqlAsyncExecuteResp response,
            @NonNull ConnectionSession session, @NonNull AsyncExecuteContext context) throws Exception {
        for (SqlExecuteInterceptor interceptor : interceptors) {
            if (interceptor.preHandle(request, response, session, context)) {
                continue;
            }
            return false;
        }
        return true;
    }

    public void afterCompletion(@NonNull SqlExecuteResult response, @NonNull ConnectionSession session,
            @NonNull AsyncExecuteContext context) throws Exception {
        for (SqlExecuteInterceptor interceptor : interceptors) {
            interceptor.afterCompletion(response, session, context);
        }
    }

}
