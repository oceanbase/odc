/*
 * Copyright (c) 2024 OceanBase.
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

package com.oceanbase.odc.service.schedule.model;

import java.util.List;
import java.util.Set;

import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.service.connection.logicaldatabase.core.model.DataNode;
import com.oceanbase.odc.service.connection.logicaldatabase.core.rewrite.RewriteContext;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;

import lombok.Data;

/**
 * @Author: Lebie
 * @Date: 2024/8/30 16:41
 * @Description: []
 */
@Data
public class LogicalDatabaseChangeParameters implements ScheduleTaskParameters {
    private String sqlContent;
    private String delimiter;
    private Long timeoutMillis;
    private Long databaseId;
    private ConnectType connectType;
    private List<RewriteContext> rewriteContexts;
    private Set<DataNode> allDataNodes;

}