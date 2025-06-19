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
package com.oceanbase.odc.server.web.controller.v2;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.oceanbase.odc.service.common.response.Responses;
import com.oceanbase.odc.service.common.response.SuccessResponse;
import com.oceanbase.odc.service.schedule.export.ScheduleExportService;
import com.oceanbase.odc.service.schedule.export.model.FileExportResponse;
import com.oceanbase.odc.service.schedule.export.model.ScheduleExportListView;
import com.oceanbase.odc.service.schedule.export.model.ScheduleTaskExportRequest;
import com.oceanbase.odc.service.state.model.StateName;
import com.oceanbase.odc.service.state.model.StatefulRoute;

@RequestMapping("/api/v2/export")
@RestController
public class ExportController {

    @Autowired
    private ScheduleExportService scheduleExportService;

    @RequestMapping(value = "getExportListView", method = RequestMethod.POST)
    public SuccessResponse<List<ScheduleExportListView>> getExportListView(
            @RequestBody ScheduleTaskExportRequest request) {
        return Responses.success(scheduleExportService.getExportListView(request));
    }

    @RequestMapping(value = "/exportSchedule", method = RequestMethod.POST)
    public SuccessResponse<String> exportScheduleTask2(@RequestBody ScheduleTaskExportRequest request) {
        return Responses.success(scheduleExportService.startExport(request));
    }

    @RequestMapping(value = "/getExportResult", method = RequestMethod.GET)
    @StatefulRoute(stateName = StateName.UUID_STATEFUL_ID, stateIdExpression = "#exportId")
    public SuccessResponse<FileExportResponse> exportScheduleTask(String exportId) {
        return Responses.success(scheduleExportService.getExportResult(exportId));
    }

    @RequestMapping(value = "/getExportLog", method = RequestMethod.GET)
    @StatefulRoute(stateName = StateName.UUID_STATEFUL_ID, stateIdExpression = "#exportId")
    public SuccessResponse<String> getExportLog(String exportId) {
        return Responses.success(scheduleExportService.getExportLog(exportId));
    }

}
