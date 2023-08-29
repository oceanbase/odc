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
package com.oceanbase.odc.service.partitionplan;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.metadb.partitionplan.DatabasePartitionPlanRepository;
import com.oceanbase.odc.metadb.partitionplan.TablePartitionPlanEntity;
import com.oceanbase.odc.metadb.partitionplan.TablePartitionPlanRepository;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.db.browser.DBSchemaAccessors;
import com.oceanbase.odc.service.flow.FlowInstanceService;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.session.factory.DefaultConnectSessionFactory;
import com.oceanbase.tools.dbbrowser.model.DBTable;
import com.oceanbase.tools.dbbrowser.model.DBTablePartition;
import com.oceanbase.tools.dbbrowser.model.DBTablePartitionDefinition;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;
import com.oceanbase.tools.dbbrowser.util.MySQLSqlBuilder;
import com.oceanbase.tools.dbbrowser.util.SqlBuilder;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author：tianke
 * @Date: 2022/10/8 16:46
 * @Descripition:
 */

@Service
@Slf4j
@SkipAuthorize("permission check inside getForConnect")
public class PartitionPlanTaskService {

    @Autowired
    private TablePartitionPlanRepository tablePartitionPlanRepository;
    @Autowired
    private DatabasePartitionPlanRepository databasePartitionPlanRepository;
    @Autowired
    private FlowInstanceService flowInstanceService;
    @Autowired
    private DatabaseService databaseService;

    @Autowired
    private AuthenticationFacade authenticationFacade;

    public void executePartitionPlan(Long flowInstanceId, List<TablePartitionPlanEntity> tablePlans)
            throws Exception {
        Set<Long> databaseIds = tablePlans.stream().map(TablePartitionPlanEntity::getDatabaseId).collect(
                Collectors.toSet());
        Optional<Long> databaseId = databaseIds.stream().findFirst();
        if (!databaseId.isPresent() || databaseIds.size() != 1) {
            log.warn("Table plans belongs to multi database,its not allow here.");
            return;
        }
        ConnectionConfig conn = databaseService.findDataSourceForConnectById(databaseId.get());
        DefaultConnectSessionFactory factory = new DefaultConnectSessionFactory(conn);
        ConnectionSession connectionSession = factory.generateSession();
        try {
            DBSchemaAccessor accessor = DBSchemaAccessors.create(connectionSession);
            // Task 1:生成 ADD PARTITION DDL，并发起数据库变更流程
            List<String> addPartitionSqls = createAddPartitionDDL(accessor, tablePlans);
            if (!addPartitionSqls.isEmpty()) {
                PartitionPlanSubFlowThread partitionPlanSubFlowThread =
                        new PartitionPlanSubFlowThread(flowInstanceId,
                                databaseId.get(), addPartitionSqls, flowInstanceService,
                                authenticationFacade.currentUser());
                partitionPlanSubFlowThread.start();
            }
            // Task 2:查找过期分区，并发起数据库变更流程
            List<String> dropPartitionSqls = createDropPartitionDDL(accessor, tablePlans);
            if (!dropPartitionSqls.isEmpty()) {
                PartitionPlanSubFlowThread partitionPlanSubFlowThread =
                        new PartitionPlanSubFlowThread(flowInstanceId,
                                databaseId.get(), dropPartitionSqls, flowInstanceService,
                                authenticationFacade.currentUser());
                partitionPlanSubFlowThread.start();
            }
        } finally {
            try {
                connectionSession.expire();
            } catch (Exception e) {
                // eat exception
            }
        }
    }

    /**
     * 生成 Add 分区 DDL
     */
    private List<String> createAddPartitionDDL(DBSchemaAccessor accessor, List<TablePartitionPlanEntity> tablePlans)
            throws ParseException {
        List<String> sqls = new LinkedList<>();
        long baseDate = System.currentTimeMillis();
        for (TablePartitionPlanEntity tablePlan : tablePlans) {
            // 查询表分区
            DBTable table = getTable(accessor, tablePlan.getSchemaName(), tablePlan.getTableName());
            List<DBTablePartitionDefinition> definitions = table.getPartition().getPartitionDefinitions();
            // 分区计划生效中，但表被删除
            if (definitions.isEmpty()) {
                log.warn("No partition found,table={}.{}", tablePlan.getSchemaName(), tablePlan.getTableName());
                continue;
            }
            // 获取最右边界分区
            List<String> maxValues = definitions.get(definitions.size() - 1).getMaxValues();
            if (CollectionUtils.isEmpty(maxValues) || Objects.isNull(maxValues.get(0))
                    || "MAXVALUE".equals(maxValues.get(0))) {
                log.warn("Invalid partition max right bound:{}", maxValues);
                continue;
            }
            // 计算预创建分区最大右边界
            long maxRightBound = PartitionPlanFunction.getPartitionRightBound(baseDate,
                    tablePlan.getPreCreatePartitionCount() * tablePlan.getPartitionInterval(),
                    tablePlan.getPartitionIntervalUnit());
            PartitionExpressionType expressionType = PartitionPlanFunction.getPartitionExpressionType(table);
            switch (expressionType) {
                case DATE: {
                    sqls.addAll(getCreateSqlForDateRangePartition(baseDate, maxRightBound, table.getPartition(),
                            tablePlan));
                    break;
                }
                case UNIX_TIMESTAMP: {
                    sqls.addAll(
                            getCreateSqlForUnixTimeStampRangePartition(baseDate, maxRightBound, table.getPartition(),
                                    tablePlan));
                    break;
                }
                case OTHER: {
                    log.warn("Unsupported partition expression!{}.{}:{}", table.getSchemaName(),
                            table.getName(), table.getPartition().getPartitionOption().getExpression());
                    break;
                }
                default: {
                    break;
                }
            }
        }
        return sqls;
    }

    private List<String> getCreateSqlForDateRangePartition(long baseDate, long maxRightBound,
            DBTablePartition partition, TablePartitionPlanEntity partitionPlan)
            throws ParseException {
        List<DBTablePartitionDefinition> definitions = partition.getPartitionDefinitions();
        DBTablePartitionDefinition right = definitions.get(definitions.size() - 1);
        String maxValue = right.getMaxValues().get(0);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        long currentRightBound = Math.max(sdf.parse(maxValue.substring(1, maxValue.length() - 1)).getTime(), baseDate);
        List<String> returnValue = new LinkedList<>();
        while (currentRightBound < maxRightBound) {
            currentRightBound = PartitionPlanFunction.getPartitionRightBound(currentRightBound,
                    partitionPlan.getPartitionInterval(),
                    partitionPlan.getPartitionIntervalUnit());
            right.setName(partitionPlan.getPartitionNamingPrefix()
                    + new SimpleDateFormat(partitionPlan.getPartitionNamingSuffixExpression())
                            .format(currentRightBound));
            right.getMaxValues().set(0, String.format("'%s'", sdf.format(new Date(currentRightBound))));
            returnValue.add(getCreateSql(partition));
        }
        return returnValue;
    }

    private List<String> getCreateSqlForUnixTimeStampRangePartition(long baseDate, long maxRightBound,
            DBTablePartition partition, TablePartitionPlanEntity partitionPlan) {
        List<DBTablePartitionDefinition> definitions = partition.getPartitionDefinitions();
        DBTablePartitionDefinition right = definitions.get(definitions.size() - 1);
        long currentRightBound = Math.max(Long.parseLong(right.getMaxValues().get(0)) * 1000, baseDate);
        List<String> returnValue = new LinkedList<>();
        while (currentRightBound < maxRightBound) {
            currentRightBound = PartitionPlanFunction.getPartitionRightBound(currentRightBound,
                    partitionPlan.getPartitionInterval(),
                    partitionPlan.getPartitionIntervalUnit());
            right.setName(partitionPlan.getPartitionNamingPrefix() +
                    new SimpleDateFormat(partitionPlan.getPartitionNamingSuffixExpression())
                            .format(currentRightBound));
            right.getMaxValues().set(0, String.valueOf(currentRightBound / 1000));
            returnValue.add(getCreateSql(partition));
        }
        return returnValue;
    }

    /**
     * 生成 Drop 分区 DDL
     */
    private List<String> createDropPartitionDDL(DBSchemaAccessor accessor, List<TablePartitionPlanEntity> tablePlans)
            throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        List<String> sqls = new LinkedList<>();
        long baseDate = System.currentTimeMillis();
        for (TablePartitionPlanEntity tablePlan : tablePlans) {
            // 查找表所有分区
            DBTable table = getTable(accessor, tablePlan.getSchemaName(), tablePlan.getTableName());
            List<DBTablePartitionDefinition> definitions = table.getPartition().getPartitionDefinitions();
            if (definitions.isEmpty()) {
                continue;
            }
            PartitionExpressionType expressionType = PartitionPlanFunction.getPartitionExpressionType(table);
            // 查询结果按升序排列，从左开始查找到第一个未过期的分区则停止
            for (DBTablePartitionDefinition definition : definitions) {
                if (expressionType == PartitionExpressionType.OTHER) {
                    log.warn("Unsupported partition expression!{}.{}:{}", table.getSchemaName(),
                            table.getName(), table.getPartition().getPartitionOption().getExpression());
                    break;
                }
                String maxValue = definition.getMaxValues().get(0);
                long partitionRightBound =
                        expressionType == PartitionExpressionType.UNIX_TIMESTAMP ? Long.parseLong(maxValue) * 1000
                                : sdf.parse(maxValue.substring(1, maxValue.length() - 1)).getTime();
                if (!PartitionPlanFunction.isExpirePartition(baseDate, partitionRightBound, tablePlan.getExpirePeriod(),
                        tablePlan.getExpirePeriodUnit())) {
                    break;
                }
                sqls.add(getDeleteSql(table.getSchemaName(), table.getName(), definition.getName()));
            }
        }
        return sqls;
    }

    private String getCreateSql(DBTablePartition partition) {
        List<DBTablePartitionDefinition> definitions = partition.getPartitionDefinitions();
        DBTablePartitionDefinition right = definitions.get(definitions.size() - 1);
        SqlBuilder sqlBuilder = new MySQLSqlBuilder();
        sqlBuilder.append("alter table ").identifier(partition.getSchemaName(), partition.getTableName()).append(
                " add partition (partition ")
                .identifier(right.getName()).append(" values less than (")
                .append(right.getMaxValues().get(0))
                .append("));");
        return sqlBuilder.toString();
    }

    private String getDeleteSql(String schema, String table, String part) {
        SqlBuilder sqlBuilder = new MySQLSqlBuilder();
        sqlBuilder.append("alter table ").identifier(schema, table).append(" drop partition (").append(part)
                .append(");");
        return sqlBuilder.toString();
    }

    private DBTable getTable(DBSchemaAccessor accessor, String schemaName, String tableName) {
        DBTable table = new DBTable();
        table.setSchemaName(schemaName);
        table.setName(tableName);
        DBTablePartition partition = accessor.getPartition(schemaName, tableName);
        partition.setTableName(tableName);
        partition.setSchemaName(schemaName);
        table.setPartition(partition);
        table.setColumns(accessor.listTableColumns(schemaName, tableName));
        return table;
    }
}
