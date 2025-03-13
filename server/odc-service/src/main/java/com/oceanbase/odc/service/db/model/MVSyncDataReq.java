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

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import com.oceanbase.tools.dbbrowser.model.DBMViewSyncDataMethod;
import com.oceanbase.tools.dbbrowser.model.DBMViewSyncDataParameter;

import lombok.Data;

/**
 * @description:
 * @author: zijia.cj
 * @date: 2025/3/3 10:55
 * @since: 4.3.4
 */
@Data
public class MVSyncDataReq {
    @NotBlank
    private String databaseName;
    @NotBlank
    private String mvName;
    @NotNull
    DBMViewSyncDataMethod method;
    private int parallelismDegree = 1;

    public DBMViewSyncDataParameter convertToDBMViewSyncDataParameter() {
        DBMViewSyncDataParameter parameter = new DBMViewSyncDataParameter();
        parameter.setDatabaseName(databaseName);
        parameter.setMvName(mvName);
        parameter.setMvSyncDataMethod(method);
        parameter.setParallelismDegree(parallelismDegree);
        return parameter;
    }

}
