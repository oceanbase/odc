/*
 * Copyright (c) 2025 OceanBase.
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

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.oceanbase.odc.core.shared.exception.UnsupportedException;
import com.oceanbase.odc.service.common.response.SuccessResponse;
import com.oceanbase.odc.service.schedule.export.model.ImportScheduleTaskView;
import com.oceanbase.odc.service.schedule.export.model.ImportTaskResult;
import com.oceanbase.odc.service.schedule.export.model.ScheduleTaskImportRequest;
import com.oceanbase.odc.service.state.model.StateName;
import com.oceanbase.odc.service.state.model.StatefulRoute;

@RequestMapping("/api/v2/import")
@RestController
public class ImportController {

    @RequestMapping(value = "/startSchedulePreviewTask", method = RequestMethod.POST)
    public SuccessResponse<String> startPreviewImportTask(@RequestBody ScheduleTaskImportRequest request) {
        throw new UnsupportedException();
    }
    /**
     * @param previewId {@link ImportController#startPreviewImportTask}'s return value
     */
    @RequestMapping(value = "/getSchedulePreviewResult", method = RequestMethod.GET)
    @StatefulRoute(stateName = StateName.UUID_STATEFUL_ID, stateIdExpression = "#previewId")
    public SuccessResponse<List<ImportScheduleTaskView>> getPreviewImportTask(String previewId) {
        throw new UnsupportedException();
    }

    @RequestMapping(value = "/startScheduleImportTask", method = RequestMethod.POST)
    public SuccessResponse<String> startImportTask(@RequestBody ScheduleTaskImportRequest request) {
        throw new UnsupportedException();
    }

    /**
     * @param importTaskId {@link ImportController#startImportTask}'s return value
     */
    @RequestMapping(value = "/getScheduleImportResult", method = RequestMethod.GET)
    @StatefulRoute(stateName = StateName.UUID_STATEFUL_ID, stateIdExpression = "#importTaskId")
    public SuccessResponse<List<ImportTaskResult>> getImportResult(String importTaskId) {
        throw new UnsupportedException();
    }

    /**
     * @param importTaskId {@link ImportController#startImportTask}'s return value
     */
    @RequestMapping(value = "/getScheduleImportLog", method = RequestMethod.GET)
    @StatefulRoute(stateName = StateName.UUID_STATEFUL_ID, stateIdExpression = "#importTaskId")
    public SuccessResponse<String> getImportLog(String importTaskId) {
        throw new UnsupportedException();
    }
}
