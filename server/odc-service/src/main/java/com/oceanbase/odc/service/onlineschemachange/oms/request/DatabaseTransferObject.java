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
package com.oceanbase.odc.service.onlineschemachange.oms.request;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import lombok.Data;

/**
 * @author yaobin
 * @date 2023-06-01
 * @since 4.2.0
 */
@Data
public class DatabaseTransferObject {

    /**
     * 映射对象集群
     */
    private String clusterName;

    /**
     * 映射对象租户
     */
    private String tenantName;

    /**
     * 库名
     */
    @NotBlank(message = "database transfer object name can not be blank")
    private String name;

    /**
     * 映射库名
     */
    @NotBlank(message = "database transfer object mapped name can not be blank")
    private String mappedName;

    /**
     * 表映射
     */
    @Valid
    @Size(min = 1, message = "database transfer object tables cannot be empty")
    private List<TableTransferObject> tables;

}
