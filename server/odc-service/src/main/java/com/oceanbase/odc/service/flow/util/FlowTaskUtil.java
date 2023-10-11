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
package com.oceanbase.odc.service.flow.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.flowable.engine.delegate.DelegateExecution;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.shared.exception.UnsupportedException;
import com.oceanbase.odc.core.shared.exception.VerifyException;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.connection.model.OBTenantEndpoint;
import com.oceanbase.odc.service.connection.model.OceanBaseAccessMode;
import com.oceanbase.odc.service.datatransfer.model.DataTransferConfig;
import com.oceanbase.odc.service.db.browser.DBSchemaAccessors;
import com.oceanbase.odc.service.flow.task.model.DatabaseChangeParameters;
import com.oceanbase.odc.service.flow.task.model.MockProperties;
import com.oceanbase.odc.service.flow.task.model.MockTaskConfig;
import com.oceanbase.odc.service.flow.task.model.RuntimeTaskConstants;
import com.oceanbase.odc.service.flow.task.model.ShadowTableSyncTaskParameter;
import com.oceanbase.odc.service.flow.task.util.MockDataTypeUtil;
import com.oceanbase.odc.service.iam.model.User;
import com.oceanbase.odc.service.integration.model.TemplateVariables;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeParameters;
import com.oceanbase.odc.service.partitionplan.model.PartitionPlanTaskParameters;
import com.oceanbase.odc.service.regulation.risklevel.model.RiskLevelDescriber;
import com.oceanbase.odc.service.resultset.ResultSetExportTaskParameter;
import com.oceanbase.odc.service.schedule.flowtask.AlterScheduleParameters;
import com.oceanbase.odc.service.session.factory.DefaultConnectSessionFactory;
import com.oceanbase.odc.service.task.model.ExecutorInfo;
import com.oceanbase.tools.datamocker.model.config.impl.DefaultColumnConfig;
import com.oceanbase.tools.datamocker.model.config.impl.DefaultTableConfig;
import com.oceanbase.tools.datamocker.model.config.impl.DefaultTaskConfig;
import com.oceanbase.tools.datamocker.model.config.model.DataBaseConfig;
import com.oceanbase.tools.datamocker.model.enums.ObModeType;
import com.oceanbase.tools.dbbrowser.model.DBTable.DBTableOptions;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @author wenniu.ly
 * @date 2022/3/2
 */
@Slf4j
public class FlowTaskUtil {

    public static void setParameters(@NonNull Map<String, Object> variables, @NonNull String parametersJson) {
        variables.put(RuntimeTaskConstants.PARAMETERS, parametersJson);
    }

    public static DatabaseChangeParameters getAsyncParameter(@NonNull DelegateExecution execution) {
        return internalGetParameter(execution, DatabaseChangeParameters.class).orElseThrow(
                () -> new VerifyException("OdcAsyncTaskParameters is absent"));
    }

    public static DataTransferConfig getDataTransferParameter(@NonNull DelegateExecution execution) {
        return internalGetParameter(execution, DataTransferConfig.class).orElseThrow(
                () -> new VerifyException("DataTransferConfig is absent"));
    }

    public static MockTaskConfig getMockParameter(@NonNull DelegateExecution execution) {
        return internalGetParameter(execution, MockTaskConfig.class).orElseThrow(
                () -> new VerifyException("MockTaskConfig is absent"));
    }

    public static PartitionPlanTaskParameters getPartitionPlanParameter(@NonNull DelegateExecution execution) {
        return internalGetParameter(execution, PartitionPlanTaskParameters.class).orElseThrow(
                () -> new VerifyException("PartitionPlan is absent"));
    }

    public static AlterScheduleParameters getAlterScheduleTaskParameters(@NonNull DelegateExecution execution) {
        return internalGetParameter(execution, AlterScheduleParameters.class).orElseThrow(
                () -> new VerifyException("AlterScheduleTaskParameter is absent"));
    }

    public static ShadowTableSyncTaskParameter getShadowTableSyncTaskParameter(@NonNull DelegateExecution execution) {
        return internalGetParameter(execution, ShadowTableSyncTaskParameter.class).orElseThrow(
                () -> new VerifyException("ShadowTableSyncTaskParameter is absent"));
    }

    public static OnlineSchemaChangeParameters getOnlineSchemaChangeParameter(@NonNull DelegateExecution execution) {
        return internalGetParameter(execution, OnlineSchemaChangeParameters.class).orElseThrow(
                () -> new VerifyException("OnlineSchemaChangeParameter is absent"));
    }

    public static ResultSetExportTaskParameter getResultSetExportTaskParameter(@NonNull DelegateExecution execution) {
        return internalGetParameter(execution, ResultSetExportTaskParameter.class).orElseThrow(
                () -> new VerifyException("ResultSetExportTaskParameter is absent"));
    }

    public static void setTaskSubmitter(@NonNull Map<String, Object> variables, ExecutorInfo submitter) {
        variables.put(RuntimeTaskConstants.TASK_SUBMITTER, submitter);
    }

    public static ExecutorInfo getTaskSubmitter(@NonNull DelegateExecution execution) {
        Object value = execution.getVariables().get(RuntimeTaskConstants.TASK_SUBMITTER);
        return internalGet(value, ExecutorInfo.class)
                .orElseThrow(() -> new VerifyException("Task submitter is absent"));
    }

    public static void setTaskCreator(@NonNull Map<String, Object> variables, @NonNull User creator) {
        variables.put(RuntimeTaskConstants.TASK_CREATOR, creator);
    }

    public static User getTaskCreator(@NonNull DelegateExecution execution) {
        Object value = execution.getVariables().get(RuntimeTaskConstants.TASK_CREATOR);
        return internalGet(value, User.class).orElseThrow(() -> new VerifyException("Task creator is absent"));
    }

    public static void setInterceptTaskId(@NonNull Map<String, Object> variables, @NonNull String interceptTaskId) {
        variables.put(RuntimeTaskConstants.INTERCEPT_TASK_ID, interceptTaskId);
    }

    public static String getInterceptTaskId(@NonNull Map<String, Object> variables) {
        Object value = variables.get(RuntimeTaskConstants.INTERCEPT_TASK_ID);
        return internalGet(value, String.class)
                .orElseThrow(() -> new VerifyException("No interceptTaskId found in intercept flow"));
    }

    public static void setInterceptSqlStatus(@NonNull Map<String, Object> variables, @NonNull String status) {
        variables.put(RuntimeTaskConstants.INTERCEPT_SQL_STATUS, status);
    }

    public static String getInterceptSqlStatus(@NonNull Map<String, Object> variables) {
        Object value = variables.get(RuntimeTaskConstants.INTERCEPT_SQL_STATUS);
        return internalGet(value, String.class)
                .orElseThrow(() -> new VerifyException("No interceptSqlStatus found in intercept flow"));
    }


    public static void setRiskLevel(@NonNull DelegateExecution delegateExecution, int riskLevel) {
        delegateExecution.setVariable(RuntimeTaskConstants.RISKLEVEL, riskLevel);
    }

    public static Integer getRiskLevel(@NonNull DelegateExecution delegateExecution) {
        return internalGet(delegateExecution.getVariable(RuntimeTaskConstants.RISKLEVEL), Integer.class).orElseThrow(
                () -> new VerifyException("RiskLevel is absent"));
    }


    public static void setExecutionExpirationInterval(@NonNull Map<String, Object> variables, long interval,
            @NonNull TimeUnit timeUnit) {
        PreConditions.notNegative(interval, "ExecutionExpirationInterval");
        long executionExpirationIntervalMilliSecs = TimeUnit.MILLISECONDS.convert(interval, timeUnit);
        variables.put(RuntimeTaskConstants.TIMEOUT_MILLI_SECONDS, executionExpirationIntervalMilliSecs);
    }

    public static void setPreCheckTaskId(@NonNull Map<String, Object> variables, @NonNull Long taskId) {
        variables.put(RuntimeTaskConstants.PRE_CHECK_TASK_ID, taskId);
    }

    public static Long getPreCheckTaskId(@NonNull DelegateExecution execution) {
        Object value = execution.getVariables().get(RuntimeTaskConstants.PRE_CHECK_TASK_ID);
        return internalGet(value, Long.class).orElseThrow(() -> new VerifyException("PreCheckTaskId is absent"));
    }

    public static void setExecutionExpirationInterval(@NonNull DelegateExecution delegateExecution, long interval,
            @NonNull TimeUnit timeUnit) {
        PreConditions.notNegative(interval, "ExecutionExpirationInterval");
        long executionExpirationIntervalMilliSecs = TimeUnit.MILLISECONDS.convert(interval, timeUnit);
        delegateExecution.setVariable(RuntimeTaskConstants.TIMEOUT_MILLI_SECONDS, executionExpirationIntervalMilliSecs);
    }

    public static Long getExecutionExpirationIntervalMillis(@NonNull DelegateExecution execution) {
        Object value = execution.getVariables().get(RuntimeTaskConstants.TIMEOUT_MILLI_SECONDS);
        return internalGet(value, Long.class).orElseThrow(
                () -> new VerifyException("ExecutionExpirationIntervalMillis is absent"));
    }

    public static void setTaskId(@NonNull Map<String, Object> variables, @NonNull Long taskId) {
        variables.put(RuntimeTaskConstants.TASK_ID, taskId);
    }

    public static Long getTaskId(@NonNull DelegateExecution execution) {
        Object value = execution.getVariables().get(RuntimeTaskConstants.TASK_ID);
        return internalGet(value, Long.class).orElseThrow(() -> new VerifyException("TaskId is absent"));
    }

    public static void setCloudMainAccountId(@NonNull Map<String, Object> variables, String cloudMainAccountId) {
        variables.put(RuntimeTaskConstants.CLOUD_MAIN_ACCOUNT_ID, cloudMainAccountId);
    }

    public static String getCloudMainAccountId(@NonNull DelegateExecution execution) {
        Object value = execution.getVariables().get(RuntimeTaskConstants.CLOUD_MAIN_ACCOUNT_ID);
        return internalGet(value, String.class).orElseThrow(() -> new VerifyException("Cloud main account is absent"));
    }

    public static void setSchemaName(@NonNull Map<String, Object> variables, @NonNull String schema) {
        variables.put(RuntimeTaskConstants.SCHEMA_NAME, schema);
    }

    public static String getSchemaName(@NonNull DelegateExecution execution) {
        Object value = execution.getVariables().get(RuntimeTaskConstants.SCHEMA_NAME);
        return internalGet(value, String.class).orElseThrow(() -> new VerifyException("Schema name is absent"));
    }

    public static void setConnectionConfig(@NonNull Map<String, Object> variables, @NonNull ConnectionConfig config) {
        variables.put(RuntimeTaskConstants.CONNECTION_CONFIG, config);
    }

    public static ConnectionConfig getConnectionConfig(@NonNull DelegateExecution execution) {
        Object value = execution.getVariables().get(RuntimeTaskConstants.CONNECTION_CONFIG);
        return internalGet(value, ConnectionConfig.class).orElseThrow(
                () -> new VerifyException("ConnectionConfig is absent"));
    }

    public static void setFlowInstanceId(@NonNull Map<String, Object> variables, @NonNull Long flowInstanceId) {
        variables.put(RuntimeTaskConstants.FLOW_INSTANCE_ID, flowInstanceId);
    }

    public static Long getFlowInstanceId(@NonNull DelegateExecution execution) {
        Object value = execution.getVariables().get(RuntimeTaskConstants.FLOW_INSTANCE_ID);
        return internalGet(value, Long.class).orElseThrow(() -> new VerifyException("FlowInstanceId is absent"));
    }

    public static void setTemplateVariables(@NonNull Map<String, Object> variables,
            @NonNull TemplateVariables templateVariables) {
        variables.put(RuntimeTaskConstants.INTEGRATION_TEMPLATE_VARIABLES, templateVariables);
    }

    public static TemplateVariables getTemplateVariables(@NonNull Map<String, Object> variables) {
        Object value = variables.get(RuntimeTaskConstants.INTEGRATION_TEMPLATE_VARIABLES);
        return internalGet(value, TemplateVariables.class).orElseThrow(
                () -> new VerifyException("TemplateVariables is absent"));
    }

    public static void setRiskLevelDescriber(@NonNull Map<String, Object> variables,
            @NonNull RiskLevelDescriber templateVariables) {
        variables.put(RuntimeTaskConstants.RISKLEVEL_DESCRIBER, templateVariables);
    }

    public static RiskLevelDescriber getRiskLevelDescriber(@NonNull DelegateExecution execution) {
        Object value = execution.getVariables().get(RuntimeTaskConstants.RISKLEVEL_DESCRIBER);
        return internalGet(value, RiskLevelDescriber.class).orElseThrow(
                () -> new VerifyException("RiskLevelDescriber is absent"));
    }


    @SuppressWarnings("all")
    public static DefaultTaskConfig generateMockConfig(@NonNull Long taskId, @NonNull DelegateExecution execution,
            @NonNull Long timeOutMilliSeconds, @NonNull MockTaskConfig config, @NonNull MockProperties mockProperties) {
        ConnectionConfig connectionConfig = getConnectionConfig(execution);
        connectionConfig.setDefaultSchema(getSchemaName(execution));
        ConnectionSession session = new DefaultConnectSessionFactory(connectionConfig).generateSession();
        try {
            String taskJson = config.getTaskDetail();
            PreConditions.notBlank(taskJson, "taskDetail");
            config.setId(taskId + "");
            ObjectMapper mapper = new ObjectMapper();
            HashMap<String, Object> map = mapper.readValue(taskJson, HashMap.class);
            List<Map<String, Object>> tableList = (List<Map<String, Object>>) map.get("tables");
            for (Map<String, Object> table : tableList) {
                List<Map<String, Object>> columns = (List<Map<String, Object>>) table.get("columns");
                int length = columns.size();
                for (int i = 0; i < length; i++) {
                    Map<String, Object> column = columns.get(i);
                    Map<String, Object> typeConfig = (Map<String, Object>) column.get("typeConfig");
                    if ("SKIP_GENERATOR".equalsIgnoreCase(typeConfig.get("generator").toString())) {
                        columns.remove(i);
                        i--;
                        length--;
                        continue;
                    }
                    String type =
                            MockDataTypeUtil.getType(session.getDialectType(), typeConfig.get("columnType").toString());
                    if (type == null) {
                        throw new UnsupportedException(String.format("target data type %s has not been supported yet",
                                typeConfig.get("columnType").toString()));
                    }
                    typeConfig.putIfAbsent("name", type);
                    if ("CHAR".equalsIgnoreCase(type)) {
                        DBSchemaAccessor schemaAccessor = DBSchemaAccessors.create(session);
                        DBTableOptions tableOptions = schemaAccessor.getTableOptions(getSchemaName(execution),
                                (String) table.get("tableName"));
                        if (StringUtils.isNotBlank(tableOptions.getCharsetName())) {
                            typeConfig.putIfAbsent("charset", tableOptions.getCharsetName().toUpperCase());
                        } else {
                            typeConfig.putIfAbsent("charset", "UTF_8");
                        }
                    }
                    MockDataTypeUtil.processTypeConfig(type, typeConfig);
                    if ("BIT".equalsIgnoreCase(typeConfig.get("columnType").toString())) {
                        // 兼容性处理，mysql模式下的bit类型位宽度使用precision字段传递，不符合mock工具的设定，改用width传递
                        if (typeConfig.get("highValue") != null) {
                            typeConfig.put("width", typeConfig.get("highValue"));
                        }
                        typeConfig.put("highValue", null);
                        typeConfig.put("lowValue", null);
                    }
                }
            }
            DefaultTaskConfig taskConfig = mapper.readValue(mapper.writeValueAsString(map), DefaultTaskConfig.class);
            taskConfig.setTaskName(config.getTaskName());
            taskConfig.setDialectType(session.getDialectType().isMysql() ? ObModeType.OB_MYSQL : ObModeType.OB_ORACLE);
            List<DefaultTableConfig> tableConfigList = taskConfig.tasks();
            PreConditions.notEmpty(tableConfigList, "tasks"); // table config list can not be null or empty

            for (DefaultTableConfig tableConfig : tableConfigList) {
                Verify.notGreaterThan(tableConfig.getTotalCount(), mockProperties.getMaxRowCount(), "MockTotalCount");
                tableConfig.setTimeout(timeOutMilliSeconds);
                tableConfig.setMaxRetainedCount(1);
                tableConfig.setSchemaName(getSchemaName(execution));

                List<DefaultColumnConfig> columnConfigs = tableConfig.columns();
                Verify.verify(CollectionUtils.isNotEmpty(columnConfigs), "Columns may not be empty");

                String filePath = String.format("%s/%s.sql",
                        mockProperties.getDownloadPath(config.getId()).getAbsolutePath(), tableConfig.getTableName());
                for (DefaultColumnConfig columnConfig : columnConfigs) {
                    String columnType = columnConfig.getTypeConfig().getColumnType();
                    columnConfig.getTypeConfig()
                            .setColumnType(String.format("%s_%s", taskConfig.getDialectType().name(), columnType));
                }
                tableConfig.setLocation(filePath);
            }
            taskConfig.setMinConnectionSize(mockProperties.getMinConnectionPoolSize());
            taskConfig.setMaxConnectionSize(mockProperties.getMaxConnectionPoolSize());
            taskConfig.setConnectionIncreasementStep(mockProperties.getConnectionPoolIncreaseStep());
            taskConfig.setDbConfig(getDbConfig(connectionConfig, execution));
            if (taskConfig.getTaskName() == null) {
                String taskName =
                        System.currentTimeMillis() + "-"
                                + ConnectionSessionUtil.getUserOrSchemaString(connectionConfig.getUsername(),
                                        connectionConfig.getDialectType())
                                + "-" + connectionConfig.getName() + "-mocker";
                taskConfig.setTaskName(taskName);
                config.setTaskName(taskName);
            }
            return taskConfig;
        } catch (Exception e) {
            log.warn("Error initializing mock data task, taskId={}", taskId, e);
            throw new RuntimeException(e);
        } finally {
            session.expire();
        }
    }

    /**
     * get packaged database information object
     *
     * @param config connection configuration for db
     * @return database config object
     */
    private static DataBaseConfig getDbConfig(ConnectionConfig config, DelegateExecution execution) {
        DataBaseConfig dbConfig = new DataBaseConfig();
        dbConfig.setCluster(config.getClusterName());
        dbConfig.setHost(config.getHost());
        dbConfig.setPassword(config.getPassword());
        dbConfig.setPort(config.getPort());
        dbConfig.setTenant(config.getTenantName());
        if (DialectType.OB_ORACLE.equals(config.getDialectType())
                || DialectType.ORACLE.equals(config.getDialectType())) {
            dbConfig.setUser(
                    "\"" + ConnectionSessionUtil.getUserOrSchemaString(config.getUsername(), config.getDialectType())
                            + "\"");
            dbConfig.setDefaultSchame("\"" + getSchemaName(execution) + "\"");
        } else if (Objects.nonNull(config.getDialectType()) && config.getDialectType().isMysql()) {
            dbConfig.setUser(config.getUsername());
            dbConfig.setDefaultSchame(config.getDefaultSchema());
        }
        OBTenantEndpoint endpoint = config.getEndpoint();
        if (Objects.nonNull(endpoint) && OceanBaseAccessMode.IC_PROXY == endpoint.getAccessMode()) {
            if (StringUtils.isNotBlank(endpoint.getProxyHost()) && Objects.nonNull(endpoint.getProxyPort())) {
                Map<String, String> connectParam = new HashMap<>();
                connectParam.put("socksProxyHost", endpoint.getProxyHost());
                connectParam.put("socksProxyPort", endpoint.getProxyPort().toString());
                dbConfig.setConnectParam(connectParam);
            }
        }
        return dbConfig;
    }

    @SuppressWarnings("all")
    private static <T> Optional<T> internalGet(Object value, Class<T> clazz) {
        if (value == null) {
            return Optional.empty();
        }
        if (!value.getClass().equals(clazz)) {
            return Optional.empty();
        }
        return Optional.of((T) value);
    }

    private static <T> Optional<T> internalGetParameter(DelegateExecution execution, Class<T> parameterClass) {
        Map<String, Object> variables = execution.getVariables();
        Object value = variables.get(RuntimeTaskConstants.PARAMETERS);
        Optional<String> optional = internalGet(value, String.class);
        if (!optional.isPresent()) {
            return Optional.empty();
        }
        T parameters = JsonUtils.fromJson(optional.get(), parameterClass);
        if (parameters == null) {
            return Optional.empty();
        }
        return Optional.of(parameters);
    }

}
