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

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.oceanbase.odc.service.llm.model.ModelType;

import lombok.Data;

@Data
public class ProviderTemplate {
    private Help help;
    private List<String> configurateMethods;
    private ModelCredentialSchema modelCredentialSchema;
    @JsonProperty(access = Access.WRITE_ONLY)
    private Models models;
    private String provider;
    private ProviderCredentialSchema providerCredentialSchema;
    private List<ModelType> supportedModelTypes;

    @Data
    public static class I18nMap {
        @JsonProperty("en_US")
        private String enUs;
        @JsonProperty("zh_Hans")
        private String zhHans;
    }

    @Data
    public static class Help {
        private I18nMap title;
        private I18nMap url;
    }

    @Data
    public static class ModelCredentialSchema {
        private List<CredentialFormSchema> credentialFormSchemas;
        private Model model;
    }

    @Data
    public static class CredentialFormSchema {
        private I18nMap label;
        private I18nMap placeholder;
        private boolean required;
        private String type;
        private String variable;
        @JsonAlias("default")
        private String defaultValue;
        private List<ShowOn> showOn;
        private List<Option> options;
    }

    @Data
    public static class ShowOn {
        private String value;
        private String variable;
    }

    @Data
    public static class Option {
        private I18nMap label;
        private String value;
    }

    @Data
    public static class Model {
        private I18nMap label;
        private I18nMap placeholder;
    }

    @Data
    public static class Models {
        private List<String> chat;
        private List<String> embedding;
    }

    @Data
    public static class ProviderCredentialSchema {
        private List<CredentialFormSchema> credentialFormSchemas;
    }

}
