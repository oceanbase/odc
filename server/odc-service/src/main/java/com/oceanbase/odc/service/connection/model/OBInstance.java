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

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.oceanbase.odc.service.cloud.model.CloudProvider;

import lombok.Data;

/**
 * 公有云/多云 实例
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OBInstance {
    /**
     * 实例 ID
     */
    private String id;
    /**
     * 实例名称
     */
    private String name;

    /**
     * 实例类型
     */
    @JsonAlias({"instanceType", "type"})
    private OBInstanceType type;

    /**
     * 实例状态
     */
    @JsonAlias("status")
    private OBInstanceStatus state;

    /**
     * 当 type 为 CLUSTER 时表示该集群实例下的所有租户，如果集群下没有租户，则返回空 List；否则为 @code{NULL}
     */
    private List<OBTenant> tenants;

    /**
     * 地域
     */
    private String region;

    /**
     * 云储存类型
     */
    private CloudProvider cloudProvider;
}
