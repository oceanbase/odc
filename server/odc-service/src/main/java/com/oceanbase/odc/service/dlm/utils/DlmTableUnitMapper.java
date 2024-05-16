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
package com.oceanbase.odc.service.dlm.utils;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.metadb.dlm.DlmTableUnitEntity;
import com.oceanbase.odc.service.dlm.model.DlmTableUnit;
import com.oceanbase.odc.service.dlm.model.DlmTableUnitParameters;
import com.oceanbase.odc.service.schedule.model.DlmExecutionDetail;
import com.oceanbase.tools.migrator.common.configure.DataSourceInfo;

/**
 * @Author：tinker
 * @Date: 2024/5/14 10:37
 * @Descripition:
 */

public class DlmTableUnitMapper {

    public static DlmTableUnit entityToModel(DlmTableUnitEntity entity) {
        DlmTableUnit dlmTableUnit = new DlmTableUnit();
        dlmTableUnit.setDlmTableUnitId(entity.getDlmTableUnitId());
        dlmTableUnit.setFireTime(entity.getFireTime());
        dlmTableUnit.setParameters(JsonUtils.fromJson(entity.getParameters(), DlmTableUnitParameters.class));
        dlmTableUnit.setScheduleTaskId(entity.getScheduleTaskId());
        dlmTableUnit.setStatus(entity.getStatus());
        dlmTableUnit.setType(entity.getType());
        dlmTableUnit
                .setSourceDatasourceInfo(JsonUtils.fromJson(entity.getSourceDatasourceInfo(), DataSourceInfo.class));
        dlmTableUnit
                .setTargetDatasourceInfo(JsonUtils.fromJson(entity.getTargetDatasourceInfo(), DataSourceInfo.class));
        dlmTableUnit.setExecutionDetail(JsonUtils.fromJson(entity.getExecutionDetail(), DlmExecutionDetail.class));
        dlmTableUnit.setTableName(entity.getTableName());
        dlmTableUnit.setTargetTableName(entity.getTargetTableName());
        return dlmTableUnit;
    }

    public static DlmTableUnitEntity modelToEntity(DlmTableUnit model) {
        DlmTableUnitEntity entity = new DlmTableUnitEntity();
        entity.setDlmTableUnitId(model.getDlmTableUnitId());
        entity.setFireTime(model.getFireTime());
        entity.setParameters(JsonUtils.toJson(model.getParameters()));
        entity.setScheduleTaskId(model.getScheduleTaskId());
        entity.setStatus(model.getStatus());
        entity.setType(model.getType());
        entity.setSourceDatasourceInfo(JsonUtils.toJson(model.getSourceDatasourceInfo()));
        entity.setTargetDatasourceInfo(JsonUtils.toJson(model.getTargetDatasourceInfo()));
        entity.setExecutionDetail(JsonUtils.toJson(model.getExecutionDetail()));
        entity.setTableName(model.getTableName());
        entity.setTargetTableName(model.getTargetTableName());
        return entity;
    }


}
