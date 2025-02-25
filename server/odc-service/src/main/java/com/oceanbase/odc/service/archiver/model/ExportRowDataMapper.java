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
package com.oceanbase.odc.service.archiver.model;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.metadb.schedule.ScheduleEntity;
import com.oceanbase.odc.service.dlm.model.DataArchiveParameters;
import com.oceanbase.odc.service.dlm.model.DataDeleteParameters;
import com.oceanbase.odc.service.schedule.archiverist.model.ArchiveDatabase;
import com.oceanbase.odc.service.schedule.archiverist.model.DataArchiveScheduleRowData;
import com.oceanbase.odc.service.schedule.archiverist.model.DataDeleteScheduleRowData;
import com.oceanbase.odc.service.schedule.model.TriggerConfig;

@Mapper
public interface ExportRowDataMapper {

    ExportRowDataMapper INSTANCE = Mappers.getMapper(ExportRowDataMapper.class);

    @Mapping(source = "database", target = "database")
    @Mapping(source = "targetDatabase", target = "targetDatabase")
    @Mapping(target = "triggerConfig", expression = "java(fromJson(scheduleEntity))")
    @Mapping(target = "name", source = "scheduleEntity.name")
    DataDeleteScheduleRowData toDataDeleteRowData(ScheduleEntity scheduleEntity, DataDeleteParameters parameters,
            ArchiveDatabase database,
            ArchiveDatabase targetDatabase);

    @Mapping(source = "database", target = "sourceDatabase")
    @Mapping(source = "targetDatabase", target = "targetDataBase")
    @Mapping(target = "triggerConfig", expression = "java(fromJson(scheduleEntity))")
    @Mapping(target = "name", source = "scheduleEntity.name")
    DataArchiveScheduleRowData toDataArchiveRowData(ScheduleEntity scheduleEntity, DataArchiveParameters parameters,
            ArchiveDatabase database,
            ArchiveDatabase targetDatabase);

    default TriggerConfig fromJson(ScheduleEntity scheduleEntity) {
        return JsonUtils.fromJson(scheduleEntity.getTriggerConfigJson(), TriggerConfig.class);
    }
}
