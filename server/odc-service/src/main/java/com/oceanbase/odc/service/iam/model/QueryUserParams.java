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
package com.oceanbase.odc.service.iam.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author gaoda.xy
 * @date 2022/12/6 14:28
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryUserParams {
    private Boolean enabled;
    private List<Long> roleIds;
    private List<String> names;
    private List<String> accountNames;
    private String authorizedResource;
    private Boolean includePermissions;
    private Long organizationId;
    private String minPrivilege;
    private Boolean basic;
}
