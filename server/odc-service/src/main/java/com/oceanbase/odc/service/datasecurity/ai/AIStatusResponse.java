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

package com.oceanbase.odc.service.datasecurity.ai;

import lombok.Data;

/**
     * AI功能状态响应
     */
    @Data
    public class AIStatusResponse {
        /**
         * AI功能是否启用
         */
        private boolean enabled;

        /**
         * AI功能是否可用（启用且配置完整）
         */
        private boolean available;

        /**
         * 使用的AI模型
         */
        private String model;

        /**
         * API基础URL
         */
        private String baseUrl;

        /**
         * API密钥是否已配置
         */
        private boolean apiKeyConfigured;
    }