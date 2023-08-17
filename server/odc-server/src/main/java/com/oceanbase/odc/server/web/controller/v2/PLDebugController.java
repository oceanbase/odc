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

import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.oceanbase.odc.service.common.response.Responses;
import com.oceanbase.odc.service.common.response.SuccessResponse;
import com.oceanbase.odc.service.common.util.SidUtils;
import com.oceanbase.odc.service.pldebug.PLDebugService;
import com.oceanbase.odc.service.pldebug.model.PLDebugBreakpoint;
import com.oceanbase.odc.service.pldebug.model.PLDebugContextResp;
import com.oceanbase.odc.service.pldebug.model.PLDebugVariable;
import com.oceanbase.odc.service.pldebug.model.StartPLDebugReq;
import com.oceanbase.odc.service.session.ConnectSessionService;

import io.swagger.annotations.ApiOperation;

/**
 * @author wenniu.ly
 * @date 2021/10/26
 */

@RestController
@RequestMapping("/api/v2/pldebug/sessions")
public class PLDebugController {

    @Autowired
    private PLDebugService plDebugService;
    @Autowired
    private ConnectSessionService sessionService;

    @ApiOperation(value = "startDebug", notes = "开启调试")
    @RequestMapping(value = "/", method = RequestMethod.POST)
    public SuccessResponse<String> start(@RequestBody StartPLDebugReq request)
            throws Exception {
        Validate.notEmpty(request.getSid(), "Sid can not be null for StartPLDebugReq");
        return Responses.success(plDebugService.start(sessionService.nullSafeGet(
                SidUtils.getSessionId(request.getSid())), request));
    }

    @ApiOperation(value = "endDebug", notes = "结束调试")
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public SuccessResponse<String> end(@PathVariable String id) {
        return Responses.success(plDebugService.end(id));
    }

    @ApiOperation(value = "setBreakpoints", notes = "设置断点")
    @RequestMapping(value = "/{id}/breakpoints/batchCreate", method = RequestMethod.POST)
    public SuccessResponse<List<PLDebugBreakpoint>> setBreakpoints(@PathVariable String id,
            @RequestBody List<PLDebugBreakpoint> breakpoints) {
        return Responses.success(plDebugService.setBreakpoints(id, breakpoints));
    }

    @ApiOperation(value = "deleteBreakpoints", notes = "删除断点")
    @RequestMapping(value = "/{id}/breakpoints/batchDelete", method = RequestMethod.DELETE)
    public SuccessResponse<Boolean> deleteBreakpoints(@PathVariable String id,
            @RequestBody List<PLDebugBreakpoint> breakpoints) {
        return Responses.success(plDebugService.deleteBreakpoints(id, breakpoints));
    }

    @ApiOperation(value = "listBreakpoints", notes = "获取断点列表")
    @RequestMapping(value = "/{id}/breakpoints", method = RequestMethod.GET)
    public SuccessResponse<List<PLDebugBreakpoint>> listBreakpoints(@PathVariable String id) {
        return Responses.success(plDebugService.listBreakpoints(id));
    }

    @ApiOperation(value = "getVariables", notes = "获取堆栈变量")
    @RequestMapping(value = "/{id}/variables", method = RequestMethod.GET)
    public SuccessResponse<List<PLDebugVariable>> getVariables(@PathVariable String id) {
        return Responses.success(plDebugService.getVariables(id));
    }

    @ApiOperation(value = "getContext", notes = "获取堆栈上下文")
    @RequestMapping(value = "/{id}/context", method = RequestMethod.GET)
    public SuccessResponse<PLDebugContextResp> getContext(@PathVariable String id) {
        return Responses.success(plDebugService.getContext(id));
    }

    @ApiOperation(value = "stepOver", notes = "单步执行")
    @RequestMapping(value = "/{id}/stepOver", method = RequestMethod.POST)
    public SuccessResponse<Boolean> stepOver(@PathVariable String id) {
        return Responses.success(plDebugService.stepOver(id));
    }

    @ApiOperation(value = "resume", notes = "恢复执行")
    @RequestMapping(value = "/{id}/resume", method = RequestMethod.POST)
    public SuccessResponse<Boolean> resume(@PathVariable String id) {
        return Responses.success(plDebugService.resume(id));
    }

    @ApiOperation(value = "stepIn", notes = "步入")
    @RequestMapping(value = "/{id}/stepIn", method = RequestMethod.POST)
    public SuccessResponse<Boolean> stepIn(@PathVariable String id) {
        return Responses.success(plDebugService.stepIn(id));
    }

    @ApiOperation(value = "stepOut", notes = "跳出")
    @RequestMapping(value = "/{id}/stepOut", method = RequestMethod.POST)
    public SuccessResponse<Boolean> stepOut(@PathVariable String id) {
        return Responses.success(plDebugService.stepOut(id));
    }

    @ApiOperation(value = "resumeIgnoreBreakpoints", notes = "恢复执行并忽略断点")
    @RequestMapping(value = "/{id}/resumeIgnoreBreakpoints", method = RequestMethod.POST)
    public SuccessResponse<Boolean> resumeIgnoreBreakpoints(@PathVariable String id) {
        return Responses.success(plDebugService.resumeIgnoreBreakpoints(id));
    }
}
