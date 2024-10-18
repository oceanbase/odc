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
package com.oceanbase.odc.service.task.base.rollback;

import java.io.Serializable;
import java.util.List;

import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.objectstorage.model.ObjectMetadata;
import com.oceanbase.odc.service.rollbackplan.model.RollbackProperties;

import lombok.Data;

/**
 * @author gaoda.xy
 * @date 2024/2/6 10:06
 */
@Data
public class RollbackPlanTaskParameters implements Serializable {

    private static final long serialVersionUID = -798017306200078615L;

    /**
     * The flow instance id of the rollback plan task
     */
    private Long flowInstanceId;
    /**
     * Rollback plan properties
     */
    private RollbackProperties rollbackProperties;
    /**
     * The connection config of target database
     */
    private ConnectionConfig connectionConfig;
    /**
     * The default schema of the task
     */
    private String defaultSchema;
    /**
     * The rollback plan SQL content
     */
    private String sqlContent;
    /**
     * The object storage metadata of the rollback plan SQL file
     */
    private List<ObjectMetadata> sqlFileObjectMetadatas;
    /**
     * The delimiter of the rollback plan SQL
     */
    private String delimiter;

}
