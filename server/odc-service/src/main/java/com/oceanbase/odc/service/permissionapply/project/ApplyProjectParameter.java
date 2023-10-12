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

package com.oceanbase.odc.service.permissionapply.project;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.oceanbase.odc.service.flow.model.TaskParameters;

import lombok.Data;

/**
 * @author gaoda.xy
 * @date 2023/10/12 09:58
 */
@Data
public class ApplyProjectParameter implements Serializable, TaskParameters {

    /**
     * ID of the project to be applied for
     */
    private Long projectId;
    /**
     * IDs of the resource roles to be applied for
     */
    private List<Long> resourceRoleIds;
    /**
     * Reason for application
     */
    private String applyReason;
    /**
     * ID of the user who applied for the project
     */
    @JsonProperty(access = Access.READ_ONLY)
    private Long userId;

}
