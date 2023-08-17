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

import java.util.List;

import javax.validation.constraints.NotNull;

import lombok.Builder;
import lombok.Data;

/**
 * @author gaoda.xy
 * @date 2023/5/19 15:03
 */
@Data
@Builder
public class QuerySensitiveRuleParams {
    @NotNull
    private Long projectId;

    private String name;

    private List<SensitiveRuleType> types;

    private List<Long> maskingAlgorithmIds;

    private Boolean enabled;

}
