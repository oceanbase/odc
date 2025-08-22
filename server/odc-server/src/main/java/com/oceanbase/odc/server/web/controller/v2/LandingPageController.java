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
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.collect.Sets;
import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.service.collaboration.landingpage.LandingPageService;
import com.oceanbase.odc.service.collaboration.landingpage.model.QueryFlowInstanceStatParams;
import com.oceanbase.odc.service.collaboration.landingpage.model.QueryScheduleStatParams;
import com.oceanbase.odc.service.collaboration.landingpage.model.QueryStatParams;
import com.oceanbase.odc.service.common.model.Stats;
import com.oceanbase.odc.service.common.response.Responses;
import com.oceanbase.odc.service.common.response.SuccessResponse;
import com.oceanbase.odc.service.schedule.model.ScheduleType;

import io.swagger.annotations.ApiOperation;

/**
 * @author mayang
 */
@RestController(value = "odcLandingPageController")
@RequestMapping("/api/v2/collaboration/landingPage")
public class LandingPageController {
    @Autowired
    private LandingPageService landingPageService;

    @ApiOperation(value = "scheduleStat", notes = "Returns schedule and task stat")
    @RequestMapping(value = "/scheduleStat", method = RequestMethod.GET)
    public SuccessResponse<Stats> listScheduleStat(
            @RequestParam(required = false, name = "types") Set<ScheduleType> types,
            @RequestParam(required = false, name = "startTime") Date startTime,
            @RequestParam(required = false, name = "projectId") Long projectId,
            @RequestParam(required = false, name = "endTime") Date endTime) {
        QueryScheduleStatParams req = QueryScheduleStatParams.builder()
                .scheduleTypes(types)
                .projectIds(Sets.newHashSet(projectId))
                .startTime(startTime)
                .endTime(endTime)
                .build();
        return Responses.success(landingPageService.listScheduleAndTaskStats(req));
    }

    @ApiOperation(value = "flowInstanceStat", notes = "Returns flowInstance and task stat")
    @RequestMapping(value = "/flowInstanceStat", method = RequestMethod.GET)
    public SuccessResponse<Stats> listFlowInstanceStat(
            @RequestParam(required = false, name = "types") Set<TaskType> types,
            @RequestParam(required = false, name = "startTime") Date startTime,
            @RequestParam(required = false, name = "endTime") Date endTime,
            @RequestParam(required = false, name = "projectId") Long projectId) {
        QueryFlowInstanceStatParams req = QueryFlowInstanceStatParams.builder()
                .taskTypes(types)
                .projectIds(Sets.newHashSet(projectId))
                .startTime(startTime)
                .endTime(endTime)
                .build();
        return Responses.success(landingPageService.listFlowInstanceTaskStats(req));
    }

    @ApiOperation(value = "flowInstanceAndScheduleTodoStat", notes = "Returns flowInstance and schedule todo stat")
    @RequestMapping(value = "/flowScheduleTodoStat", method = RequestMethod.GET)
    public SuccessResponse<Stats> listFlowInstanceAndScheduleTodoStat(
            @RequestParam(required = false, name = "projectId") Long projectId) {
        QueryStatParams req = QueryStatParams.builder()
                .projectIds(Sets.newHashSet(projectId))
                .build();
        return Responses.success(landingPageService.getFlowAndScheduleTodoStat(req));
    }
}
