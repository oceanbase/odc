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
package com.oceanbase.odc.service.schedule.model;

import java.io.Serializable;
import java.util.Map;

import com.oceanbase.odc.service.connection.logicaldatabase.model.DetailLogicalDatabaseResp;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;

import lombok.Data;

/**
 * @Author: Lebie
 * @Date: 2024/9/3 17:41
 * @Description: []
 */
@Data
public class PublishLogicalDatabaseChangeReq implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long scheduleTaskId;
    private String sqlContent;
    private String delimiter;
    private Long timeoutMillis;
    private DetailLogicalDatabaseResp logicalDatabaseResp;
    private Map<String, ConnectionConfig> schemaName2ConnectionConfig;
}

