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
package com.oceanbase.odc.service.flow.task.logicdatabasechange;

import com.oceanbase.odc.service.connection.logicaldatabase.LogicalDatabaseService;
import com.oceanbase.odc.service.flow.task.model.LogicalDatabaseChangePublishReq;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author longpeng.zlp
 * @date 2024/10/10 13:59
 */
@Slf4j
@AllArgsConstructor
public class LogicalDBChangeTerminateProcessor {
    protected LogicalDatabaseService logicalDatabaseService;

    public void process(Long flowInstanceId, LogicalDatabaseChangePublishReq req) {
        try {
            if (req != null && req.getLogicalDatabaseResp() != null) {
                logicalDatabaseService.extractLogicalTablesSkipAuth(req.getLogicalDatabaseResp().getId(),
                        req.getCreatorId());
                log.info("Submit the extract logical tables task succeed, logicalDatabaseId={}, flowInstanceId={}",
                        req.getLogicalDatabaseResp().getId(), flowInstanceId);
            }
        } catch (Exception ex) {
            log.warn("Failed to submit the extract logical tables task, ex=", ex);
        }
    }
}
