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
package com.oceanbase.odc.service.integration.model;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import com.oceanbase.odc.common.util.YamlUtils;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author gaoda.xy
 * @date 2023/3/27 14:43
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ApprovalProperties extends IntegrationProperties {
    private int approvalTimeoutSeconds = 24 * 60 * 60;
    @Valid
    @NotNull
    private Api api;
    private AdvancedProperties advanced;

    @Data
    public static class Api {
        @Valid
        @NotNull
        private StartProperties start;
        @Valid
        @NotNull
        private StatusProperties status;
        @Valid
        @NotNull
        private ApiProperties cancel;
    }

    @Data
    public static class AdvancedProperties {
        private String hyperlinkExpression;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class StartProperties extends ApiProperties {
        @NotBlank
        private String extractInstanceIdExpression;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class StatusProperties extends ApiProperties {
        @NotBlank
        private String processPendingExpression;
        @NotBlank
        private String processApprovedExpression;
        @NotBlank
        private String processRejectedExpression;
        @NotBlank
        private String processTerminatedExpression;
    }

    public static ApprovalProperties from(IntegrationConfig config) {
        ApprovalProperties properties = YamlUtils.from(config.getConfiguration(), ApprovalProperties.class);
        properties.setEncryption(config.getEncryption());
        return properties;
    }
}
