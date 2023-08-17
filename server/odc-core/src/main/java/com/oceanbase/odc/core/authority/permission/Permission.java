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
package com.oceanbase.odc.core.authority.permission;

/**
 * Permission object, used to describe a permission. {@link Permission} is used to indicate whether
 * the user has the right to perform a certain operation on a certain resource.
 *
 * Of course, this is a more ideal state. For simplicity, {@link Permission} indicate whether a user
 * has the right to do something.
 *
 * @author yh263208
 * @date 2021-07-12 16:08
 * @since ODC_release_3.2.0
 */
public interface Permission {
    /**
     * There are some partial order relationships between two {@link Permission}.
     *
     * For example, write permission may imply read permission, which means that If a user has write
     * permission, then he must have read permission.
     *
     * @param permission target permission
     * @return flag to indicate the partial order relationship
     */
    boolean implies(Permission permission);

}
