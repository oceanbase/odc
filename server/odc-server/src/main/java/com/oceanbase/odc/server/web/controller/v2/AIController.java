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
import com.oceanbase.odc.service.datasecurity.ai.AIStatusResponse;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * AI功能控制器
 * 提供AI功能状态查询接口
 */
@Api(tags = "AI功能")
@RestController
@RequestMapping("/api/v2/ai")
public class AIController {

    @Autowired
    private AIConfig aiConfig;

    /**
     * 查询AI功能状态
     * @return AI功能状态信息
     */
    @ApiOperation(value = "查询AI功能状态", notes = "返回AI功能是否启用以及配置状态")
    @SkipAuthorize("AI status is safe to query for authenticated users")
    @GetMapping("/status")
    public SuccessResponse<AIStatusResponse> getAIStatus() {
        AIStatusResponse response = new AIStatusResponse();
        response.setEnabled(aiConfig.isEnabled());
        response.setAvailable(aiConfig.isAIAvailable());
        response.setModel(aiConfig.getModel());
        response.setBaseUrl(aiConfig.getBaseUrl());
        // 不返回敏感信息如API密钥
        response.setApiKeyConfigured(aiConfig.getApiKey() != null && !aiConfig.getApiKey().trim().isEmpty());

        return Responses.success(response);
    }
}
