/*
 * Copyright (c) 2025 OceanBase.
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

package com.oceanbase.odc.service.flow.task.model;

import java.util.Map;

import com.oceanbase.odc.core.flow.model.AbstractFlowTaskResult;
import com.oceanbase.odc.service.connection.logicaldatabase.model.DetailLogicalDatabaseResp;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;

/**
 * @author Yizhuo
 * @date 2025/06/04 14:12:07
 */
public class LogicalDatabaseChangeTaskResult extends AbstractFlowTaskResult {

    private static final long                          serialVersionUID = 1L;
    private              Long                          creatorId;
    private              Long                          scheduleTaskId;
    private              String                        sqlContent;
    private String delimiter;
    private              Long                          timeoutMillis;
    private              DetailLogicalDatabaseResp     logicalDatabaseResp;
    private              Map<String, ConnectionConfig> schemaName2ConnectionConfig;

}
