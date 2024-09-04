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
import java.util.Set;

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

import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.core.shared.exception.UnsupportedException;
import com.oceanbase.odc.service.common.response.ListResponse;
import com.oceanbase.odc.service.common.response.PaginatedResponse;
import com.oceanbase.odc.service.common.response.Responses;
import com.oceanbase.odc.service.common.response.SuccessResponse;
import com.oceanbase.odc.service.dlm.model.RateLimitConfiguration;
import com.oceanbase.odc.service.schedule.ScheduleService;
import com.oceanbase.odc.service.schedule.model.CreateScheduleReq;
import com.oceanbase.odc.service.schedule.model.OperationType;
import com.oceanbase.odc.service.schedule.model.QueryScheduleParams;
import com.oceanbase.odc.service.schedule.model.QueryScheduleTaskParams;
import com.oceanbase.odc.service.schedule.model.Schedule;
import com.oceanbase.odc.service.schedule.model.ScheduleChangeLog;
import com.oceanbase.odc.service.schedule.model.ScheduleChangeParams;
import com.oceanbase.odc.service.schedule.model.ScheduleDetailResp;
import com.oceanbase.odc.service.schedule.model.ScheduleOverview;
import com.oceanbase.odc.service.schedule.model.ScheduleStatus;
import com.oceanbase.odc.service.schedule.model.ScheduleTaskDetailResp;
import com.oceanbase.odc.service.schedule.model.ScheduleTaskListOverview;
import com.oceanbase.odc.service.schedule.model.ScheduleTaskOverview;
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

    // change log

    @RequestMapping(value = "/schedules/{id:[\\d]+}/changes", method = RequestMethod.GET)
    public ListResponse<ScheduleChangeLog> listChangeLog(@PathVariable Long id) {
        return Responses.list(scheduleService.listScheduleChangeLog(id));

    }

    @RequestMapping(value = "/schedules/{id:[\\d]+}/changes/{scheduleChangeLogId:[\\d]+}", method = RequestMethod.GET)
    public SuccessResponse<ScheduleChangeLog> getChangeLog(@PathVariable Long id,
            @PathVariable Long scheduleChangeLogId) {
        return Responses.success(scheduleService.getChangeLog(id, scheduleChangeLogId));
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
        scheduleService.stopTask(scheduleId, taskId);
        return Responses.success(Boolean.TRUE);
    }

    @ApiOperation(value = "StartTask", notes = "启动任务")
    @RequestMapping(value = "/schedules/{scheduleId:[\\d]+}/tasks/{taskId:[\\d]+}/start",
            method = RequestMethod.POST)
    public SuccessResponse<Boolean> startTask(@PathVariable Long scheduleId, @PathVariable Long taskId) {
        scheduleService.startTask(scheduleId, taskId);
        return Responses.success(Boolean.TRUE);
    }

    @RequestMapping(value = "/schedules/{scheduleId:[\\d]+}/tasks/{taskId:[\\d]+}/rollback",
            method = RequestMethod.POST)
    public SuccessResponse<Boolean> rollbackTask(@PathVariable Long scheduleId, @PathVariable Long taskId) {
        scheduleService.rollbackTask(scheduleId, taskId);
        return Responses.success(Boolean.TRUE);

    }

    @RequestMapping(value = "/schedules/{scheduleId:[\\d]+}/tasks/{taskId:[\\d]+}/executions/latest/log",
            method = RequestMethod.GET)
    public SuccessResponse<String> getTaskLog(@PathVariable Long scheduleId, @PathVariable Long taskId,
            @RequestParam OdcTaskLogLevel logType) {
        return Responses.success(scheduleService.getLog(scheduleId, taskId, logType));
    }


    @RequestMapping(value = "/schedules/{scheduleId:[\\d]+}/tasks/{taskId:[\\d]+}", method = RequestMethod.GET)
    public SuccessResponse<ScheduleTaskDetailResp> detailScheduleTask(@PathVariable Long scheduleId,
            @PathVariable Long taskId) {
        return Responses.success(scheduleService.detailScheduleTask(scheduleId, taskId));
    }

    @RequestMapping(value = "/schedules/{scheduleId:[\\d]+}/tasks", method = RequestMethod.GET)
    public PaginatedResponse<ScheduleTaskOverview> listTask(
            @PageableDefault(size = Integer.MAX_VALUE, sort = {"id"}, direction = Direction.DESC) Pageable pageable,
            @PathVariable Long scheduleId) {
        return Responses.paginated(scheduleService.listScheduleTaskOverview(pageable, scheduleId));
    }


    // list all schedule task by schedule type, type can not be null, currently only for sql plan
    @RequestMapping(value = "/tasks", method = RequestMethod.GET)
    public PaginatedResponse<ScheduleTaskListOverview> listAllTask(
            @PageableDefault(size = Integer.MAX_VALUE, sort = {"id"}, direction = Direction.DESC) Pageable pageable,
            @RequestParam(required = false, name = "dataSourceId") Set<Long> datasourceIds,
            @RequestParam(required = false, name = "databaseName") String databaseName,
            @RequestParam(required = false, name = "tenantId") String tenantId,
            @RequestParam(required = false, name = "clusterId") String clusterId,
            @RequestParam(required = false, name = "id") Long id,
            @RequestParam(required = false, name = "scheduleId") Long scheduleId,
            @RequestParam(required = false, name = "scheduleName") String scheduleName,
            @RequestParam(required = false, name = "status") List<TaskStatus> status,
            @RequestParam(required = true, name = "scheduleType") ScheduleType scheduleType,
            @RequestParam(required = false, name = "startTime") Date startTime,
            @RequestParam(required = false, name = "endTime") Date endTime,
            @RequestParam(required = false, name = "creator") String creator,
            @RequestParam(required = false, name = "projectId") Long projectId) {

        QueryScheduleTaskParams req = QueryScheduleTaskParams.builder()
                .id(id)
                .scheduleId(scheduleId)
                .scheduleName(scheduleName)
                .dataSourceIds(datasourceIds)
                .databaseName(databaseName)
                .tenantId(tenantId)
                .clusterId(clusterId)
                .statuses(status)
                .scheduleType(scheduleType)
                .startTime(startTime)
                .endTime(endTime)
                .creator(creator)
                .projectId(projectId)
                .build();

        return Responses.paginated(scheduleService.listScheduleTaskOverviewByScheduleType(pageable, req));
    }

    // schedule

    @RequestMapping(value = "/schedules/{id:[\\d]+}", method = RequestMethod.DELETE)
    public SuccessResponse<Boolean> deleteSchedule(@PathVariable Long id) {
        scheduleService.changeSchedule(ScheduleChangeParams.with(id, OperationType.DELETE));
        return Responses.success(Boolean.TRUE);
    }

    @RequestMapping(value = "/schedules/{id:[\\d]+}/terminate", method = RequestMethod.POST)
    public SuccessResponse<Boolean> terminateSchedule(@PathVariable("id") Long id) {
        scheduleService.changeSchedule(ScheduleChangeParams.with(id, OperationType.TERMINATE));
        return Responses.success(Boolean.TRUE);
    }

    @RequestMapping(value = "/schedules/{id:[\\d]+}/pause", method = RequestMethod.POST)
    public SuccessResponse<Boolean> pauseSchedule(@PathVariable Long id) {
        scheduleService.changeSchedule(ScheduleChangeParams.with(id, OperationType.PAUSE));
        return Responses.success(Boolean.TRUE);
    }

    @RequestMapping(value = "/schedules/{id:[\\d]+}/resume", method = RequestMethod.POST)
    public SuccessResponse<Boolean> resumeSchedule(@PathVariable Long id) {
        scheduleService.changeSchedule(ScheduleChangeParams.with(id, OperationType.RESUME));
        return Responses.success(Boolean.TRUE);
    }

    @RequestMapping(value = "/schedules/{id:[\\d]+}", method = RequestMethod.PUT)
    public SuccessResponse<Boolean> updateSchedule(@PathVariable Long id, @RequestBody UpdateScheduleReq req) {
        scheduleService.changeSchedule(ScheduleChangeParams.with(id, req));
        return Responses.success(Boolean.TRUE);
    }

    @RequestMapping(value = "/schedules", method = RequestMethod.POST)
    public SuccessResponse<Schedule> createSchedule(@RequestBody CreateScheduleReq req) {
        return Responses.success(scheduleService.changeSchedule(ScheduleChangeParams.with(req)));
    }


    @RequestMapping(value = "/schedules", method = RequestMethod.GET)
    public PaginatedResponse<ScheduleOverview> list(
            @PageableDefault(size = Integer.MAX_VALUE, sort = {"id"}, direction = Direction.DESC) Pageable pageable,
            @RequestParam(required = false, name = "dataSourceId") Set<Long> datasourceIds,
            @RequestParam(required = false, name = "databaseName") String databaseName,
            @RequestParam(required = false, name = "tenantId") String tenantId,
            @RequestParam(required = false, name = "clusterId") String clusterId,
            @RequestParam(required = false, name = "id") Long id,
            @RequestParam(required = false, name = "name") String name,
            @RequestParam(required = false, name = "status") List<ScheduleStatus> status,
            @RequestParam(required = false, name = "type") ScheduleType type,
            @RequestParam(required = false, name = "startTime") Date startTime,
            @RequestParam(required = false, name = "endTime") Date endTime,
            @RequestParam(required = false, name = "creator") String creator,
            @RequestParam(required = false, name = "projectId") Long projectId) {

        QueryScheduleParams req = QueryScheduleParams.builder()
                .id(id)
                .name(name)
                .dataSourceIds(datasourceIds)
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

        return Responses.paginated(scheduleService.listScheduleOverview(pageable, req));

    }

    @RequestMapping(value = "/schedules/{id:[\\d]+}", method = RequestMethod.GET)
    public SuccessResponse<ScheduleDetailResp> detailSchedule(@PathVariable Long id) {
        return Responses.success(scheduleService.detailSchedule(id));
    }

    @RequestMapping(value = "/schedules/{id:[\\d]+}/dlmRateLimitConfiguration", method = RequestMethod.PUT)
    public SuccessResponse<RateLimitConfiguration> updateLimiterConfig(@PathVariable Long id,
            @RequestBody RateLimitConfiguration limiterConfig) {
        return Responses.single(scheduleService.updateDlmRateLimit(id, limiterConfig));
    }
}
