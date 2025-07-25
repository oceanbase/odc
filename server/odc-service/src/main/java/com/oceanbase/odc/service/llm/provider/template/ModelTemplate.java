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
package com.oceanbase.odc.service.llm.provider.template;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.oceanbase.odc.service.llm.model.ModelType;

import lombok.Data;

@Data
public class ModelTemplate {

    private String model;

    private ModelType modelType;

    private List<String> features;

    private ModelProperties modelProperties;

    private List<ParameterRule> parameterRules;

    private boolean deprecated;

    @Data
    public static class I18nMap {
        @JsonProperty("en_US")
        private String enUs;
        @JsonProperty("zh_Hans")
        private String zhHans;
    }

    @Data
    public static class ModelProperties {
        private String mode;

        private Integer contextSize;

        private Map<String, Object> additionalProperties;
    }

    @Data
    public static class ParameterRule {
        private String name;

        private String useTemplate;

        private Integer min;

        private Integer max;

        @JsonProperty("default")
        private Object defaultValue;

        private Boolean required;

        private String type;

        private List<String> options;
    }
}
