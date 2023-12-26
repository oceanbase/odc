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
package com.oceanbase.odc.service.onlineschemachange.pipeline;

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskEntity;
import com.oceanbase.odc.service.connection.ConnectionService;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeParameters;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeScheduleTaskParameters;
import com.oceanbase.odc.service.onlineschemachange.monitor.DBUserMonitorExecutor;
import com.oceanbase.odc.service.onlineschemachange.rename.DefaultRenameTableInvoker;
import com.oceanbase.odc.service.session.DBSessionManageFacade;
import com.oceanbase.odc.service.session.factory.DefaultConnectSessionFactory;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2023-06-11
 * @since 4.2.0
 */
@Slf4j
@Component
public class SwapTableNameValve extends BaseValve {
    @Autowired
    private ConnectionService connectionService;

    @Autowired
    private DBSessionManageFacade dbSessionManageFacade;

    @Override
    public void invoke(ValveContext valveContext) {
        OscValveContext context = (OscValveContext) valveContext;
        ScheduleTaskEntity scheduleTask = context.getScheduleTask();
        log.info("Start execute {}, schedule task id {}", getClass().getSimpleName(), scheduleTask.getId());

        OnlineSchemaChangeScheduleTaskParameters taskParameters = context.getTaskParameter();
        PreConditions.notNull(taskParameters, "OnlineSchemaChangeScheduleTaskParameters is null");
        OnlineSchemaChangeParameters parameters = context.getParameter();

        ConnectionConfig config = context.getConnectionConfig();
        DBUserMonitorExecutor userMonitorExecutor = new DBUserMonitorExecutor(config, parameters.getLockUsers());
        ConnectionSession connectionSession = new DefaultConnectSessionFactory(config).generateSession();
        try {
            if (enableUserMonitor(parameters.getLockUsers())) {
                userMonitorExecutor.start();
            }
            ConnectionSessionUtil.setCurrentSchema(connectionSession, taskParameters.getDatabaseName());
            DefaultRenameTableInvoker defaultRenameTableInvoker =
                    new DefaultRenameTableInvoker(connectionSession, dbSessionManageFacade);
            defaultRenameTableInvoker.invoke(taskParameters, parameters);
            context.setSwapSucceedCallBack(true);
        } finally {
            try {
                if (enableUserMonitor(parameters.getLockUsers())) {
                    userMonitorExecutor.stop();
                }
            } finally {
                connectionSession.expire();
            }
        }
    }

    private boolean enableUserMonitor(List<String> lockUsers) {
        return CollectionUtils.isNotEmpty(lockUsers);
    }
}


