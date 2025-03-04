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
package com.oceanbase.odc.service.schedule.archiverist.model;

import com.oceanbase.odc.common.security.EncryptAlgorithm;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.exporter.model.Encryptable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExportedDataSource implements Encryptable {
    private String cloudProvider;
    private String instanceId;
    private String tenantId;
    private String host;
    private Integer port;
    private String username;
    private String password;

    public static ExportedDataSource fromConnectionConfig(ConnectionConfig dataSource) {
        return new ExportedDataSource(dataSource.getCloudProvider(), dataSource.getClusterName(),
                dataSource.getTenantName(),
                dataSource.getHost(), dataSource.getPort(), dataSource.getUsername(), dataSource.getPassword());
    }

    @Override
    public void encrypt(String encryptKey) {
        this.password = EncryptAlgorithm.AES.encrypt(this.password, encryptKey, "UTF-8");
    }

    @Override
    public void decrypt(String encryptKey) {
        this.password = EncryptAlgorithm.AES.decrypt(this.password, encryptKey, "UTF-8");

    }
}
