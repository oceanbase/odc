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
package com.oceanbase.odc.service.db;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.springframework.jdbc.core.StatementCallback;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.sql.execute.SyncJdbcExecutor;
import com.oceanbase.odc.service.db.model.BatchCompileResp;
import com.oceanbase.odc.service.db.model.BatchCompileStatus;
import com.oceanbase.odc.service.db.model.CompileResult;
import com.oceanbase.odc.service.db.util.OBOracleCompilePLCallBack;
import com.oceanbase.tools.dbbrowser.model.DBPLObjectIdentity;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author wenniu.ly
 * @date 2022/6/13
 */

@Slf4j
public class BatchCompileTaskCallable implements Callable<BatchCompileResp> {

    private final ConnectionSession session;
    @Getter
    private final List<DBPLObjectIdentity> identities;
    @Getter
    private volatile Integer completedCompileCounter = 0;

    public BatchCompileTaskCallable(ConnectionSession session, List<DBPLObjectIdentity> identities) {
        this.session = session;
        this.identities = identities;
    }

    @Override
    public BatchCompileResp call() throws Exception {
        List<CompileResult> results = new ArrayList<>();
        BatchCompileResp resp = new BatchCompileResp();
        resp.setResults(results);
        try {
            SyncJdbcExecutor jdbcExecutor = session.getSyncJdbcExecutor(ConnectionSessionConstants.CONSOLE_DS_KEY);
            for (int i = 0; i < this.identities.size() && !Thread.currentThread().isInterrupted(); i++) {
                DBPLObjectIdentity plIdentity = this.identities.get(i);
                CompileResult result = new CompileResult();
                result.setIdentity(plIdentity);
                StatementCallback<String> callback = new OBOracleCompilePLCallBack(plIdentity, jdbcExecutor);
                try {
                    String warning = jdbcExecutor.execute(callback);
                    if (StringUtils.isEmpty(warning)) {
                        result.setSuccessful(true);
                    } else {
                        result.setSuccessful(false);
                        result.setErrorMessage(warning);
                    }
                } catch (Exception e) {
                    result.setSuccessful(false);
                    result.setErrorMessage(e.getMessage());
                    if (e.getCause() != null) {
                        result.setErrorMessage(e.getCause().getMessage());
                    }
                }
                completedCompileCounter++;
                results.add(result);
            }
            if (Thread.currentThread().isInterrupted()) {
                Thread.interrupted();
                throw new InterruptedException("Batch compile task has been cancelled by user");
            }
        } catch (Exception e) {
            log.error("Error occurs while batch compiling", e);
            if (e instanceof InterruptedException) {
                resp.setStatus(BatchCompileStatus.TERMINATED);
            }
        }
        return resp;
    }

}
