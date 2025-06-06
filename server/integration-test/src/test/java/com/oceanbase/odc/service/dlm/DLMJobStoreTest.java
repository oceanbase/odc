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
package com.oceanbase.odc.service.dlm;

import java.sql.SQLException;

import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.tools.migrator.common.dto.TaskGenerator;
import com.oceanbase.tools.migrator.common.util.ResourceIdUtils;

public class DLMJobStoreTest {

    public static void main(String[] args) throws SQLException {
        ConnectionConfig connectionConfig = new ConnectionConfig();
        connectionConfig.setDefaultSchema("tianke434");
        connectionConfig.setPassword("aaAA11__");
        connectionConfig.setHost("11.124.5.137");
        connectionConfig.setPort(2883);
        connectionConfig.setUsername("root@dlm_test#gaoda_rd");
        connectionConfig.setType(ConnectType.OB_MYSQL);

        DLMJobStore dlmJobStore = new DLMJobStore(connectionConfig);
        TaskGenerator taskGenerator = new TaskGenerator();
        taskGenerator.setId(ResourceIdUtils.getResourceId("g"));
        taskGenerator.setJobId("MIGRATOR-1");
        dlmJobStore.storeTaskGenerator(taskGenerator);
    }

    public void store() throws SQLException {
        ConnectionConfig connectionConfig = new ConnectionConfig();
        connectionConfig.setDefaultSchema("tianke434");
        connectionConfig.setPassword("aaAA11__");
        connectionConfig.setHost("11.124.5.137");
        connectionConfig.setPort(2883);
        connectionConfig.setUsername("root");
        connectionConfig.setType(ConnectType.OB_MYSQL);

        DLMJobStore dlmJobStore = new DLMJobStore(connectionConfig);
        TaskGenerator taskGenerator = new TaskGenerator();
        taskGenerator.setId(ResourceIdUtils.getResourceId("g"));
        taskGenerator.setJobId("MIGRATOR-1");
        dlmJobStore.storeTaskGenerator(taskGenerator);
    }
}
