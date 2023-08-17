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
package com.oceanbase.odc.service.feature;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.TestConnectionUtil;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.feature.VersionDiffConfigService.OBSupport;
import com.oceanbase.odc.service.feature.model.DataTypeUnit;

public class VersionDiffConfigServiceTest extends ServiceTestEnv {

    @Autowired
    private VersionDiffConfigService versionDiffConfigService;

    @Test
    public void getDatatypeList_OracleMode() {
        ConnectionSession session = TestConnectionUtil.getTestConnectionSession(ConnectType.OB_ORACLE);
        List<DataTypeUnit> result = versionDiffConfigService.getDatatypeList(session, "column_data_type");
        Assert.assertTrue(result.size() > 0);
    }

    @Test
    public void getSupportFeatures_MysqlMode() {
        ConnectionSession session = TestConnectionUtil.getTestConnectionSession(ConnectType.OB_MYSQL);
        // mock connection config
        ConnectionConfig config = new ConnectionConfig();
        config.setId(1L);
        session.setAttribute(ConnectionSessionConstants.CONNECTION_CONFIG_KEY, config);
        List<OBSupport> result = versionDiffConfigService.getSupportFeatures(session);
        Assert.assertTrue(result.size() > 0);
    }

}
