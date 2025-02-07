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
package com.oceanbase.odc.service.loaddata.model;

import java.util.List;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.objectstorage.cloud.model.ObjectStorageConfiguration;
import com.oceanbase.odc.service.schedule.model.ScheduleTaskParameters;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 导入作业的配置信息。
 *
 * @author xien.sxe
 * @date 2024/3/4
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
public class LoadDataParameters implements ScheduleTaskParameters {

    /**
     * 数据加载资源对象，包含数据源的相关配置。
     */
    @NotNull(message = "\"resource\" cannot be null")
    private LoadDataResource resource;

    /**
     * 数据库实例凭证，包含访问数据库实例所需的认证信息。
     */
    private DataBaseInstanceCredential credential;

    private Long datasourceId;

    @NotNull(message = "'databaseId' cannot be null")
    private Long databaseId;

    /**
     * 目标数据库名称。
     */
    @NotBlank(message = "\"database\" cannot be blank")
    private String database;

    /**
     * 目标数据表名称。当导入 sample-data 时可以不填
     */
    private String table;

    /**
     * 数据列预览列表，包含要加载数据的列的信息。当导入 sample-data 时可以不填
     */
    private List<ColumnPreview> columns;

    /**
     * 标识是否为新表，若为新表，则在加载数据时可能需要创建表。
     */
    private boolean newTable;

    /**
     * 加载数据时遇到错误是否立即停止。
     */
    @NotNull(message = "\"stopWhenError\" cannot be null")
    private Boolean stopWhenError;

    /**
     * internal usage
     */
    private transient ObjectStorageConfiguration privateObjectStorageConfig;
    private transient ConnectionConfig connectionConfig;
    private String creator;
    private String dssDatasourceName;
}
