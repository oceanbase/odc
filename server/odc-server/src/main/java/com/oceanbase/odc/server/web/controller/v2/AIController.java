/*
 * Copyright (c) 2025 OceanBase.
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.service.common.response.Responses;
import com.oceanbase.odc.service.common.response.SuccessResponse;
import com.oceanbase.odc.service.datasecurity.ai.AIConfig;
import com.oceanbase.odc.service.datasecurity.ai.AIInferenceService;
import com.oceanbase.odc.service.datasecurity.ai.AIStatusResponse;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * @author fenyf
 * @date 2025/8/10 12:41
 */
@Api(tags = "AI")
@RestController
@RequestMapping("/api/v2/ai")
public class AIController {

    @Autowired
    private AIConfig aiConfig;

    @Autowired
    private AIInferenceService aiInferenceService;


    @ApiOperation(value = "Query the status of the AI function",
        notes = "Return the status of whether the AI function is enabled and its configuration status")
    @SkipAuthorize("AI status is safe to query for authenticated users")
    @GetMapping("/status")
    public SuccessResponse<AIStatusResponse> getAIStatus() {
        AIStatusResponse response = new AIStatusResponse();
        response.setEnabled(aiConfig.isEnabled());
        response.setAvailable(aiInferenceService.isAIAvailable());
        response.setModel(aiConfig.getModel());
        response.setBaseUrl(aiConfig.getBaseUrl());
        response.setApiKeyConfigured(aiConfig.getApiKey() != null && !aiConfig.getApiKey().trim().isEmpty());

        return Responses.success(response);
    }
}
