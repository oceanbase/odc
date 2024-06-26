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

import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.oceanbase.odc.core.shared.exception.UnsupportedException;
import com.oceanbase.odc.service.common.response.ListResponse;
import com.oceanbase.odc.service.common.response.PaginatedResponse;
import com.oceanbase.odc.service.common.response.Responses;
import com.oceanbase.odc.service.common.response.SuccessResponse;
import com.oceanbase.odc.service.dlm.DlmLimiterService;
import com.oceanbase.odc.service.dlm.model.RateLimitConfiguration;
import com.oceanbase.odc.service.schedule.ScheduleService;
import com.oceanbase.odc.service.schedule.model.CreateScheduleReq;
import com.oceanbase.odc.service.schedule.model.QueryScheduleParams;
import com.oceanbase.odc.service.schedule.model.ScheduleChangeLog;
import com.oceanbase.odc.service.schedule.model.ScheduleDetailResp;
import com.oceanbase.odc.service.schedule.model.ScheduleListResp;
import com.oceanbase.odc.service.schedule.model.ScheduleStatus;
import com.oceanbase.odc.service.schedule.model.ScheduleTaskDetailResp;
import com.oceanbase.odc.service.schedule.model.ScheduleTaskListResp;
import com.oceanbase.odc.service.schedule.model.ScheduleType;
import com.oceanbase.odc.service.schedule.model.UpdateScheduleReq;
import com.oceanbase.odc.service.task.model.OdcTaskLogLevel;

import io.swagger.annotations.ApiOperation;

/**
 * @Author：tinker
 * @Date: 2022/11/22 14:27
 * @Descripition:
 */

@RestController
@RequestMapping("/api/v2/schedule")
public class ScheduleController {
    @Autowired
    private ScheduleService scheduleService;

    @Autowired
    private DlmLimiterService dlmLimiterService;


    // change log

    @RequestMapping(value = "/schedules/{id:[\\d]+}/changes", method = RequestMethod.GET)
    public ListResponse<ScheduleChangeLog> listChangeLog(@PathVariable Long id) {
        throw new UnsupportedException();
    }

    @RequestMapping(value = "/schedules/{id:[\\d]+}/changes/{scheduleChangeLogId:[\\d]+}", method = RequestMethod.GET)
    public SuccessResponse<ScheduleChangeLog> getChangeLog(@PathVariable Long id,
            @PathVariable Long scheduleChangeLogId) {
        throw new UnsupportedException();
    }

    // schedule task

    @RequestMapping(value = "/schedules/{scheduleId:[\\d]+}/tasks/{taskId:[\\d]+}/executions/latest/terminate",
            method = RequestMethod.POST)
    public void terminateTask(@PathVariable Long scheduleId, @PathVariable Long taskId) {
        throw new UnsupportedException();
    }

    @RequestMapping(value = "/schedules/{scheduleId:[\\d]+}/tasks/{taskId:[\\d]+}/executions/latest/stop",
            method = RequestMethod.POST)
    public SuccessResponse<Boolean> stopTask(@PathVariable Long scheduleId, @PathVariable Long taskId) {
        throw new UnsupportedException();

    }

    @ApiOperation(value = "StartTask", notes = "启动任务")
    @RequestMapping(value = "/schedules/{scheduleId:[\\d]+}/tasks/{taskId:[\\d]+}/executions/latest/start",
            method = RequestMethod.POST)
    public SuccessResponse<Boolean> startTask(@PathVariable Long scheduleId, @PathVariable Long taskId) {
        throw new UnsupportedException();

    }

    @RequestMapping(value = "/schedules/{scheduleId:[\\d]+}/tasks/{taskId:[\\d]+}/executions/latest/rollback",
            method = RequestMethod.POST)
    public SuccessResponse<Boolean> rollbackTask(@PathVariable Long scheduleId, @PathVariable Long taskId) {
        throw new UnsupportedException();

    }

    @RequestMapping(value = "/schedules/{scheduleId:[\\d]+}/tasks/{taskId:[\\d]+}/executions/latest/log",
            method = RequestMethod.GET)
    public SuccessResponse<String> getTaskLog(@PathVariable Long scheduleId, @PathVariable Long taskId,
            @RequestParam OdcTaskLogLevel logType) {
        throw new UnsupportedException();
    }


    @RequestMapping(value = "/schedules/{scheduleId:[\\d]+}/tasks/{taskId:[\\d]+}/", method = RequestMethod.GET)
    public SuccessResponse<ScheduleTaskDetailResp> detailScheduleTask(@PathVariable Long scheduleId,
            @PathVariable Long taskId) {
        throw new UnsupportedException();
    }

    @RequestMapping(value = "/schedules/{scheduleId:[\\d]+}/tasks", method = RequestMethod.GET)
    public PaginatedResponse<ScheduleTaskListResp> listTask(
            @PageableDefault(size = Integer.MAX_VALUE, sort = {"id"}, direction = Direction.DESC) Pageable pageable,
            @PathVariable Long scheduleId) {
        throw new UnsupportedException();
    }

    // schedule

    @RequestMapping(value = "/schedules/{id:[\\d]+}/terminate", method = RequestMethod.POST)
    public SuccessResponse<Boolean> terminateSchedule(@PathVariable("id") Long id) {
        throw new UnsupportedException();

    }

    @RequestMapping(value = "/schedules/{id:[\\d]+}/pause", method = RequestMethod.POST)
    public SuccessResponse<Boolean> pauseSchedule(@PathVariable Long id) {
        throw new UnsupportedException();

    }

    @RequestMapping(value = "/schedules/{id:[\\d]+}/resume", method = RequestMethod.POST)
    public SuccessResponse<Boolean> resumeSchedule(@PathVariable Long id) {
        throw new UnsupportedException();

    }

    @RequestMapping(value = "/schedules/{id:[\\d]+}", method = RequestMethod.PUT)
    public SuccessResponse<Boolean> updateSchedule(@PathVariable Long id, @RequestBody UpdateScheduleReq req) {
        throw new UnsupportedException();

    }

    @RequestMapping(value = "/schedules", method = RequestMethod.POST)
    public SuccessResponse<Boolean> createSchedule(@RequestBody CreateScheduleReq req) {
        throw new UnsupportedException();

    }


    @RequestMapping(value = "/schedules", method = RequestMethod.GET)
    public PaginatedResponse<ScheduleListResp> list(
            @PageableDefault(size = Integer.MAX_VALUE, sort = {"id"}, direction = Direction.DESC) Pageable pageable,
            @RequestParam(required = false, name = "connectionId") List<Long> connectionIds,
            @RequestParam(required = false, name = "databaseName") String databaseName,
            @RequestParam(required = false, name = "tenantId") String tenantId,
            @RequestParam(required = false, name = "clusterId") String clusterId,
            @RequestParam(required = false, name = "id") Long id,
            @RequestParam(required = false, name = "status") List<ScheduleStatus> status,
            @RequestParam(required = false, name = "type") ScheduleType type,
            @RequestParam(required = false, name = "startTime") Date startTime,
            @RequestParam(required = false, name = "endTime") Date endTime,
            @RequestParam(required = false, name = "creator") String creator,
            @RequestParam(required = false, name = "projectId") Long projectId) {

        QueryScheduleParams req = QueryScheduleParams.builder()
                .id(id)
                .connectionIds(connectionIds)
                .databaseName(databaseName)
                .tenantId(tenantId)
                .clusterId(clusterId)
                .statuses(status)
                .type(type)
                .startTime(startTime)
                .endTime(endTime)
                .creator(creator)
                .projectId(projectId)
                .build();
        throw new UnsupportedException();

    }

    @RequestMapping(value = "/schedules/{id:[\\d]+}", method = RequestMethod.GET)
    public SuccessResponse<ScheduleDetailResp> detailSchedule(@PathVariable Long id) {
        throw new UnsupportedException();
    }

    @RequestMapping(value = "/schedules/{id:[\\d]+}/dlmRateLimitConfiguration", method = RequestMethod.PUT)
    public SuccessResponse<RateLimitConfiguration> updateLimiterConfig(@PathVariable Long id,
            @RequestBody RateLimitConfiguration limiterConfig) {
        return Responses.single(dlmLimiterService.updateByOrderId(id, limiterConfig));
    }

}
