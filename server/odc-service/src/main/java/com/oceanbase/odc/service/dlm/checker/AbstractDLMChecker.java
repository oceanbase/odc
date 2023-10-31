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

package com.oceanbase.odc.service.dlm.checker;

import java.util.List;

import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionFactory;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.dlm.model.DataArchiveTableConfig;
import com.oceanbase.odc.service.dlm.model.OffsetConfig;
import com.oceanbase.odc.service.session.factory.DefaultConnectSessionFactory;

/**
 * @Author：tinker
 * @Date: 2023/10/30 16:09
 * @Descripition:
 */
public abstract class AbstractDLMChecker implements DLMChecker {

    public Database database;

    private ConnectionSession session;

    public AbstractDLMChecker(Database database) {
        this.database = database;
    }

    public ConnectionSession getConnectionSession() {
        if (session == null || session.isExpired()) {
            ConnectionConfig dataSource = database.getDataSource();
            dataSource.setDefaultSchema(database.getName());
            ConnectionSessionFactory sourceSessionFactory = new DefaultConnectSessionFactory(dataSource);
            session = sourceSessionFactory.generateSession();
        }
        return session;
    }

    public void dealloc() {
        if (session != null && !session.isExpired()) {
            session.expire();
        }
    }

    @Override
    public void checkSysTenantUser() {

    }

    @Override
    public void checkTargetDbType(DialectType dbType) {

    }

    @Override
    public void checkTablesPrimaryKey(List<DataArchiveTableConfig> tables) {

    }

    @Override
    public void checkDLMTableCondition(List<DataArchiveTableConfig> tables, List<OffsetConfig> variables) {

    }
}
