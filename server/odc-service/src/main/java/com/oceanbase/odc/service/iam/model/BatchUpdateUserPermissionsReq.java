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
package com.oceanbase.odc.service.iam.model;

import java.util.List;

import com.oceanbase.odc.core.authority.model.SecurityResource;

import lombok.Data;

/**
 * @author gaoda.xy
 * @date 2022/12/15 17:35
 */

@Data
public class BatchUpdateUserPermissionsReq {
    /**
     * Composed of resourceType:resourceId, references {@link SecurityResource}
     */
    private String resourceIdentifier;

    /**
     * Record user and authorized action, references {@link UserAction}
     */
    private List<UserAction> userActions;

    @Data
    public static class UserAction {
        /**
         * User id
         */
        private Long userId;

        /**
         * Authorized action, include "apply", "readonlyconnect" and "connect"
         */
        private String action;
    }
}
