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
package com.oceanbase.odc.service.db.schema;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Service;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.service.connection.ConnectionService;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.db.browser.DBSchemaAccessors;
import com.oceanbase.odc.service.db.schema.synchronizer.DBSchemaSyncer;
import com.oceanbase.odc.service.session.factory.DefaultConnectSessionFactory;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;

import lombok.NonNull;

/**
 * @author gaoda.xy
 * @date 2024/4/10 20:56
 */
@Service
@SkipAuthorize("odc internal usage")
public class DBSchemaSyncService {

    @Autowired
    private ListableBeanFactory beanFactory;

    @Autowired
    private DatabaseService databaseService;

    @Autowired
    private ConnectionService connectionService;

    private List<DBSchemaSyncer> syncers;

    @PostConstruct
    public void init() {
        Map<String, DBSchemaSyncer> beans = beanFactory.getBeansOfType(DBSchemaSyncer.class);
        List<DBSchemaSyncer> implementations = new ArrayList<>(beans.values());
        implementations.sort(Comparator.comparingInt(Ordered::getOrder));
        this.syncers = implementations;
    }

    public void sync(@NonNull Long databaseId) {
        Database database = databaseService.getBasicSkipPermissionCheck(databaseId);
        PreConditions.notNull(database.getDataSource(), "database.dataSource");
        ConnectionConfig config =
                connectionService.getForConnectionSkipPermissionCheck(database.getDataSource().getId());
        ConnectionSession session = new DefaultConnectSessionFactory(config).generateSession();
        try {
            DBSchemaAccessor accessor = DBSchemaAccessors.create(session);
            for (DBSchemaSyncer syncer : syncers) {
                if (syncer.support(config.getDialectType())) {
                    syncer.sync(accessor, database);
                }
            }
        } finally {
            session.expire();
        }
    }

}
