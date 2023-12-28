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
import com.oceanbase.odc.service.schedule.model.JobType;
import com.oceanbase.odc.service.schedule.model.QueryScheduleParams;
import com.oceanbase.odc.service.schedule.model.ScheduleDetailResp;
import com.oceanbase.odc.service.schedule.model.ScheduleStatus;
import com.oceanbase.odc.service.schedule.model.ScheduleTaskResp;
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

    @RequestMapping("/scheduleConfigs")
    public PaginatedResponse<ScheduleDetailResp> list(
            @PageableDefault(size = Integer.MAX_VALUE, sort = {"id"}, direction = Direction.DESC) Pageable pageable,
            @RequestParam(required = false, name = "connectionId") List<Long> connectionIds,
            @RequestParam(required = false, name = "id") Long id,
            @RequestParam(required = false, name = "status") List<ScheduleStatus> status,
            @RequestParam(required = false, name = "type") JobType type,
            @RequestParam(required = false, name = "startTime") Date startTime,
            @RequestParam(required = false, name = "endTime") Date endTime,
            @RequestParam(required = false, name = "creator") String creator,
            @RequestParam(required = false, name = "projectId") Long projectId) {

        QueryScheduleParams req = QueryScheduleParams.builder()
                .id(id)
                .connectionIds(connectionIds)
                .statuses(status)
                .type(type)
                .startTime(startTime)
                .endTime(endTime)
                .creator(creator)
                .projectId(projectId)
                .build();
        return Responses.paginated(scheduleService.list(pageable, req));

    }

    @RequestMapping(value = "/scheduleConfigs/{id:[\\d]+}", method = RequestMethod.GET)
    public SuccessResponse<ScheduleDetailResp> detail(@PathVariable Long id) {
        return Responses.single(scheduleService.getById(id));
    }

    @RequestMapping(value = "/{id:[\\d]+}/jobs/async/batchGetDownloadUrl", method = RequestMethod.POST)
    public ListResponse<String> getDownloadUrl(@PathVariable Long id, @RequestBody List<String> objectId) {
        return Responses.list(scheduleService.getAsyncDownloadUrl(id, objectId));
    }

    @RequestMapping(value = "/schedules/{id:[\\d]+}/dlmRateLimitConfiguration", method = RequestMethod.PUT)
    public SuccessResponse<RateLimitConfiguration> updateLimiterConfig(@PathVariable Long id,
            @RequestBody RateLimitConfiguration limiterConfig) {
        return Responses.single(dlmLimiterService.updateByOrderId(id, limiterConfig));
    }

    @ApiOperation(value = "TriggerJob", notes = "立即调度")
    @RequestMapping(value = "/schedules/{scheduleId:[\\d]+}", method = RequestMethod.POST)
    public SuccessResponse<ScheduleDetailResp> triggerJob(@PathVariable Long scheduleId, @RequestParam String jobType) {
        return Responses.single(scheduleService.triggerJob(scheduleId, jobType));
    }

    @RequestMapping(value = "/schedules/{scheduleId:[\\d]+}/tasks", method = RequestMethod.GET)
    public PaginatedResponse<ScheduleTaskResp> listTask(
            @PageableDefault(size = Integer.MAX_VALUE, sort = {"id"}, direction = Direction.DESC) Pageable pageable,
            @PathVariable Long scheduleId) {
        return Responses.paginated(scheduleService.listTask(pageable, scheduleId));
    }

    @ApiOperation(value = "StartTask", notes = "启动任务")
    @RequestMapping(value = "/schedules/{scheduleId:[\\d]+}/tasks/{taskId:[\\d]+}/start", method = RequestMethod.PUT)
    public SuccessResponse<ScheduleTaskResp> startTask(@PathVariable Long scheduleId, @PathVariable Long taskId) {
        return Responses.single(scheduleService.startTask(scheduleId, taskId));
    }

    @ApiOperation(value = "InterruptTask", notes = "中断任务")
    @RequestMapping(value = "/schedules/{scheduleId:[\\d]+}/tasks/{taskId:[\\d]+}/interrupt",
            method = RequestMethod.PUT)
    public SuccessResponse<ScheduleDetailResp> interruptTask(@PathVariable Long scheduleId, @PathVariable Long taskId) {
        return Responses.single(scheduleService.interruptJob(scheduleId, taskId));
    }

    @ApiOperation(value = "PauseTask", notes = "暂停任务")
    @RequestMapping(value = "/schedules/{scheduleId:[\\d]+}/tasks/{taskId:[\\d]+}/pause", method = RequestMethod.PUT)
    public SuccessResponse<ScheduleTaskResp> pauseTask(@PathVariable Long scheduleId, @PathVariable Long taskId) {
        throw new UnsupportedException();
    }

    @ApiOperation(value = "RollbackTask", notes = "回滚任务")
    @RequestMapping(value = "/schedules/{scheduleId:[\\d]+}/tasks/{taskId:[\\d]+}/rollback", method = RequestMethod.PUT)
    public SuccessResponse<ScheduleTaskResp> rollbackTask(@PathVariable Long scheduleId, @PathVariable Long taskId) {
        return Responses.single(scheduleService.rollbackTask(scheduleId, taskId));
    }


    @ApiOperation(value = "GetScheduleTaskLog", notes = "获取计划任务日志")
    @RequestMapping(value = "/schedules/{scheduleId:[\\d]+}/tasks/{taskId:[\\d]+}/log", method = RequestMethod.GET)
    public SuccessResponse<String> getScheduleTaskLog(@PathVariable Long scheduleId, @PathVariable Long taskId,
            @RequestParam OdcTaskLogLevel logType) {
        return Responses.single(scheduleService.getLog(scheduleId, taskId, logType));
    }

}
