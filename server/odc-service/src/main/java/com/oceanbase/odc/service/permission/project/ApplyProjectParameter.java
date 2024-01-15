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

package com.oceanbase.odc.service.permission.project;

import java.io.Serializable;
import java.util.List;

import com.oceanbase.odc.core.flow.model.TaskParameters;
import com.oceanbase.odc.core.shared.constant.ResourceRoleName;
import com.oceanbase.odc.service.collaboration.project.model.Project;
import com.oceanbase.odc.service.iam.model.ResourceRole;

import lombok.Data;

/**
 * @author gaoda.xy
 * @date 2023/10/12 09:58
 */
@Data
public class ApplyProjectParameter implements Serializable, TaskParameters {

    /**
     * Project to be applied for
     */
    private ApplyProject project;
    /**
     * Resource roles to be applied for
     */
    private List<ApplyResourceRole> resourceRoles;
    /**
     * Reason for application
     */
    private String applyReason;

    @Data
    public static class ApplyProject implements Serializable {
        /**
         * Project id, refer to {@link Project#getId()}
         */
        private Long id;
        /**
         * Project name, filled in by {@link ApplyProjectPreprocessor}
         */
        private String name;
    }

    @Data
    public static class ApplyResourceRole implements Serializable {
        /**
         * Resource role id, refer to {@link ResourceRole#getId()}
         */
        private Long id;
        /**
         * Resource role name, filled in by {@link ApplyProjectPreprocessor}
         */
        private ResourceRoleName name;
    }

}
