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
package com.oceanbase.odc.service.task.base.databasechange;

import java.util.List;

import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.objectstorage.model.ObjectMetadata;

import lombok.Data;

/**
 * @author gaoda.xy
 * @date 2024/1/31 16:55
 */
@Data
public class DatabaseChangeTaskParameters {

    /**
     * The raw parameter json string of the task
     */
    private String parameterJson;
    /**
     * The connection config of target database
     */
    private ConnectionConfig connectionConfig;
    /**
     * The ID of task related flow instance
     */
    private Long flowInstanceId;
    /**
     * The timezone of connection session
     */
    private String sessionTimeZone;
    /**
     * The object storage metadata of the SQL files
     */
    private List<ObjectMetadata> sqlFileObjectMetadatas;
    /**
     * Whether data masking is needed
     */
    private boolean needDataMasking;
    /**
     * Whether the task's timeout period is automatically modified
     */
    private boolean autoModifyTimeout;

}
