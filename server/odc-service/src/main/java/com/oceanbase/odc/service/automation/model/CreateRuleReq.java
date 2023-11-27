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
package com.oceanbase.odc.service.automation.model;

import java.util.List;

import javax.validation.constraints.Size;

import com.oceanbase.odc.common.validate.Name;

import lombok.Data;

@Data
public class CreateRuleReq {
    @Size(min = 1, max = 128, message = "Automation rule name is out of range [1,128]")
    @Name(message = "Automation rule name cannot start or end with whitespaces")
    private String name;
    private Long eventId;
    private Boolean enabled;
    private String description;
    private List<AutomationCondition> conditions;
    private List<AutomationAction> actions;
}
