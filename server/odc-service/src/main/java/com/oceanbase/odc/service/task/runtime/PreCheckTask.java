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
package com.oceanbase.odc.service.task.runtime;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.datasource.SingleConnectionDataSource;
import com.oceanbase.odc.core.flow.model.FlowTaskResult;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.core.sql.execute.model.SqlTuple;
import com.oceanbase.odc.core.sql.split.OffsetString;
import com.oceanbase.odc.core.sql.split.SqlStatementIterator;
import com.oceanbase.odc.service.common.util.SqlUtils;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.flow.model.PreCheckTaskResult;
import com.oceanbase.odc.service.flow.task.model.DatabaseChangeParameters;
import com.oceanbase.odc.service.flow.task.model.DatabasePermissionCheckResult;
import com.oceanbase.odc.service.flow.task.model.SqlCheckTaskResult;
import com.oceanbase.odc.service.objectstorage.model.ObjectMetadata;
import com.oceanbase.odc.service.objectstorage.model.StorageObject;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeParameters;
import com.oceanbase.odc.service.permission.database.model.DatabasePermissionType;
import com.oceanbase.odc.service.regulation.ruleset.model.Rule;
import com.oceanbase.odc.service.regulation.ruleset.model.Rule.RuleViolation;
import com.oceanbase.odc.service.regulation.ruleset.model.RuleMetadata;
import com.oceanbase.odc.service.regulation.ruleset.model.RuleType;
import com.oceanbase.odc.service.resultset.ResultSetExportTaskParameter;
import com.oceanbase.odc.service.schedule.flowtask.AlterScheduleParameters;
import com.oceanbase.odc.service.schedule.model.JobType;
import com.oceanbase.odc.service.session.factory.OBConsoleDataSourceFactory;
import com.oceanbase.odc.service.session.model.UnauthorizedResource;
import com.oceanbase.odc.service.session.util.SchemaExtractor;
import com.oceanbase.odc.service.sqlcheck.DefaultSqlChecker;
import com.oceanbase.odc.service.sqlcheck.SqlCheckContext;
import com.oceanbase.odc.service.sqlcheck.SqlCheckRule;
import com.oceanbase.odc.service.sqlcheck.SqlCheckRuleFactory;
import com.oceanbase.odc.service.sqlcheck.model.CheckViolation;
import com.oceanbase.odc.service.sqlcheck.rule.SqlCheckRules;
import com.oceanbase.odc.service.task.caller.JobContext;
import com.oceanbase.odc.service.task.constants.JobParametersKeyConstants;
import com.oceanbase.odc.service.task.executor.server.ObjectStorageHandler;
import com.oceanbase.odc.service.task.executor.task.BaseTask;
import com.oceanbase.odc.service.task.runtime.PreCheckTaskParameters.AuthorizedDatabase;
import com.oceanbase.odc.service.task.util.JobUtils;
import com.oceanbase.tools.dbbrowser.parser.constant.SqlType;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @author gaoda.xy
 * @date 2024/1/30 11:02
 */
@Slf4j
public class PreCheckTask extends BaseTask<FlowTaskResult> {

    private PreCheckTaskParameters parameters;
    private List<OffsetString> userInputSqls;
    private InputStream uploadFileInputStream;
    private SqlStatementIterator uploadFileSqlIterator;
    private long taskId;
    private volatile boolean overLimit = false;
    private volatile boolean success = false;
    private SqlCheckTaskResult sqlCheckResult = null;
    private DatabasePermissionCheckResult permissionCheckResult = null;

    @Override
    protected void doInit(JobContext context) throws Exception {
        this.taskId = getJobContext().getJobIdentity().getId();
        log.info("Initiating pre-check task, taskId={}", taskId);
        this.parameters = JobUtils.fromJson(getJobParameters().get(JobParametersKeyConstants.TASK_PARAMETER_JSON_KEY),
                PreCheckTaskParameters.class);
        log.info("Load pre-check task parameters successfully, taskId={}", taskId);
        loadUserInputSqlContent();
        loadUploadFileInputStream();
        log.info("Load sql content successfully, taskId={}", taskId);
    }

    @Override
    protected boolean doStart(JobContext context) throws Exception {
        try {
            List<OffsetString> sqls = new ArrayList<>();
            this.overLimit = getSqlContentUntilOverLimit(sqls, this.parameters.getMaxReadContentBytes());
            List<UnauthorizedResource> unauthorizedResources = new ArrayList<>();
            List<CheckViolation> violations = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(sqls)) {
                violations.addAll(checkViolations(sqls));
                log.info("SQL check successfully, taskId={}", taskId);
                unauthorizedResources.addAll(filterUnAuthorizedDatabase(sqls));
                log.info("Database permission check successfully, taskId={}", taskId);
            }
            this.permissionCheckResult = new DatabasePermissionCheckResult(unauthorizedResources);
            this.sqlCheckResult = SqlCheckTaskResult.success(violations);
            this.success = true;
            log.info("Pre-check task end up running, task id: {}", taskId);
        } finally {
            tryCloseInputStream();
        }
        return this.success;
    }

    @Override
    protected void doStop() throws Exception {
        tryCloseInputStream();
    }

    @Override
    protected void doClose() throws Exception {
        tryCloseInputStream();
    }

    @Override
    public double getProgress() {
        return this.success ? 100D : 0D;
    }

    @Override
    public FlowTaskResult getTaskResult() {
        PreCheckTaskResult result = new PreCheckTaskResult();
        result.setOverLimit(this.overLimit);
        result.setSqlCheckResult(this.sqlCheckResult);
        result.setPermissionCheckResult(this.permissionCheckResult);
        return result;
    }

    private void loadUserInputSqlContent() {
        String sqlContent = null;
        String delimiter = ";";
        TaskType taskType = this.parameters.getTaskType();
        String paramJson = this.parameters.getParameterJson();
        if (taskType == TaskType.ASYNC) {
            DatabaseChangeParameters params = JsonUtils.fromJson(paramJson, DatabaseChangeParameters.class);
            sqlContent = params.getSqlContent();
            delimiter = params.getDelimiter();
        } else if (taskType == TaskType.ONLINE_SCHEMA_CHANGE) {
            OnlineSchemaChangeParameters params = JsonUtils.fromJson(paramJson, OnlineSchemaChangeParameters.class);
            sqlContent = params.getSqlContent();
            delimiter = params.getDelimiter();
        } else if (taskType == TaskType.EXPORT_RESULT_SET) {
            ResultSetExportTaskParameter params = JsonUtils.fromJson(paramJson, ResultSetExportTaskParameter.class);
            sqlContent = params.getSql();
        } else if (taskType == TaskType.ALTER_SCHEDULE) {
            AlterScheduleParameters params = JsonUtils.fromJson(paramJson, AlterScheduleParameters.class);
            if (params.getType() != JobType.SQL_PLAN) {
                return;
            }
            DatabaseChangeParameters dcParams = (DatabaseChangeParameters) params.getScheduleTaskParameters();
            sqlContent = dcParams.getSqlContent();
            delimiter = dcParams.getDelimiter();
        }
        if (StringUtils.isNotBlank(sqlContent)) {
            this.userInputSqls = SqlUtils.splitWithOffset(this.parameters.getConnectionConfig().getDialectType(),
                    sqlContent, delimiter);
        }
    }

    private void loadUploadFileInputStream() throws IOException {
        DatabaseChangeParameters params = null;
        TaskType taskType = this.parameters.getTaskType();
        String paramJson = this.parameters.getParameterJson();
        if (taskType == TaskType.ASYNC) {
            params = JsonUtils.fromJson(paramJson, DatabaseChangeParameters.class);
        } else if (taskType == TaskType.ALTER_SCHEDULE) {
            AlterScheduleParameters asParams = JsonUtils.fromJson(paramJson, AlterScheduleParameters.class);
            if (asParams.getType() != JobType.SQL_PLAN) {
                return;
            }
            params = (DatabaseChangeParameters) asParams.getScheduleTaskParameters();
        }
        this.uploadFileInputStream = new ByteArrayInputStream(new byte[0]);
        List<ObjectMetadata> objectMetadataList = this.parameters.getSqlFileObjectMetadatas();
        if (Objects.nonNull(params) && CollectionUtils.isNotEmpty(objectMetadataList)) {
            for (ObjectMetadata objectMetadata : objectMetadataList) {
                StorageObject object =
                        new ObjectStorageHandler(getCloudObjectStorageService(), JobUtils.getExecutorDataPath())
                                .loadObject(objectMetadata);
                InputStream current = object.getContent();
                // remove UTF-8 BOM if exists
                current.mark(3);
                byte[] byteSql = new byte[3];
                if (current.read(byteSql) >= 3 && byteSql[0] == (byte) 0xef && byteSql[1] == (byte) 0xbb
                        && byteSql[2] == (byte) 0xbf) {
                    current.reset();
                    current.skip(3);
                } else {
                    current.reset();
                }
                this.uploadFileInputStream = new SequenceInputStream(this.uploadFileInputStream, current);
            }
            this.uploadFileSqlIterator = SqlUtils.iterator(this.parameters.getConnectionConfig().getDialectType(),
                    params.getDelimiter(), this.uploadFileInputStream, StandardCharsets.UTF_8);
        }
    }

    /**
     * Get the sql content from the databaseChangeRelatedSqls and sqlIterator, and put them into the
     * sqlBuffer. If the sql content is over the maxSqlBytes, return true, else return false.
     *
     * @param sqlBuffer the buffer to store the sql content
     * @param maxSqlBytes the max sql content bytes
     * @return true if the sql content is over the maxSqlBytes, else return false
     */
    boolean getSqlContentUntilOverLimit(@NonNull List<OffsetString> sqlBuffer, long maxSqlBytes) {
        long curSqlBytes = 0;
        if (CollectionUtils.isNotEmpty(userInputSqls)) {
            for (OffsetString sql : userInputSqls) {
                int sqlBytes = sql.getStr().getBytes(StandardCharsets.UTF_8).length;
                if (curSqlBytes + sqlBytes > maxSqlBytes) {
                    return true;
                }
                curSqlBytes += sqlBytes;
                sqlBuffer.add(sql);
            }
        }
        if (Objects.nonNull(this.uploadFileSqlIterator)) {
            while (this.uploadFileSqlIterator.hasNext()) {
                String sql = this.uploadFileSqlIterator.next().getStr();
                int sqlBytes = sql.getBytes(StandardCharsets.UTF_8).length;
                if (curSqlBytes + sqlBytes > maxSqlBytes) {
                    return true;
                }
                curSqlBytes += sqlBytes;
                sqlBuffer.add(new OffsetString(0, sql));
            }
        }
        return false;
    }

    private List<CheckViolation> checkViolations(List<OffsetString> sqls) {
        if (CollectionUtils.isEmpty(sqls)) {
            return Collections.emptyList();
        }
        ConnectionConfig config = this.parameters.getConnectionConfig();
        List<Rule> rules = this.parameters.getRules();
        OBConsoleDataSourceFactory factory = new OBConsoleDataSourceFactory(config, true, false);
        factory.resetSchema(origin -> OBConsoleDataSourceFactory
                .getSchema(this.parameters.getRiskLevelDescriber().getDatabaseName(), config.getDialectType()));
        SqlCheckContext checkContext = new SqlCheckContext((long) sqls.size());
        try (SingleConnectionDataSource dataSource = (SingleConnectionDataSource) factory.getDataSource()) {
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            List<SqlCheckRule> checkRules = getRules(rules, config.getDialectType(), jdbc);
            DefaultSqlChecker sqlChecker = new DefaultSqlChecker(config.getDialectType(), null, checkRules);
            List<CheckViolation> checkViolations = new ArrayList<>();
            for (OffsetString sql : sqls) {
                List<CheckViolation> violations = sqlChecker.check(Collections.singletonList(sql), checkContext);
                fullFillRiskLevel(rules, violations);
                checkViolations.addAll(violations);
            }
            return checkViolations;
        }
    }

    private List<SqlCheckRule> getRules(List<Rule> rules, @NonNull DialectType dialectType,
            @NonNull JdbcOperations jdbc) {
        if (CollectionUtils.isEmpty(rules)) {
            return Collections.emptyList();
        }
        List<SqlCheckRuleFactory> candidates = SqlCheckRules.getAllFactories(dialectType, jdbc);
        return rules.stream().filter(rule -> {
            RuleMetadata metadata = rule.getMetadata();
            if (metadata == null || !Boolean.TRUE.equals(rule.getEnabled())) {
                return false;
            }
            return Objects.equals(metadata.getType(), RuleType.SQL_CHECK);
        }).map(rule -> {
            try {
                return SqlCheckRules.createByRule(candidates, dialectType, rule);
            } catch (Exception e) {
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    private void fullFillRiskLevel(List<Rule> rules, @NonNull List<CheckViolation> violations) {
        Map<String, Rule> name2RuleMap = CollectionUtils.isEmpty(rules)
                ? new HashMap<>()
                : rules.stream().collect(Collectors.toMap(r -> r.getMetadata().getName(), r -> r));
        violations.forEach(c -> {
            String name = "${" + c.getType().getName() + "}";
            Rule rule = name2RuleMap.getOrDefault(name, null);
            if (Objects.nonNull(rule)) {
                c.setLevel(rule.getLevel());
                Rule newRule = new Rule();
                newRule.setLevel(rule.getLevel());
                newRule.setViolation(RuleViolation.fromCheckViolation(c));
            }
        });
    }

    private List<UnauthorizedResource> filterUnAuthorizedDatabase(List<OffsetString> sqls) {
        // Get needed permission types for accessing the schemas
        Map<String, Set<DatabasePermissionType>> neededSchemaName2PermissionTypes = new HashMap<>();
        Map<String, Set<SqlType>> schemaName2SqlTypes = SchemaExtractor.listSchemaName2SqlTypes(
                sqls.stream().map(e -> SqlTuple.newTuple(e.getStr())).collect(Collectors.toList()),
                this.parameters.getDefaultSchema(), this.parameters.getConnectionConfig().getDialectType());
        for (Entry<String, Set<SqlType>> entry : schemaName2SqlTypes.entrySet()) {
            Set<SqlType> sqlTypes = entry.getValue();
            if (CollectionUtils.isNotEmpty(sqlTypes)) {
                Set<DatabasePermissionType> permissionTypes = sqlTypes.stream().map(DatabasePermissionType::from)
                        .filter(Objects::nonNull).collect(Collectors.toSet());
                permissionTypes.addAll(DatabasePermissionType.from(this.parameters.getTaskType()));
                if (CollectionUtils.isNotEmpty(permissionTypes)) {
                    neededSchemaName2PermissionTypes.put(entry.getKey(), permissionTypes);
                }
            }
        }
        if (neededSchemaName2PermissionTypes.isEmpty()) {
            return Collections.emptyList();
        }
        // Get authorized permission types for all schemas in current datasource
        Map<String, Set<DatabasePermissionType>> authorizedSchema2PermissionTypes = new HashMap<>();
        Map<String, Database> schemaName2Database = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        List<AuthorizedDatabase> authorizedDatabases = this.parameters.getAuthorizedDatabase();
        if (CollectionUtils.isNotEmpty(authorizedDatabases)) {
            for (AuthorizedDatabase authorizedDatabase : authorizedDatabases) {
                authorizedSchema2PermissionTypes.put(authorizedDatabase.getName(),
                        authorizedDatabase.getPermissionTypes());
                Database database = new Database();
                database.setId(authorizedDatabase.getId());
                database.setName(authorizedDatabase.getName());
                database.setDataSource(this.parameters.getConnectionConfig());
                schemaName2Database.put(authorizedDatabase.getName(), database);
            }
        }
        List<UnauthorizedResource> ret = new ArrayList<>();
        for (Map.Entry<String, Set<DatabasePermissionType>> entry : neededSchemaName2PermissionTypes.entrySet()) {
            String schemaName = entry.getKey();
            Set<DatabasePermissionType> needs = entry.getValue();
            if (CollectionUtils.isEmpty(needs)) {
                continue;
            }
            if (schemaName2Database.containsKey(schemaName)) {
                Database database = schemaName2Database.get(schemaName);
                Set<DatabasePermissionType> authorized = authorizedSchema2PermissionTypes.get(schemaName);
                if (CollectionUtils.isEmpty(authorized)) {
                    ret.add(UnauthorizedResource.from(database, needs));
                } else {
                    Set<DatabasePermissionType> unauthorized =
                            needs.stream().filter(p -> !authorized.contains(p)).collect(Collectors.toSet());
                    if (CollectionUtils.isNotEmpty(unauthorized)) {
                        ret.add(UnauthorizedResource.from(database, unauthorized));
                    }
                }
            } else {
                Database unknownDatabase = new Database();
                unknownDatabase.setName(schemaName);
                unknownDatabase.setDataSource(this.parameters.getConnectionConfig());
                ret.add(UnauthorizedResource.from(unknownDatabase, needs));
            }
        }
        return ret;
    }


    private void tryCloseInputStream() {
        if (Objects.nonNull(this.uploadFileInputStream)) {
            try {
                this.uploadFileInputStream.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }

}
