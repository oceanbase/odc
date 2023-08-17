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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.oceanbase.odc.service.common.response.ListResponse;
import com.oceanbase.odc.service.common.response.Responses;
import com.oceanbase.odc.service.common.response.SuccessResponse;
import com.oceanbase.odc.service.lab.TutorialService;
import com.oceanbase.odc.service.lab.model.Tutorial;
import com.oceanbase.odc.service.session.SessionLimitService;
import com.oceanbase.odc.service.session.SessionLimitService.SessionLimitResp;

import io.swagger.annotations.ApiOperation;

/**
 * @author liuyizhuo.lyz
 * @date 2022/7/18
 */

@RestController
@RequestMapping("/api/v2/lab")
public class LabController {
    @Autowired
    private TutorialService tutorialService;

    @Autowired
    private SessionLimitService sessionLimitService;

    @ApiOperation(value = "listTutorials", notes = "查询教程列表")
    @RequestMapping(value = "/tutorials", method = RequestMethod.GET)
    public ListResponse<Tutorial> listTutorials() {
        return Responses.list(tutorialService.list());
    }

    @ApiOperation(value = "getTutorialById", notes = "根据ID查询教程详情")
    @RequestMapping(value = "/tutorials/{id:[\\d]+}", method = RequestMethod.GET)
    public SuccessResponse<Tutorial> getTutorialById(@PathVariable Long id) {
        return Responses.success(tutorialService.findById(id));
    }

    @ApiOperation(value = "getTutorialByFilename", notes = "根据文件名查询教程详情")
    @RequestMapping(value = "/tutorials/{filename}/{language}", method = RequestMethod.GET)
    public SuccessResponse<Tutorial> getTutorialByFilename(@PathVariable String filename,
            @PathVariable String language) {
        return Responses.success(tutorialService.findByFilenameAndLanguage(filename, language));
    }

    @GetMapping("/status")
    public SuccessResponse<SessionLimitResp> lineupStatus() {
        return Responses.success(sessionLimitService.getUserLineupStatus());
    }
}
