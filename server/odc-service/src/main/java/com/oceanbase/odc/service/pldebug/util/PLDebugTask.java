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
package com.oceanbase.odc.service.pldebug.util;

import java.util.concurrent.Callable;

import org.springframework.jdbc.core.JdbcOperations;

import com.oceanbase.odc.service.pldebug.model.PLDebugResult;
import com.oceanbase.odc.service.pldebug.model.StartPLDebugReq;
import com.oceanbase.odc.service.pldebug.session.DebuggeeSession;
import com.oceanbase.tools.dbbrowser.model.DBFunction;
import com.oceanbase.tools.dbbrowser.model.DBProcedure;

import lombok.extern.slf4j.Slf4j;

/**
 * @author wenniu.ly
 * @date 2021/11/10
 */

@Slf4j
public class PLDebugTask implements Callable<PLDebugResult> {
    private final String debugId;
    private final DebuggeeSession debuggeeSession;
    private final DBProcedure procedure;
    private final String anonymousBlock;
    private final DBFunction function;

    public PLDebugTask(StartPLDebugReq req, String debugId, DebuggeeSession debuggeeSession) {
        this.anonymousBlock = req.getAnonymousBlock();
        if (req.getFunction() != null) {
            this.function = req.getFunction();
        } else {
            this.function = null;
        }
        if (req.getProcedure() != null) {
            this.procedure = req.getProcedure();
        } else {
            this.procedure = null;
        }
        this.debugId = debugId;
        this.debuggeeSession = debuggeeSession;
    }

    @Override
    public PLDebugResult call() {
        log.info("Start to run debug, debugId={}", debugId);
        PLDebugResult result = null;
        JdbcOperations jdbcOperations = debuggeeSession.getJdbcOperations();
        if (anonymousBlock != null) {
            jdbcOperations
                    .execute(PLUtils.getSpecifiedRoute(debuggeeSession.getPlDebugODPSpecifiedRoute()) + anonymousBlock);
            result = new PLDebugResult();
        }
        log.info("Ending debug running, debugId={}", debugId);
        return result;
    }

}
