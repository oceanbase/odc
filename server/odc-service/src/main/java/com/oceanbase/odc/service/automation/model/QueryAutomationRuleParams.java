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

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QueryAutomationRuleParams {

    /**
     * 按照名称模糊搜索
     */
    private String name;
    /**
     * 按照创建者名称进行模糊搜索
     */
    private String creatorName;
    /**
     * 按照是否启用进行筛选
     */
    private Boolean enabled;

}
