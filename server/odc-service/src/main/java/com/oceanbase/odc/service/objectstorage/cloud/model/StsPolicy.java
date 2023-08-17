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
package com.oceanbase.odc.service.objectstorage.cloud.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.oceanbase.odc.core.shared.PreConditions;

import lombok.Data;

/**
 * Additional permission scope of Credential, <br>
 * e.g. resource path <br>
 * refer schema from
 * https://docs.aws.amazon.com/AmazonS3/latest/userguide/example-bucket-policies.html
 */
@Data
public class StsPolicy {
    @JsonProperty("Version")
    private String version = "1";
    @JsonProperty("Statement")
    private List<Statement> statements = new ArrayList<>();

    public StsPolicy withVersion(String version) {
        this.version = version;
        return this;
    }

    public StsPolicy allowPutObject(String action, String resource) {
        PreConditions.notBlank(action, "action");
        PreConditions.notBlank(resource, "resource");
        Statement statement = new Statement();
        statement.setEffect(Effect.Allow);
        statement.setActions(Arrays.asList(action));
        statement.setResources(Arrays.asList(resource));
        this.statements.add(statement);
        return this;
    }

    public enum Effect {
        Allow,
        Deny,;
    }

    @Data
    public static class Statement {
        @JsonProperty("Effect")
        private Effect effect;
        @JsonProperty("Action")
        private List<String> actions;
        @JsonProperty("Resource")
        private List<String> resources;
    }
}
