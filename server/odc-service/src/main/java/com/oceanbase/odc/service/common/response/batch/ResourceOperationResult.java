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
package com.oceanbase.odc.service.common.response.batch;

import com.oceanbase.odc.core.shared.constant.OperationType;
import com.oceanbase.odc.core.shared.constant.ResourceType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author keyang
 * @date 2024/09/18
 * @since 4.3.2
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResourceOperationResult {
    private ResourceType resourceType;
    private String resourceId;
    private String resourceName;
    private OperationType operation;
    private Boolean success;
    private Error error;
}
