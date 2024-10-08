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
package com.oceanbase.odc.service.workspace.adapter;

import java.util.Set;

import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.workspace.model.CreateWorkspaceReq;
import com.oceanbase.odc.service.workspace.model.UpdateWorkspaceInstanceAccountReq;

/**
 * @author keyang
 * @date 2024/09/19
 * @since 4.3.2
 */
public interface WorkspaceDataSourceAdapter {
    ConnectionConfig createDataSource(CreateWorkspaceReq req);

    ConnectionConfig updateDataSource(Long connectionConfigId, UpdateWorkspaceInstanceAccountReq req);

    void deleteDataSources(Set<Long> connectionConfigIds);

    void testConnectivity(CreateWorkspaceReq req);

    void testConnectivityForUpdate(Long connectionConfigId, UpdateWorkspaceInstanceAccountReq req);
}
