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
package com.oceanbase.odc.service.integration.model;

import lombok.Builder;
import lombok.Data;

/**
 * @author gaoda.xy
 * @date 2023/3/27 20:50
 */
@Data
@Builder
public class QueryIntegrationParams {
    /**
     * Fuzzy search by name
     */
    private String name;

    /**
     * Search by type
     */
    private IntegrationType type;

    /**
     * Fuzzy search by creator name
     */
    private String creatorName;

    /**
     * Search by enabled status
     */
    private Boolean enabled;
}
