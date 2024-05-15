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
import com.oceanbase.odc.metadb.dlm.DlmJobEntity;
import com.oceanbase.odc.service.dlm.model.DLMJobParameters;
import com.oceanbase.odc.service.dlm.model.DlmJob;
import com.oceanbase.tools.migrator.common.configure.DataSourceInfo;

/**
 * @Author：tinker
 * @Date: 2024/5/14 10:37
 * @Descripition:
 */

public class DlmJobMapper {

    public static DlmJob entityToModel(DlmJobEntity entity) {
        DlmJob dlmJob = new DlmJob();
        dlmJob.setDlmJobId(entity.getDlmJobId());
        dlmJob.setFireTime(entity.getFireTime());
        dlmJob.setParameters(JsonUtils.fromJson(entity.getParameters(), DLMJobParameters.class));
        dlmJob.setScheduleTaskId(entity.getScheduleTaskId());
        dlmJob.setStatus(entity.getStatus());
        dlmJob.setType(entity.getType());
        dlmJob.setSourceDatasourceInfo(JsonUtils.fromJson(entity.getSourceDatasourceInfo(), DataSourceInfo.class));
        dlmJob.setTargetDatasourceInfo(JsonUtils.fromJson(entity.getTargetDatasourceInfo(), DataSourceInfo.class));
        dlmJob.setTableName(entity.getTableName());
        dlmJob.setTargetTableName(entity.getTargetTableName());
        return dlmJob;
    }

    public static DlmJobEntity modelToEntity(DlmJob model) {
        DlmJobEntity entity = new DlmJobEntity();
        entity.setDlmJobId(model.getDlmJobId());
        entity.setFireTime(model.getFireTime());
        entity.setParameters(JsonUtils.toJson(model.getParameters()));
        entity.setScheduleTaskId(model.getScheduleTaskId());
        entity.setStatus(model.getStatus());
        entity.setType(model.getType());
        entity.setSourceDatasourceInfo(JsonUtils.toJson(model.getSourceDatasourceInfo()));
        entity.setTargetDatasourceInfo(JsonUtils.toJson(model.getTargetDatasourceInfo()));
        entity.setTableName(model.getTableName());
        entity.setTargetTableName(model.getTargetTableName());
        return entity;
    }


}
