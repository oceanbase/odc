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
package com.oceanbase.odc.service.schedule;

import java.util.List;
import java.util.Set;

import com.oceanbase.odc.service.exporter.model.ExportProperties;
import com.oceanbase.odc.service.schedule.export.exception.DatabaseNonExistException;
import com.oceanbase.odc.service.schedule.export.model.ExportedDataSource;
import com.oceanbase.odc.service.schedule.export.model.ExportedDatabase;
import com.oceanbase.odc.service.schedule.export.model.ImportScheduleTaskView;
import com.oceanbase.odc.service.schedule.export.model.ScheduleRowPreviewDto;
import com.oceanbase.odc.service.schedule.model.ScheduleType;

public interface ScheduleExportImportFacade {

    Set<ScheduleType> supportedScheduleTypes();

    void adaptProperties(ExportProperties exportProperties);

    void exportDatasourceAdapt(ExportedDataSource exportedDataSource);

    List<ImportScheduleTaskView> preview(ScheduleType scheduleType, Long projectId, ExportProperties exportProperties,
            List<ScheduleRowPreviewDto> dtos);


    Long getOrCreateDatabaseId(Long projectId, ExportedDatabase exportedDatabase) throws DatabaseNonExistException;


}
