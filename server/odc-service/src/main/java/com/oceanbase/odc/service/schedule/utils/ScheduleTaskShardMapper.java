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
import com.oceanbase.odc.metadb.schedule.ScheduleTaskShardEntity;
import com.oceanbase.odc.service.schedule.model.DataArchiveExecutionDetail;
import com.oceanbase.odc.service.schedule.model.DataArchiveTaskShardParameters;
import com.oceanbase.odc.service.schedule.model.ScheduleTaskShard;

/**
 * @Author：tinker
 * @Date: 2024/5/10 14:27
 * @Descripition:
 */
public class ScheduleTaskShardMapper {

    public static ScheduleTaskShardEntity toEntity(ScheduleTaskShard model) {
        ScheduleTaskShardEntity entity = new ScheduleTaskShardEntity();
        entity.setScheduleTaskId(model.getScheduleTaskId());
        entity.setScheduleTaskType(model.getScheduleTaskType());
        entity.setStartTime(model.getStartTime());
        entity.setEndTime(model.getEndTime());
        entity.setStatus(model.getStatus());
        entity.setExecutionDetail(JsonUtils.toJson(model.getExecutionDetail()));
        entity.setParameters(JsonUtils.toJson(model.getParameters()));
        return entity;
    }

    public static ScheduleTaskShard toModel(ScheduleTaskShardEntity entity) {
        ScheduleTaskShard model = new ScheduleTaskShard();
        model.setScheduleTaskId(entity.getScheduleTaskId());
        model.setEndTime(entity.getEndTime());
        model.setStartTime(entity.getStartTime());
        model.setStatus(entity.getStatus());
        model.setId(entity.getId());
        model.setScheduleTaskType(entity.getScheduleTaskType());
        switch (entity.getScheduleTaskType()) {
            case DATA_DELETE:
            case DATA_ARCHIVE:
            case DATA_ARCHIVE_DELETE:
            case DATA_ARCHIVE_ROLLBACK: {
                model.setParameters(JsonUtils.fromJson(entity.getParameters(),
                        DataArchiveTaskShardParameters.class));
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
