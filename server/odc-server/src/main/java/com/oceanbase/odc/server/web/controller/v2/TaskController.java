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

import java.util.Collections;
import java.util.Map;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.service.common.response.Responses;
import com.oceanbase.odc.service.common.response.SuccessResponse;
import com.oceanbase.odc.service.task.caller.JobUtils;
import com.oceanbase.odc.service.task.executor.task.DefaultTaskResult;

import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2023-11-29
 * @since 4.2.4
 */
@Slf4j
@RestController
@RequestMapping("/api/v2/task")
public class TaskController {

    @ApiOperation(value = "updateResult", notes = "update task result")
    @RequestMapping(value = "/result", method = RequestMethod.POST)
    public SuccessResponse<String> updateResult(@RequestBody DefaultTaskResult taskResult) {
        log.info("====== get task result =======");
        log.info(JobUtils.toJson(taskResult));
        Map<String, String> map = Collections.singletonMap("resultUpdate", "ok");

        return Responses.success(JsonUtils.toJson(map));
    }

}
