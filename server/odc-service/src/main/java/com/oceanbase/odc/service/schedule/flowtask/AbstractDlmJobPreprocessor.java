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
package com.oceanbase.odc.service.schedule.flowtask;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.shared.exception.UnsupportedException;
import com.oceanbase.odc.core.sql.execute.SyncJdbcExecutor;
import com.oceanbase.odc.metadb.schedule.ScheduleEntity;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.dlm.DlmLimiterService;
import com.oceanbase.odc.service.dlm.model.DataArchiveTableConfig;
import com.oceanbase.odc.service.dlm.model.OffsetConfig;
import com.oceanbase.odc.service.dlm.model.RateLimitConfiguration;
import com.oceanbase.odc.service.dlm.utils.DataArchiveConditionUtil;
import com.oceanbase.odc.service.flow.model.CreateFlowInstanceReq;
import com.oceanbase.odc.service.flow.processor.Preprocessor;
import com.oceanbase.odc.service.schedule.model.ScheduleStatus;
import com.oceanbase.tools.dbbrowser.util.MySQLSqlBuilder;
import com.oceanbase.tools.dbbrowser.util.OracleSqlBuilder;
import com.oceanbase.tools.dbbrowser.util.SqlBuilder;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author：tinker
 * @Date: 2023/7/13 19:47
 * @Descripition:
 */

@Slf4j
public class AbstractDlmJobPreprocessor implements Preprocessor {

    @Override
    public void process(CreateFlowInstanceReq req) {}

    public ScheduleEntity buildScheduleEntity(CreateFlowInstanceReq req) {
        AlterScheduleParameters parameters = (AlterScheduleParameters) req.getParameters();
        ScheduleEntity scheduleEntity = new ScheduleEntity();
        scheduleEntity.setConnectionId(req.getConnectionId());
        scheduleEntity.setDatabaseName(req.getDatabaseName());
        scheduleEntity.setDatabaseId(req.getDatabaseId());
        scheduleEntity.setProjectId(req.getProjectId());
        scheduleEntity.setJobType(parameters.getType());
        scheduleEntity.setStatus(ScheduleStatus.APPROVING);
        scheduleEntity.setAllowConcurrent(parameters.getAllowConcurrent());
        scheduleEntity.setMisfireStrategy(parameters.getMisfireStrategy());
        scheduleEntity.setJobParametersJson(JsonUtils.toJson(parameters.getScheduleTaskParameters()));
        scheduleEntity.setTriggerConfigJson(JsonUtils.toJson(parameters.getTriggerConfig()));
        scheduleEntity.setModifierId(scheduleEntity.getCreatorId());
        scheduleEntity.setDescription(req.getDescription());
        return scheduleEntity;
    }

    public void checkTableAndCondition(ConnectionSession connectionSession, Database sourceDb,
            List<DataArchiveTableConfig> tables,
            List<OffsetConfig> variables) {
        checkShardKey(connectionSession, sourceDb.getName(), tables);
        Map<DataArchiveTableConfig, String> sqlMap = getDataArchiveSqls(sourceDb, tables, variables);
        checkDataArchiveSql(connectionSession, sqlMap);
    }


    private void checkShardKey(ConnectionSession connectionSession, String databaseName,
            List<DataArchiveTableConfig> tables) {
        SyncJdbcExecutor syncJdbcExecutor = connectionSession.getSyncJdbcExecutor(
                ConnectionSessionConstants.CONSOLE_DS_KEY);
        SqlBuilder sqlBuilder;
        if (connectionSession.getDialectType().isMysql()) {
            sqlBuilder = new MySQLSqlBuilder();
            sqlBuilder.append(
                    "SELECT TABLE_NAME from INFORMATION_SCHEMA.STATISTICS where NON_UNIQUE = 0 AND NULLABLE != 'YES' ");
            sqlBuilder.append(String.format("AND TABLE_SCHEMA='%s' GROUP BY TABLE_NAME", databaseName));
        } else {
            sqlBuilder = new OracleSqlBuilder();
            sqlBuilder.append(
                    String.format("select table_name from all_constraints where constraint_type = 'P' and owner = '%s'",
                            databaseName));
        }
        HashSet<String> tableNames =
                new HashSet<>(syncJdbcExecutor.query(sqlBuilder.toString(), (rs, num) -> rs.getString(1)));
        tables.forEach(tableConfig -> {
            if (!tableNames.contains(tableConfig.getTableName())) {
                throw new IllegalArgumentException(
                        String.format("The table need to contain a primary key!tableName=%s",
                                tableConfig.getTableName()));
            }
        });
    }

    private void checkDataArchiveSql(ConnectionSession connectionSession, Map<DataArchiveTableConfig, String> sqlMap) {
        SyncJdbcExecutor syncJdbcExecutor = connectionSession.getSyncJdbcExecutor(
                ConnectionSessionConstants.CONSOLE_DS_KEY);
        // Ensure the conditions are valid when executing.
        sqlMap.forEach((key, value) -> {
            try {
                syncJdbcExecutor.execute("explain " + value);
            } catch (Exception e) {
                log.warn("Test condition failed,sql={}", value, e);
                throw new IllegalArgumentException(String.format("Condition is not supported!TableName=%s,Condition=%s",
                        key.getTableName(), key.getConditionExpression()));
            }
        });
    }

    public Map<DataArchiveTableConfig, String> getDataArchiveSqls(Database database,
            List<DataArchiveTableConfig> tables,
            List<OffsetConfig> variables) {
        Map<DataArchiveTableConfig, String> sqlMap = new HashMap<>();
        tables.forEach(table -> sqlMap.put(table, generateTestSql(database, table, variables)));
        return sqlMap;
    }

    private String generateTestSql(Database database, DataArchiveTableConfig table, List<OffsetConfig> variables) {
        try {
            DialectType dbType = database.getDataSource().getDialectType();
            if (!dbType.isOracle() && !dbType.isMysql()) {
                throw new UnsupportedException();
            }
            SqlBuilder sqlBuilder = dbType.isMysql() ? new MySQLSqlBuilder() : new OracleSqlBuilder();
            sqlBuilder.append("SELECT 1 FROM ").identifier(database.getName(), table.getTableName());
            if (StringUtils.isNotEmpty(table.getConditionExpression())) {
                sqlBuilder.append(" WHERE ")
                        .append(DataArchiveConditionUtil.parseCondition(table.getConditionExpression(),
                                variables, new Date()));
            }
            return sqlBuilder.toString();
        } catch (Exception e) {
            throw new IllegalArgumentException(String.format("Parse condition error,message=%s", e.getMessage()));
        }
    }

    protected void initLimiterConfig(Long scheduleId, RateLimitConfiguration limiterConfig,
            DlmLimiterService limiterService) {
        RateLimitConfiguration defaultLimiterConfig = limiterService.getDefaultLimiterConfig();
        if (limiterConfig.getRowLimit() == null) {
            limiterConfig.setRowLimit(defaultLimiterConfig.getRowLimit());
        }
        if (limiterConfig.getDataSizeLimit() == null) {
            limiterConfig.setDataSizeLimit(defaultLimiterConfig.getDataSizeLimit());
        }
        if (limiterConfig.getBatchSize() == null) {
            limiterConfig.setBatchSize(defaultLimiterConfig.getBatchSize());
        }
        limiterConfig.setOrderId(scheduleId);
        limiterService.create(limiterConfig);
    }

}
