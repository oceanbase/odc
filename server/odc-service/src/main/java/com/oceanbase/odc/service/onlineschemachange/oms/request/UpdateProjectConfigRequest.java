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

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author hengyu
 * @since 2024/1/5
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProjectConfigRequest extends BaseOmsRequest {
    /**
     * Oms project id
     */
    @NotEmpty
    private String id;
    /**
     * full transfer config
     */
    @Valid
    private FullTransferConfig fullTransferConfig;
    /**
     * increment transfer config
     */
    @Valid
    private IncrTransferConfig incrTransferConfig;
}
