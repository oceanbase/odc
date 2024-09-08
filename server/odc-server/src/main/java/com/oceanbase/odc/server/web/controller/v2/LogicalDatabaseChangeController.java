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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.oceanbase.odc.service.common.response.Responses;
import com.oceanbase.odc.service.common.response.SuccessResponse;
import com.oceanbase.odc.service.connection.logicaldatabase.LogicalDatabaseChangeService;
import com.oceanbase.odc.service.connection.logicaldatabase.model.SchemaChangeRecord;
import com.oceanbase.odc.service.task.exception.JobException;

/**
 * @Author: Lebie
 * @Date: 2024/9/4 12:20
 * @Description: []
 */
@RestController
@RequestMapping("/api/v2/logicaldatabase")
public class LogicalDatabaseChangeController {
    private static final Logger log = LoggerFactory.getLogger(LogicalDatabaseChangeController.class);
    @Autowired
    private LogicalDatabaseChangeService logicalDatabaseChangeService;

    @RequestMapping(value = "/scheduleTasks/{scheduleTaskId:[\\d]+}/records/{recordId:[\\d]+}",
            method = RequestMethod.GET)
    public SuccessResponse<SchemaChangeRecord> detailPhysicalDatabaseChangeTask(@PathVariable Long scheduleTaskId,
            @PathVariable Long recordId) {
        return Responses.success(logicalDatabaseChangeService.detail(scheduleTaskId, recordId));
    }

    @RequestMapping(value = "/scheduleTasks/{scheduleTaskId:[\\d]+}/records/{recordId:[\\d]+}/skipCurrent",
            method = RequestMethod.POST)
    public SuccessResponse<Boolean> skipPhysicalDatabaseChangeTask(@PathVariable Long scheduleTaskId,
            @PathVariable Long recordId) throws InterruptedException, JobException {
        return Responses.success(logicalDatabaseChangeService.skipCurrent(scheduleTaskId, recordId));
    }

    @RequestMapping(value = "/scheduleTasks/{scheduleTaskId:[\\d]+}/records/{recordId:[\\d]+}/terminateCurrent",
            method = RequestMethod.POST)
    public SuccessResponse<Boolean> terminatePhysicalDatabaseChangeTask(@PathVariable Long scheduleTaskId,
            @PathVariable Long recordId) throws InterruptedException, JobException {
        return Responses.success(logicalDatabaseChangeService.terminateCurrent(scheduleTaskId, recordId));
    }
}
