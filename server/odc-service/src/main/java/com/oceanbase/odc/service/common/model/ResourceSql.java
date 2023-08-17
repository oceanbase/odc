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
package com.oceanbase.odc.service.common.model;

import lombok.Data;

/**
 * @author kuiseng.zhb
 */
@Data
public class ResourceSql {

    private String sql;
    private String tip;
    private boolean affectMultiRows;
    /**
     * <pre>
     * - traceId while GetExecDetailCommand
     * - sqlId while GetExecSqlExplainCommand
     * - originalTableName while ValidateDdlCommand
     * - charset while startTask(export)
     * - set disableAutocommit while ExecuteSqlOneByOneCommand
     * </pre>
     */
    private String tag;
    /**
     * fileType, optional values: sql/csv/...
     */
    private String type;
    private String desc;
    /**
     * query result set row limitation
     */
    private Integer queryLimit;

    public static ResourceSql ofSql(String sql) {
        ResourceSql resourceSql = new ResourceSql();
        resourceSql.setSql(sql);
        return resourceSql;
    }

}
