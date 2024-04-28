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
import java.util.Objects;

import com.oceanbase.odc.core.flow.model.TaskParameters;
import com.oceanbase.odc.service.connection.table.model.Table;
import com.oceanbase.odc.service.permission.database.model.DatabasePermissionType;
import com.oceanbase.odc.service.permission.project.ApplyProjectParameter.ApplyProject;

import lombok.Data;

/**
 * ClassName: ApplyTableParameter Package: com.oceanbase.odc.service.permission.table.model
 * Description:
 *
 * @Author: fenghao
 * @Create 2024/3/14 16:30
 * @Version 1.0
 */
@Data
public class ApplyTableParameter implements Serializable, TaskParameters {
    private static final long serialVersionUID = -2482302525012272875L;


    /**
     * Project to be applied for, the
     */
    private ApplyProject project;
    /**
     * Databases to be applied for, required
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

        private Long databaseId;
        private String databaseName;
        private Long dataSourceId;
        private String dataSourceName;
        private Long tableId;
        private String tableName;
        private List<String> tableNames;

        public static ApplyTable from(Table table) {
            ApplyTable applyTable = new ApplyTable();
            if (Objects.isNull(applyTable)) {
                return null;
            }
            applyTable.setTableId(table.getId());
            applyTable.setTableName(table.getName());
            applyTable.setDatabaseId(table.getDatabaseId());
            applyTable.setDatabaseName(table.getDatabaseName());
            applyTable.setDataSourceId(table.getDataSourceId());
            applyTable.setDataSourceName(table.getDataSourceName());
            return applyTable;
        }
    }

}
