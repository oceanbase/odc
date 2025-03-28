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
package com.oceanbase.odc.service.db.model;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import com.oceanbase.tools.dbbrowser.model.DBMViewRefreshParameter;
import com.oceanbase.tools.dbbrowser.model.DBMaterializedViewRefreshMethod;

import lombok.Data;

/**
 * @description:
 * @author: zijia.cj
 * @date: 2025/3/3 10:55
 * @since: 4.3.4
 */
@Data
public class MViewRefreshReq {
    @NotBlank
    private String databaseName;
    @NotBlank
    private String mvName;
    @NotNull
    DBMaterializedViewRefreshMethod method;
    @Min(1)
    private Long parallelismDegree;

    public DBMViewRefreshParameter convertToDBMViewRefreshParameter() {
        DBMViewRefreshParameter parameter = new DBMViewRefreshParameter();
        parameter.setDatabaseName(databaseName);
        parameter.setMvName(mvName);
        parameter.setRefreshMethod(method);
        parameter.setParallelismDegree(parallelismDegree);
        return parameter;
    }

}
