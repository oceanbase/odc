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

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.oceanbase.odc.service.ai.chat.SqlCopilotService;
import com.oceanbase.odc.service.ai.chat.model.SqlCopilotReq;

/**
 * @author: liuyizhuo.lyz
 * @date: 2025/7/22
 */
@RestController()
@RequestMapping("/api/v2/copilot")
public class SqlCopilotChatController {
    @Autowired
    private SqlCopilotService sqlCopilotService;

    @PostMapping(value = "/chat/completions", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter completion(@RequestBody SqlCopilotReq req) throws IOException {
        return sqlCopilotService.completion(req);
    }

}
