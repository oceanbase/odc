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
package com.oceanbase.odc.service.onlineschemachange.model;

import java.io.Serializable;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.oceanbase.odc.core.flow.model.TaskParameters;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.constant.TaskErrorStrategy;
import com.oceanbase.odc.core.shared.exception.UnexpectedException;
import com.oceanbase.odc.core.shared.model.TableIdentity;
import com.oceanbase.odc.core.sql.split.OffsetString;
import com.oceanbase.odc.service.common.util.SqlUtils;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.onlineschemachange.ddl.DdlUtils;
import com.oceanbase.odc.service.onlineschemachange.ddl.OBMysqlTableNameReplacer;
import com.oceanbase.odc.service.onlineschemachange.ddl.OBOracleTableNameReplacer;
import com.oceanbase.odc.service.onlineschemachange.ddl.OscFactoryWrapper;
import com.oceanbase.odc.service.onlineschemachange.ddl.OscFactoryWrapperGenerator;
import com.oceanbase.odc.service.onlineschemachange.ddl.ReplaceElement;
import com.oceanbase.odc.service.onlineschemachange.ddl.ReplaceResult;
import com.oceanbase.odc.service.onlineschemachange.ddl.TableNameDescriptor;
import com.oceanbase.odc.service.onlineschemachange.ddl.TableNameReplacer;
import com.oceanbase.odc.service.onlineschemachange.subtask.SubTaskParameterFactory;
import com.oceanbase.tools.sqlparser.statement.Statement;
import com.oceanbase.tools.sqlparser.statement.alter.table.AlterTable;
import com.oceanbase.tools.sqlparser.statement.createindex.CreateIndex;
import com.oceanbase.tools.sqlparser.statement.createtable.CreateTable;

import lombok.Data;

/**
 * Received parameters from user
 *
 * @author yaobin
 * @since 4.2.0
 */
@Data
public class OnlineSchemaChangeParameters implements Serializable, TaskParameters {
    private static final long serialVersionUID = 2870979595720162565L;

    private OnlineSchemaChangeSqlType sqlType;

    private String sqlContent;
    private TaskErrorStrategy errorStrategy;

    private Integer lockTableTimeOutSeconds;

    private Integer swapTableNameRetryTimes;

    private OriginTableCleanStrategy originTableCleanStrategy;

    private String delimiter = ";";

    private List<String> lockUsers;
    private SwapTableType swapTableType;
    private Long flowInstanceId;
    private TransferConfig fullTransfer = new TransferConfig();
    private TransferConfig incrTransfer = new TransferConfig();

    public boolean isContinueOnError() {
        return this.errorStrategy == TaskErrorStrategy.CONTINUE;
    }

    public List<OnlineSchemaChangeScheduleTaskParameters> generateSubTaskParameters(ConnectionConfig connectionConfig,
            String schema) {
        List<String> sqls =
                SqlUtils.splitWithOffset(connectionConfig.getDialectType(), this.sqlContent + "\n", this.delimiter,
                        true)
                        .stream().map(OffsetString::getStr).collect(Collectors.toList());;

        OscFactoryWrapper oscFactoryWrapper = OscFactoryWrapperGenerator.generate(connectionConfig.getDialectType());
        try (SubTaskParameterFactory subTaskParameterFactory =
                new SubTaskParameterFactory(connectionConfig, schema, oscFactoryWrapper)) {
            Map<TableIdentity, OnlineSchemaChangeScheduleTaskParameters> taskParameters = new LinkedHashMap<>();
            for (String sql : sqls) {
                Statement statement = oscFactoryWrapper.getSqlParser().parse(new StringReader(sql));
                if (statement instanceof CreateTable || statement instanceof AlterTable) {
                    OnlineSchemaChangeScheduleTaskParameters parameter =
                            subTaskParameterFactory.generate(sql, sqlType, statement);
                    TableNameDescriptor tableNameDescriptor = oscFactoryWrapper.getTableNameDescriptorFactory()
                            .getTableNameDescriptor(parameter.getOriginTableName());
                    TableIdentity key = new TableIdentity(DdlUtils.getUnwrappedName(parameter.getDatabaseName()),
                            tableNameDescriptor.getOriginTableNameUnwrapped());
                    taskParameters.putIfAbsent(key, parameter);

                    if (sqlType == OnlineSchemaChangeSqlType.ALTER) {
                        if (statement instanceof AlterTable && connectionConfig.getDialectType().isOracle()) {
                            List<ReplaceElement> replaceElements = parameter.getReplaceResult().getReplaceElements();
                            ReplaceResult result = new OBOracleTableNameReplacer().replaceStmtValue(
                                    OnlineSchemaChangeSqlType.ALTER, sql, replaceElements);
                            taskParameters.get(key).getSqlsToBeExecuted().add(result.getNewSql());
                        } else {
                            String newAlterStmt = DdlUtils.replaceTableName(sql, parameter.getNewTableName(),
                                    connectionConfig.getDialectType(), sqlType).getNewSql();
                            taskParameters.get(key).getSqlsToBeExecuted().add(newAlterStmt);
                        }
                    }

                } else {
                    PreConditions.validArgumentState(statement instanceof CreateIndex, ErrorCodes.IllegalArgument,
                            new Object[] {}, "statement is not CreateIndex");
                    CreateIndex createIndex = (CreateIndex) statement;
                    String tableName = createIndex.getOn().getRelation();
                    TableNameDescriptor tableNameDescriptor = oscFactoryWrapper.getTableNameDescriptorFactory()
                            .getTableNameDescriptor(tableName);
                    TableIdentity key = new TableIdentity(DdlUtils.getUnwrappedName(schema),
                            tableNameDescriptor.getOriginTableNameUnwrapped());
                    TableNameReplacer rewriter =
                            connectionConfig.getDialectType().isMysql() ? new OBMysqlTableNameReplacer()
                                    : new OBOracleTableNameReplacer();

                    String createIndexOnNewTable = rewriter.replaceCreateIndexStmt(
                            sql, tableNameDescriptor.getNewTableName());

                    String displaySql = taskParameters.get(key).getNewTableCreateDdlForDisplay();
                    taskParameters.get(key).setNewTableCreateDdlForDisplay(displaySql + "\n" + sql);
                    taskParameters.get(key).getSqlsToBeExecuted().add(createIndexOnNewTable);
                }
            }
            return new ArrayList<>(taskParameters.values());
        } catch (Exception e) {
            throw new UnexpectedException("Failed to generate subtasks with parameter: " + this, e);
        }
    }

}
