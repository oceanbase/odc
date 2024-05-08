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

import java.util.Collections;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.sql.execute.model.SqlExecuteStatus;
import com.oceanbase.odc.service.session.model.AsyncExecuteContext;
import com.oceanbase.odc.service.session.model.SqlExecuteResult;
import com.oceanbase.odc.service.session.util.SchemaExtractor;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @author gaoda.xy
 * @date 2024/2/29 16:38
 */
@Slf4j
@Component
public class SwitchDatabaseInterceptor implements SqlExecuteInterceptor {

    @Override
    public void afterCompletion(@NonNull SqlExecuteResult response, @NonNull ConnectionSession session,
            @NonNull AsyncExecuteContext context) throws Exception {
        if (response.getStatus() != SqlExecuteStatus.SUCCESS) {
            return;
        }
        String currentSchema = ConnectionSessionUtil.getCurrentSchema(session);
        Optional<String> switchSchema = SchemaExtractor
                .extractSwitchedSchemaName(Collections.singletonList(response.getSqlTuple()), session.getDialectType());
        if (switchSchema.isPresent() && !Objects.equals(switchSchema.get(), currentSchema)) {
            ConnectionSessionUtil.setCurrentSchema(session, switchSchema.get());
            log.info("Switch current schema of session to {} after executing sql={}, sid={}", switchSchema.get(),
                    response.getExecuteSql(), session.getId());
        }
    }

    @Override
    public int getOrder() {
        return 6;
    }

}
