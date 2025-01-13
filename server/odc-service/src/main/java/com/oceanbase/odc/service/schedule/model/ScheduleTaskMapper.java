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
package com.oceanbase.odc.service.schedule.model;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.shared.exception.UnsupportedException;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskEntity;
import com.oceanbase.odc.service.dlm.model.DataArchiveParameters;
import com.oceanbase.odc.service.dlm.model.DataDeleteParameters;
import com.oceanbase.odc.service.loaddata.model.LoadDataParameters;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeParameters;
import com.oceanbase.odc.service.partitionplan.model.PartitionPlanConfig;
import com.oceanbase.odc.service.sqlplan.model.SqlPlanParameters;

/**
 * @Author：tinker
 * @Date: 2023/6/6 20:13
 * @Descripition:
 */

@Mapper
public interface ScheduleTaskMapper {

    ScheduleTaskMapper INSTANCE = Mappers.getMapper(ScheduleTaskMapper.class);

    @Mapping(target = "parameters", source = "entity", qualifiedByName = "entityToParameters")
    ScheduleTask entityToModel(ScheduleTaskEntity entity);

    @Mapping(target = "parametersJson", source = "model", qualifiedByName = "modelToParametersJson")
    ScheduleTaskEntity modelToEntity(ScheduleTask model);

    @Named("entityToParameters")
    default ScheduleTaskParameters entityToParameters(ScheduleTaskEntity entity) {
        switch (ScheduleTaskType.valueOf(entity.getJobGroup())) {
            case DATA_ARCHIVE:
                return JsonUtils.fromJson(entity.getParametersJson(), DataArchiveParameters.class);
            case DATA_DELETE:
                return JsonUtils.fromJson(entity.getParametersJson(), DataDeleteParameters.class);
            case DATA_ARCHIVE_DELETE:
                return JsonUtils.fromJson(entity.getParametersJson(), DataArchiveClearParameters.class);
            case DATA_ARCHIVE_ROLLBACK:
                return JsonUtils.fromJson(entity.getParametersJson(), DataArchiveRollbackParameters.class);
            case ONLINE_SCHEMA_CHANGE_COMPLETE:
                return JsonUtils.fromJson(entity.getParametersJson(), OnlineSchemaChangeParameters.class);
            case SQL_PLAN:
                return JsonUtils.fromJson(entity.getParametersJson(), SqlPlanParameters.class);
            case LOGICAL_DATABASE_CHANGE:
                return JsonUtils.fromJson(entity.getParametersJson(), LogicalDatabaseChangeParameters.class);
            case LOAD_DATA:
                return JsonUtils.fromJson(entity.getParametersJson(), LoadDataParameters.class);
            case PARTITION_PLAN:
                return JsonUtils.fromJson(entity.getParametersJson(), PartitionPlanConfig.class);
            default:
                throw new UnsupportedException();
        }
    }

    @Named("modelToParametersJson")
    default String modelToParametersJson(ScheduleTask model) {
        return JsonUtils.toJson(model.getParameters());
    }
}
