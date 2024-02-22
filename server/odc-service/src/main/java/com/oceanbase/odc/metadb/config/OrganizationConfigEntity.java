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
package com.oceanbase.odc.metadb.config;

import javax.validation.constraints.NotNull;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Author: Lebie
 * @Date: 2021/7/12 下午2:33
 * @Description: [Organization Configuration Data Object for DAO]
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OrganizationConfigEntity extends ConfigEntity {
    /**
     * organizationId of this organization config
     */
    @NotNull
    private Long organizationId;

}
