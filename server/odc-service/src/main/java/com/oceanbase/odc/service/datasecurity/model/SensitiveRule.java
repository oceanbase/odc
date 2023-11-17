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
package com.oceanbase.odc.service.datasecurity.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.oceanbase.odc.common.validate.Name;
import com.oceanbase.odc.core.authority.model.SecurityResource;
import com.oceanbase.odc.core.shared.OrganizationIsolated;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.service.common.model.InnerUser;
import com.oceanbase.odc.service.datasecurity.util.ParameterValidateUtil;

import lombok.Data;

/**
 * @author gaoda.xy
 * @date 2023/5/9 11:40
 */
@Data
public class SensitiveRule implements SecurityResource, OrganizationIsolated {

    @JsonProperty(access = Access.READ_ONLY)
    private Long id;

    @Size(min = 1, max = 64, message = "Sensitive rule name is out of range [1,64]")
    @Name(message = "Sensitive rule name cannot start or end with whitespaces")
    private String name;

    @NotNull
    private Boolean enabled;

    @JsonProperty(access = Access.READ_ONLY)
    private Long projectId;

    @NotNull
    private SensitiveRuleType type;

    private String databaseRegexExpression;

    private String tableRegexExpression;

    private String columnRegexExpression;

    private String columnCommentRegexExpression;

    private String groovyScript;

    private List<String> pathIncludes = new ArrayList<>();

    private List<String> pathExcludes = new ArrayList<>();

    @NotNull
    private Long maskingAlgorithmId;

    private SensitiveLevel level = SensitiveLevel.HIGH;

    private String description;

    @JsonProperty(access = Access.READ_ONLY)
    private Boolean builtin;

    @JsonProperty(access = Access.READ_ONLY)
    private InnerUser creator;

    @JsonProperty(access = Access.READ_ONLY)
    private Date createTime;

    @JsonProperty(access = Access.READ_ONLY)
    private Date updateTime;

    @JsonProperty(access = Access.READ_ONLY)
    private Long organizationId;

    @Override
    public String resourceId() {
        return this.id.toString();
    }

    @Override
    public String resourceType() {
        return ResourceType.ODC_SENSITIVE_RULE.name();
    }

    @Override
    public Long organizationId() {
        return this.organizationId;
    }

    @Override
    public Long id() {
        return this.id;
    }

    public void validate() {
        switch (type) {
            case PATH:
                ParameterValidateUtil.validatePathExpression(pathIncludes, pathExcludes);
                break;
            case REGEX:
                ParameterValidateUtil.validateRegexExpression(databaseRegexExpression, tableRegexExpression,
                        columnRegexExpression, columnCommentRegexExpression);
                break;
            case GROOVY:
                ParameterValidateUtil.validateGroovyScript(groovyScript);
                break;
            default:
                break;
        }
    }

}
