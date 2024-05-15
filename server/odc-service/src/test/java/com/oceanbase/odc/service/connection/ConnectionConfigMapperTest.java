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
package com.oceanbase.odc.service.connection;

import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.odc.core.shared.constant.Cipher;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.core.shared.constant.ConnectionVisibleScope;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.metadb.connection.ConnectionEntity;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.connection.model.ConnectionConfig.SSLConfig;
import com.oceanbase.odc.service.connection.util.ConnectionMapper;

public class ConnectionConfigMapperTest {
    private ConnectionMapper MAPPER = ConnectionMapper.INSTANCE;

    @Test
    public void entityToModel() {
        ConnectionEntity entity = new ConnectionEntity();
        entity.setId(1L);
        entity.setQueryTimeoutSeconds(10);
        entity.setEnabled(true);
        entity.setPasswordSaved(true);
        entity.setCipher(Cipher.AES256SALT);
        entity.setVisibleScope(ConnectionVisibleScope.ORGANIZATION);
        entity.setTemp(false);
        entity.setEnvironmentId(1L);
        entity.setSslEnabled(false);
        entity.setSslCACertObjectId("1");
        entity.setSslClientCertObjectId("2");
        entity.setSslClientKeyObjectId("3");


        ConnectionConfig expected = new ConnectionConfig();
        expected.setId(1L);
        expected.setQueryTimeoutSeconds(10);
        expected.setEnabled(true);
        expected.setPasswordSaved(true);
        expected.setCipher(Cipher.AES256SALT);
        expected.setVisibleScope(ConnectionVisibleScope.ORGANIZATION);
        expected.setTemp(false);
        expected.setEnvironmentId(1L);
        expected.setSslConfig(sslConfig());
        ConnectionConfig connection = MAPPER.entityToModel(entity);

        Assert.assertEquals(expected, connection);
    }

    @Test
    public void modelToEntity() {
        ConnectionConfig connection = new ConnectionConfig();
        connection.setType(ConnectType.OB_MYSQL);
        connection.setSslConfig(sslConfig());
        connection.setEnvironmentId(1L);
        connection.setId(1L);
        connection.setQueryTimeoutSeconds(10);
        connection.setEnabled(true);
        connection.setPasswordSaved(true);
        connection.setCipher(Cipher.AES256SALT);
        connection.setVisibleScope(ConnectionVisibleScope.ORGANIZATION);
        connection.setSetTop(false);
        connection.setTemp(false);

        ConnectionEntity expected = new ConnectionEntity();
        expected.setType(ConnectType.OB_MYSQL);
        expected.setDialectType(DialectType.OB_MYSQL);
        expected.setDefaultSchema("information_schema");
        expected.setEnvironmentId(1L);
        expected.setSslEnabled(false);
        expected.setSslCACertObjectId("1");
        expected.setSslClientCertObjectId("2");
        expected.setSslClientKeyObjectId("3");
        expected.setId(1L);
        expected.setQueryTimeoutSeconds(10);
        expected.setEnabled(true);
        expected.setPasswordSaved(true);
        expected.setCipher(Cipher.AES256SALT);
        expected.setVisibleScope(ConnectionVisibleScope.ORGANIZATION);
        expected.setTemp(false);

        Assert.assertEquals(expected, MAPPER.modelToEntity(connection));
    }

    private SSLConfig sslConfig() {
        SSLConfig sslConfig = new SSLConfig();
        sslConfig.setEnabled(false);
        sslConfig.setCACertObjectId("1");
        sslConfig.setClientCertObjectId("2");
        sslConfig.setClientKeyObjectId("3");
        return sslConfig;
    }
}
