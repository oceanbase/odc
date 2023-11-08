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
package com.oceanbase.odc.service.onlineschemachange;

import java.text.MessageFormat;

import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.shared.constant.TaskErrorStrategy;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeParameters;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeSqlType;
import com.oceanbase.odc.service.onlineschemachange.model.OriginTableCleanStrategy;

/**
 * @author yaobin
 * @date 2023-07-17
 * @since 4.2.0
 */
public abstract class OBMySqlOscTestEnv extends BaseOscTestEnv {

    @Override
    protected DialectType getDialectType() {
        return DialectType.OB_MYSQL;
    }

    protected void createTableForTask(String tableName) {
        String createTemplate = "create table if not exists {0} (id int(20) primary key, name1 varchar(20))";
        jdbcTemplate.execute(MessageFormat.format(createTemplate, tableName));
    }

    protected void dropTableForTask(String tableName) {
        String dropTemplate = "drop table if exists {0}";
        jdbcTemplate.execute(MessageFormat.format(dropTemplate, tableName));
    }

    protected void createTableForMultiTask() {
        createTableForTask("t1");
        createTableForTask("t2");
    }

    protected void dropTableForMultiTask() {
        dropTableForTask("t1");
        dropTableForTask("t2");
    }

    protected OnlineSchemaChangeParameters getOnlineSchemaChangeParameters() {
        OnlineSchemaChangeParameters changeParameters = new OnlineSchemaChangeParameters();
        changeParameters.setSwapTableNameRetryTimes(3);
        changeParameters.setSqlType(OnlineSchemaChangeSqlType.CREATE);
        changeParameters.setErrorStrategy(TaskErrorStrategy.ABORT);
        changeParameters.setOriginTableCleanStrategy(OriginTableCleanStrategy.ORIGIN_TABLE_RENAME_AND_RESERVED);
        changeParameters.setSqlContent("create table t1 (id int(20) primary key, name1 varchar(20));"
                + "create table t2 (id int(20) primary key, name1 varchar(20));");
        return changeParameters;
    }


}
