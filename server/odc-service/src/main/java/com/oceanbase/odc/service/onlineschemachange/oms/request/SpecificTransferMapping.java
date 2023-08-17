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
import javax.validation.constraints.Size;

import lombok.Data;

/**
 * 传输对象映射
 *
 * @author yaobin
 * @date 2023-06-01
 * @since 4.2.0
 */
@Data
public class SpecificTransferMapping {

    /**
     * 传输对象映射模式 目前存在传统方式（即库表映射）和黑白名单方式 1. SPECIFIC 2. WILDCARD
     */
    private String mode = "SPECIFIC";

    @Valid
    @Size(min = 1, message = "specific transfer mapping databases can not be empty")
    public List<DatabaseTransferObject> databases;

}
