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
package com.oceanbase.odc.service.connection.model;

import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.core.shared.constant.DialectType;

import lombok.Builder;
import lombok.Data;

/**
 * @author yizhou.xw
 * @version : QueryConnectionParams.java, v 0.1 2021-07-22 9:57
 */
@Data
@Builder
public class QueryConnectionParams {

    /**
     * 用于查询用户关联的数据源，如果不设则默认为当前用户
     */
    @JsonIgnore
    private Long relatedUserId;
    private String fuzzySearchKeyword;
    private List<ConnectType> types;
    private List<DialectType> dialectTypes;
    /**
     * 数据源的 id 列表，该值不接受外界 set，是一个计算值。即使 set 也会被覆盖掉
     */
    private Set<Long> ids;
    private Boolean enabled;
    /**
     * 最小权限限制，意为查询者拥有至少 minPrivilege 权限或以上的数据源才会返回
     */
    private String minPrivilege;
    private List<String> clusterNames;
    private List<String> tenantNames;
    private List<String> permittedActions;
    private String hostPort;
    private String name;
    /**
     * 数据库用户名
     */
    private String username;

}
