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
package com.oceanbase.odc.service.resourcegroup.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Request object, used to list all <code>ResourceGroup</code> or get a specific
 * <code>ResourceGroup</code>
 *
 * @author yh263208
 * @date 2021-07-27 20:09
 * @since ODC-release_3.2.0
 */
@Getter
@Setter
@AllArgsConstructor
@ToString
public class QueryResourceGroupReq {
    /**
     * Query parmeter for <code>ResourceGroup.name</code>
     */
    private String fuzzySearchKeyword;
    /**
     * Query parmeter for <code>ResourceGroup.enabled</code>
     */
    private List<Boolean> statuses;

    private String minPrivilege;

    public QueryResourceGroupReq() {}

}
