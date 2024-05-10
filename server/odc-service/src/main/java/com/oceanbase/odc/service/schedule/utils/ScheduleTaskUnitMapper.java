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
package com.oceanbase.odc.service.schedule.utils;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskUnitEntity;
import com.oceanbase.odc.service.schedule.model.DataArchiveExecutionDetail;
import com.oceanbase.odc.service.schedule.model.DataArchiveTaskUnitParameters;
import com.oceanbase.odc.service.schedule.model.ScheduleTaskUnit;

/**
 * @Author：tinker
 * @Date: 2024/5/10 14:27
 * @Descripition:
 */
public class ScheduleTaskUnitMapper {

    public static ScheduleTaskUnitEntity toEntity(ScheduleTaskUnit model) {
        ScheduleTaskUnitEntity entity = new ScheduleTaskUnitEntity();
        entity.setScheduleTaskId(model.getScheduleTaskId());
        entity.setType(model.getType());
        entity.setStartTime(model.getStartTime());
        entity.setEndTime(model.getEndTime());
        entity.setStatus(model.getStatus());
        entity.setExecutionDetail(JsonUtils.toJson(model.getExecutionDetail()));
        entity.setTaskUnitParameters(JsonUtils.toJson(model.getTaskUnitParameters()));
        return entity;
    }

    public static ScheduleTaskUnit toModel(ScheduleTaskUnitEntity entity) {
        ScheduleTaskUnit model = new ScheduleTaskUnit();
        model.setScheduleTaskId(entity.getScheduleTaskId());
        model.setEndTime(entity.getEndTime());
        model.setStartTime(entity.getStartTime());
        model.setStatus(entity.getStatus());
        model.setId(entity.getId());
        model.setType(entity.getType());
        switch (entity.getType()) {
            case DATA_DELETE:
            case DATA_ARCHIVE:
            case DATA_ARCHIVE_DELETE:
            case DATA_ARCHIVE_ROLLBACK: {
                model.setTaskUnitParameters(JsonUtils.fromJson(entity.getTaskUnitParameters(),
                        DataArchiveTaskUnitParameters.class));
                model.setExecutionDetail(
                        JsonUtils.fromJson(entity.getExecutionDetail(), DataArchiveExecutionDetail.class));
                break;
            }
            default:
                break;
        }
        return model;
    }
}
