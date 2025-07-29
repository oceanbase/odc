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
package com.oceanbase.odc.core.shared.constant;

/**
 * @author xiangmu
 * @date 2025/07/17
 *
 *       Search type for {@code fuzzySearchKeyword}
 */
public enum SearchType {
    /**
     * For flow ID
     */
    ID,
    /**
     * For flow description
     */
    DESCRIPTION,
    /**
     * For flow candidate approvers
     */
    CANDIDATE_APPROVER_NAME,
    /**
     * For flow creators
     */
    CREATOR_NAME,
    /**
     * For flow database name
     */
    DATABASE_NAME,
    /**
     * For flow datasource name
     */
    DATASOURCE_NAME,
    /**
     * For flow cluster name
     */
    CLUSTER_NAME,
    /**
     * For flow tenant name
     */
    TENANT_NAME;
}
