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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.oceanbase.odc.service.common.NlsFormatService;
import com.oceanbase.odc.service.common.model.NlsFormatReq;
import com.oceanbase.odc.service.common.response.Responses;
import com.oceanbase.odc.service.common.response.SuccessResponse;
import com.oceanbase.odc.service.session.ConnectSessionService;
import com.oceanbase.odc.service.state.model.StateName;
import com.oceanbase.odc.service.state.model.StatefulRoute;

import io.swagger.annotations.ApiOperation;

/**
 * {@link NlsFormatController}
 *
 * @author yh263208
 * @date 2023-07-04 16:55
 * @since ODC_release_4.2.0
 */
@RestController
@RequestMapping("/api/v2/connects")
public class NlsFormatController {

    @Autowired
    private NlsFormatService nlsFormatService;
    @Autowired
    private ConnectSessionService sessionService;

    /**
     * 格式化日期
     *
     * @param sessionId
     * @return
     */
    @ApiOperation(value = "format", notes = "格式化时间")
    @RequestMapping(value = "/sessions/{sessionId}/format", method = RequestMethod.POST)
    @StatefulRoute(stateName = StateName.DB_SESSION, stateIdExpression = "#sessionId")
    public SuccessResponse<String> createSession(@PathVariable String sessionId, @RequestBody NlsFormatReq req) {
        return Responses.single(nlsFormatService.format(sessionService.nullSafeGet(sessionId, true), req));
    }

}
