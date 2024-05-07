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
package com.oceanbase.odc.service.permission.table.model;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import com.oceanbase.odc.core.flow.model.TaskParameters;
import com.oceanbase.odc.service.permission.database.model.DatabasePermissionType;
import com.oceanbase.odc.service.permission.project.ApplyProjectParameter.ApplyProject;
import com.oceanbase.odc.service.permission.table.ApplyTablePermissionPreprocessor;

import lombok.Data;

/**
 *
 * @Author: fenghao
 * @Create 2024/3/14 16:30
 * @Version 1.0
 */
@Data
public class ApplyTableParameter implements Serializable, TaskParameters {

    private static final long serialVersionUID = -2482302525012272875L;

    /**
     * Project to be applied for, required
     */
    private ApplyProject project;
    /**
     * Tables to be applied for, required
     */
    private List<ApplyTable> tables;
    /**
     * Permission types to be applied for, required
     */
    private List<DatabasePermissionType> types;
    /**
     * Expiration time, null means no expiration, optional
     */
    private Date expireTime;
    /**
     * Reason for application, required
     */
    private String applyReason;

    @Data
    public static class ApplyTable implements Serializable {

        private static final long serialVersionUID = -8433967513537417701L;

        /**
         * ID of the table to be applied for, required
         */
        private Long tableId;
        /**
         * Following fields are all filled in by the {@link ApplyTablePermissionPreprocessor}
         */
        private String tableName;
        private Long databaseId;
        private String databaseName;
        private Long dataSourceId;
        private String dataSourceName;

    }

}
