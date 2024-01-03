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
package com.oceanbase.odc.service.permissionapply.database;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import com.oceanbase.odc.core.flow.model.TaskParameters;
import com.oceanbase.odc.service.connection.database.model.Database;

import lombok.Data;

/**
 * @author gaoda.xy
 * @date 2024/1/3 13:54
 */
@Data
public class ApplyDatabaseParameter implements Serializable, TaskParameters {

    /**
     * Project ID, required
     */
    private Long projectId;
    /**
     * Databases to be applied for, the front-end only transfer the id field and the back-end will
     * return more field value, required
     */
    private List<Database> databases;
    /**
     * Permission types to be applied for, required
     */
    private List<DatabasePermissionType> permissionTypes;
    /**
     * Expiration time, null means no expiration, optional
     */
    private Date expireTime;
    /**
     * Reason for application, required
     */
    private String applyReason;
    /**
     * Creator ID, filled in by the back-end
     */
    private Long userId;
    /**
     * Project name, filled in by the back-end
     */
    private String projectName;

}
