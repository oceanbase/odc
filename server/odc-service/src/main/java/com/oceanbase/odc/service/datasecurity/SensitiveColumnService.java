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
package com.oceanbase.odc.service.datasecurity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.google.common.collect.Sets;
import com.oceanbase.odc.core.authority.util.Authenticated;
import com.oceanbase.odc.core.authority.util.PreAuthenticate;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.metadb.datasecurity.SensitiveColumnEntity;
import com.oceanbase.odc.metadb.datasecurity.SensitiveColumnRepository;
import com.oceanbase.odc.metadb.datasecurity.SensitiveColumnSpecs;
import com.oceanbase.odc.service.common.model.InnerUser;
import com.oceanbase.odc.service.common.model.Stats;
import com.oceanbase.odc.service.connection.ConnectionService;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.datasecurity.extractor.model.DBColumn;
import com.oceanbase.odc.service.datasecurity.model.QuerySensitiveColumnParams;
import com.oceanbase.odc.service.datasecurity.model.SensitiveColumn;
import com.oceanbase.odc.service.datasecurity.model.SensitiveColumnScanningReq;
import com.oceanbase.odc.service.datasecurity.model.SensitiveColumnScanningTaskInfo;
import com.oceanbase.odc.service.datasecurity.model.SensitiveRule;
import com.oceanbase.odc.service.datasecurity.util.SensitiveColumnMapper;
import com.oceanbase.odc.service.iam.HorizontalDataPermissionValidator;
import com.oceanbase.odc.service.iam.UserService;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

/**
 * @author gaoda.xy
 * @date 2023/5/22 11:12
 */
@Slf4j
@Service
@Validated
@Authenticated
public class SensitiveColumnService {

    @Autowired
    private SensitiveRuleService ruleService;

    @Autowired
    private MaskingAlgorithmService algorithmService;

    @Autowired
    private ConnectionService connectionService;

    @Autowired
    private SensitiveColumnRepository repository;

    @Autowired
    private UserService userService;

    @Autowired
    private DatabaseService databaseService;

    @Autowired
    private SensitiveColumnScanningTaskManager scanningTaskManager;

    @Autowired
    private AuthenticationFacade authenticationFacade;

    @Autowired
    private HorizontalDataPermissionValidator permissionValidator;

    private static final SensitiveColumnMapper mapper = SensitiveColumnMapper.INSTANCE;

    @Transactional(rollbackFor = Exception.class)
    @PreAuthenticate(hasAnyResourceRole = {"OWNER, DBA"}, resourceType = "ODC_PROJECT", indexOfIdParam = 0)
    public Boolean exists(@NotNull Long projectId, @NotNull SensitiveColumn column) {
        PreConditions.notNull(column.getDatabase(), "database");
        PreConditions.notEmpty(column.getTableName(), "tableName");
        PreConditions.notEmpty(column.getColumnName(), "columnName");
        checkProjectDatabases(projectId, Collections.singleton(column.getDatabase().getId()));
        SensitiveColumnEntity entity = new SensitiveColumnEntity();
        entity.setDatabaseId(column.getDatabase().getId());
        entity.setTableName(column.getTableName());
        entity.setColumnName(column.getColumnName());
        entity.setOrganizationId(authenticationFacade.currentOrganizationId());
        return repository.exists(Example.of(entity));
    }

    @Transactional(rollbackFor = Exception.class)
    @PreAuthenticate(hasAnyResourceRole = {"OWNER, DBA"}, resourceType = "ODC_PROJECT", indexOfIdParam = 0)
    public List<SensitiveColumn> batchCreate(@NotNull Long projectId,
            @NotEmpty @Valid List<SensitiveColumn> columns) {
        Set<Long> databaseIds =
                columns.stream().map(SensitiveColumn::getDatabase).map(Database::getId).collect(Collectors.toSet());
        checkProjectDatabases(projectId, databaseIds);
        Set<Long> maskingAlgorithmIds =
                columns.stream().map(SensitiveColumn::getMaskingAlgorithmId).collect(Collectors.toSet());
        permissionValidator.checkCurrentOrganization(algorithmService.batchNullSafeGetModel(maskingAlgorithmIds));
        Long organizationId = authenticationFacade.currentOrganizationId();
        Long userId = authenticationFacade.currentUserId();
        Specification<SensitiveColumnEntity> spec = Specification
                .where(SensitiveColumnSpecs.databaseIdIn(databaseIds))
                .and(SensitiveColumnSpecs.organizationIdEqual(organizationId));
        Set<SensitiveColumnMeta> exists =
                repository.findAll(spec).stream().map(SensitiveColumnMeta::new).collect(Collectors.toSet());
        List<SensitiveColumnEntity> entities = new ArrayList<>();
        for (SensitiveColumn column : columns) {
            SensitiveColumnEntity entity = mapper.modelToEntity(column);
            PreConditions.validNoDuplicated(ResourceType.ODC_SENSITIVE_COLUMN, "tableName.columnName",
                    column.getTableName() + "." + column.getColumnName(),
                    () -> exists.contains(new SensitiveColumnMeta(entity)));
            entity.setCreatorId(userId);
            entity.setOrganizationId(organizationId);
            entities.add(entity);
        }
        repository.saveAll(entities);
        log.info("Sensitive columns has been created, id={}", entities.stream().map(SensitiveColumnEntity::getId)
                .map(Object::toString).collect(Collectors.joining(",")));
        return entities.stream().map(mapper::entityToModel).collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    @PreAuthenticate(hasAnyResourceRole = {"OWNER, DBA"}, resourceType = "ODC_PROJECT", indexOfIdParam = 0)
    public SensitiveColumn detail(@NotNull Long projectId, @NotNull Long id) {
        SensitiveColumnEntity entity = nullSafeGet(id);
        checkProjectDatabases(projectId, Collections.singletonList(entity.getDatabaseId()));
        SensitiveColumn column = mapper.entityToModel(entity);
        permissionValidator.checkCurrentOrganization(column);
        if (Objects.nonNull(entity.getCreatorId())) {
            try {
                column.setCreator(new InnerUser(userService.nullSafeGet(entity.getCreatorId())));
            } catch (NotFoundException e) {
                log.warn("The creator of sensitive column is not exists, columnId={}, creatorId={}", id,
                        entity.getCreatorId(), e);
            }
        }
        if (Objects.nonNull(entity.getDatabaseId())) {
            try {
                column.setDatabase(databaseService.detail(entity.getDatabaseId()));
            } catch (NotFoundException e) {
                log.warn("The database of sensitive column is not exists, columnId={}, databaseId={}", id,
                        entity.getDatabaseId(), e);
            }
        }
        return column;
    }

    @Transactional(rollbackFor = Exception.class)
    @PreAuthenticate(hasAnyResourceRole = {"OWNER, DBA"}, resourceType = "ODC_PROJECT", indexOfIdParam = 0)
    public List<SensitiveColumn> batchUpdate(@NotNull Long projectId, @NotEmpty List<Long> ids,
            @NotNull Long maskingAlgorithmId) {
        List<SensitiveColumnEntity> entities = batchNullSafeGet(new HashSet<>(ids));
        List<Long> databaseIds =
                entities.stream().map(SensitiveColumnEntity::getDatabaseId).collect(Collectors.toList());
        checkProjectDatabases(projectId, databaseIds);
        permissionValidator.checkCurrentOrganization(
                algorithmService.batchNullSafeGetModel(Collections.singleton(maskingAlgorithmId)));
        entities.forEach(entity -> entity.setMaskingAlgorithmId(maskingAlgorithmId));
        repository.saveAll(entities);
        log.info("Sensitive columns has been updated, id={}", entities.stream().map(SensitiveColumnEntity::getId)
                .map(Object::toString).collect(Collectors.joining(",")));
        return entities.stream().map(mapper::entityToModel).collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    @PreAuthenticate(hasAnyResourceRole = {"OWNER, DBA"}, resourceType = "ODC_PROJECT", indexOfIdParam = 0)
    public List<SensitiveColumn> batchDelete(@NotNull Long projectId, @NotEmpty List<Long> ids) {
        List<SensitiveColumnEntity> entities = batchNullSafeGet(new HashSet<>(ids));
        Set<Long> databaseIds = entities.stream().map(SensitiveColumnEntity::getDatabaseId).collect(Collectors.toSet());
        checkProjectDatabases(projectId, databaseIds);
        List<SensitiveColumn> columns = entities.stream().map(mapper::entityToModel).collect(Collectors.toList());
        permissionValidator.checkCurrentOrganization(columns);
        repository.deleteAll(entities);
        log.info("Sensitive columns has been deleted, id={}", entities.stream().map(SensitiveColumnEntity::getId)
                .map(Object::toString).collect(Collectors.joining(",")));
        return columns;
    }

    @Transactional(rollbackFor = Exception.class)
    @PreAuthenticate(hasAnyResourceRole = {"OWNER, DBA"}, resourceType = "ODC_PROJECT", indexOfIdParam = 0)
    public Page<SensitiveColumn> list(@NotNull Long projectId, @NotNull QuerySensitiveColumnParams params,
            Pageable pageable) {
        Set<Long> databaseIds = databaseService.listDatabaseIdsByProjectId(projectId);
        if (CollectionUtils.isNotEmpty(params.getDatasourceNames())) {
            List<Long> connectionIds = connectionService.innerListIdByOrganizationIdAndNames(
                    authenticationFacade.currentOrganizationId(), params.getDatasourceNames());
            if (CollectionUtils.isEmpty(connectionIds)) {
                return Page.empty(pageable);
            }
            databaseIds = Sets.intersection(databaseIds, databaseService.listDatabaseIdsByConnectionIds(connectionIds));
        }
        if (CollectionUtils.isNotEmpty(params.getDatabaseNames())) {
            Set<String> databaseNames = new HashSet<>(params.getDatabaseNames());
            databaseIds = Sets.intersection(databaseIds,
                    databaseService.listDatabaseByNames(params.getDatabaseNames()).stream()
                            .filter(e -> databaseNames.contains(e.getName())).map(Database::getId)
                            .collect(Collectors.toSet()));
        }
        if (databaseIds.isEmpty()) {
            return Page.empty(pageable);
        }
        List<Database> databases = databaseService.listDatabasesByIds(databaseIds);
        Map<Long, Database> id2Database =
                databases.stream().collect(Collectors.toMap(Database::getId, d -> d, (d1, d2) -> d1));
        List<ConnectionConfig> datasources = connectionService.innerListByIds(
                databases.stream().map(database -> database.getDataSource().getId()).collect(Collectors.toSet()));
        Map<Long, ConnectionConfig> id2Datasource =
                datasources.stream().collect(Collectors.toMap(ConnectionConfig::getId, c -> c, (c1, c2) -> c1));
        id2Database.values()
                .forEach(database -> database.setDataSource(id2Datasource.get(database.getDataSource().getId())));
        Specification<SensitiveColumnEntity> spec = Specification
                .where(SensitiveColumnSpecs.databaseIdIn(databaseIds))
                .and(SensitiveColumnSpecs.tableNameLike(params.getFuzzyTableColumn())
                        .or(SensitiveColumnSpecs.columnNameLike(params.getFuzzyTableColumn())))
                .and(SensitiveColumnSpecs.maskingAlgorithmIdIn(params.getMaskingAlgorithmIds()))
                .and(SensitiveColumnSpecs.enabledEqual(params.getEnabled()))
                .and(SensitiveColumnSpecs.organizationIdEqual(authenticationFacade.currentOrganizationId()));
        return repository.findAll(spec, pageable).map(entity -> {
            SensitiveColumn column = mapper.entityToModel(entity);
            column.setDatabase(id2Database.get(entity.getDatabaseId()));
            return column;
        });
    }

    @Transactional(rollbackFor = Exception.class)
    @PreAuthenticate(hasAnyResourceRole = {"OWNER, DBA"}, resourceType = "ODC_PROJECT", indexOfIdParam = 0)
    public Stats stats(@NotNull Long projectId) {
        Set<Long> databaseIds = databaseService.listDatabaseIdsByProjectId(projectId);
        List<SensitiveColumnEntity> entities = repository.findByDatabaseIdIn(databaseIds);
        if (entities.isEmpty()) {
            return null;
        }
        List<Database> databases = databaseService.listDatabasesByIds(
                entities.stream().map(SensitiveColumnEntity::getDatabaseId).collect(Collectors.toSet()));
        Set<String> databaseNames = databases.stream().map(Database::getName).collect(Collectors.toSet());
        Set<String> datasourceNames = connectionService
                .innerListByIds(databases.stream().map(d -> d.getDataSource().getId()).collect(Collectors.toSet()))
                .stream().map(ConnectionConfig::getName).collect(Collectors.toSet());
        Set<String> algorithmIds =
                entities.stream().map(e -> e.getMaskingAlgorithmId().toString()).collect(Collectors.toSet());
        Stats stats = new Stats();
        stats.andDistinct("datasource", datasourceNames);
        stats.andDistinct("database", databaseNames);
        stats.andDistinct("maskingAlgorithmId", algorithmIds);
        return stats;
    }

    @Transactional(rollbackFor = Exception.class)
    @PreAuthenticate(hasAnyResourceRole = {"OWNER, DBA"}, resourceType = "ODC_PROJECT", indexOfIdParam = 0)
    public SensitiveColumn setEnabled(@NotNull Long projectId, @NotNull Long id, @NotNull Boolean enabled) {
        SensitiveColumnEntity entity = nullSafeGet(id);
        checkProjectDatabases(projectId, Collections.singleton(entity.getDatabaseId()));
        permissionValidator.checkCurrentOrganization(mapper.entityToModel(entity));
        if (!Objects.equals(entity.getEnabled(), enabled)) {
            entity.setEnabled(enabled);
            repository.saveAndFlush(entity);
            log.info("Sensitive column has been updated, id={}", entity.getId());
        }
        return mapper.entityToModel(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    @PreAuthenticate(hasAnyResourceRole = {"OWNER, DBA"}, resourceType = "ODC_PROJECT", indexOfIdParam = 0)
    public SensitiveColumnScanningTaskInfo startScanning(@NotNull Long projectId,
            @NotNull @Valid SensitiveColumnScanningReq req) {
        Set<Long> databaseIds;
        if (req.getAllDatabases()) {
            databaseIds = Sets.intersection(databaseService.listDatabaseIdsByProjectId(projectId),
                    databaseService.listDatabaseIdsByConnectionIds(Collections.singleton(req.getConnectionId())));
        } else {
            PreConditions.notEmpty(req.getDatabaseIds(), "databaseIds");
            databaseIds = new HashSet<>(req.getDatabaseIds());
            checkProjectDatabases(projectId, databaseIds);
        }
        PreConditions.notEmpty(databaseIds, "databaseIds");
        List<Database> databases = databaseService.listDatabasesByIds(databaseIds);
        List<SensitiveRule> rules;
        if (req.getAllSensitiveRules()) {
            rules = ruleService.getByProjectId(projectId);
        } else {
            PreConditions.notEmpty(req.getSensitiveRuleIds(), "sensitiveRuleIds");
            rules = ruleService.batchNullSafeGetModel(new HashSet<>(req.getSensitiveRuleIds()));
            checkoutSensitiveRules(projectId, rules);
        }
        PreConditions.notEmpty(databases, "databases");
        PreConditions.notEmpty(rules, "sensitiveRules");
        ConnectionConfig connectionConfig = databaseService.findDataSourceForConnectById(databases.get(0).getId());
        Map<Long, List<SensitiveColumn>> databaseId2SensitiveColumns = repository.findByDatabaseIdIn(databaseIds)
                .stream().map(mapper::entityToModel).collect(Collectors.groupingBy(e -> e.getDatabase().getId()));
        return scanningTaskManager.start(databases, rules, connectionConfig, databaseId2SensitiveColumns);
    }

    @Transactional(rollbackFor = Exception.class)
    @PreAuthenticate(hasAnyResourceRole = {"OWNER, DBA"}, resourceType = "ODC_PROJECT", indexOfIdParam = 0)
    public SensitiveColumnScanningTaskInfo getScanningResults(@NotNull Long projectId, @NotBlank String taskId) {
        SensitiveColumnScanningTaskInfo taskInfo = scanningTaskManager.get(taskId);
        if (!Objects.equals(taskInfo.getProjectId(), projectId)) {
            String errorMsg = String.format("Sensitive column scanning task not exists, taskId=%s", taskId);
            throw new NotFoundException(ErrorCodes.IllegalArgument, new Object[] {"taskId", errorMsg}, null);
        }
        return taskInfo;
    }

    @SkipAuthorize("odc internal usages")
    public SensitiveColumnEntity nullSafeGet(@NotNull Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException(ResourceType.ODC_SENSITIVE_COLUMN, "id", id));
    }

    @SkipAuthorize("odc internal usages")
    public List<SensitiveColumnEntity> batchNullSafeGet(@NotNull Set<Long> ids) {
        List<SensitiveColumnEntity> entities = repository.findByIdIn(ids);
        if (ids.size() > entities.size()) {
            Set<Long> presentIds = entities.stream().map(SensitiveColumnEntity::getId).collect(Collectors.toSet());
            String absentIds = ids.stream().filter(id -> !presentIds.contains(id)).map(Object::toString)
                    .collect(Collectors.joining(","));
            throw new NotFoundException(ResourceType.ODC_SENSITIVE_COLUMN, "id", absentIds);
        }
        return entities;
    }

    @SkipAuthorize("odc internal usages")
    public List<Set<SensitiveColumn>> filterSensitiveColumns(@NotNull Long datasourceId,
            @NotEmpty List<Set<DBColumn>> tableColumns) {
        Set<String> databaseNames = new HashSet<>();
        Set<String> tableNames = new HashSet<>();
        for (Set<DBColumn> columns : tableColumns) {
            if (CollectionUtils.isNotEmpty(columns)) {
                databaseNames.addAll(columns.stream().map(DBColumn::getDatabaseName).collect(Collectors.toSet()));
                tableNames.addAll(columns.stream().map(DBColumn::getTableName).collect(Collectors.toSet()));
            }
        }
        List<Set<SensitiveColumn>> tableSensitiveColumns = new ArrayList<>();
        if (databaseNames.isEmpty() || tableNames.isEmpty()) {
            tableColumns.forEach(t -> tableSensitiveColumns.add(new HashSet<>()));
            return tableSensitiveColumns;
        }
        Map<String, Long> databaseName2Id =
                databaseService.listDatabasesByConnectionIds(Collections.singleton(datasourceId)).stream()
                        .collect(Collectors.toMap(Database::getName, Database::getId, (d1, d2) -> d1));
        Map<Long, String> databaseId2Name = new HashMap<>();
        for (String databaseName : databaseNames) {
            databaseId2Name.put(databaseName2Id.get(databaseName), databaseName);
        }
        Specification<SensitiveColumnEntity> spec = Specification
                .where(SensitiveColumnSpecs.databaseIdIn(databaseId2Name.keySet()))
                .and(SensitiveColumnSpecs.tableNameIn(tableNames))
                .and(SensitiveColumnSpecs.enabledEqual(true));
        List<SensitiveColumn> sensitiveColumns =
                repository.findAll(spec).stream().map(mapper::entityToModel).collect(Collectors.toList());
        Map<DBColumn, SensitiveColumn> dbColumn2SensitiveColumn =
                sensitiveColumns.stream().collect(Collectors.toMap(
                        column -> new DBColumn(databaseId2Name.get(column.getDatabase().getId()), column.getTableName(),
                                column.getColumnName()),
                        column -> column,
                        (c1, c2) -> c2));
        for (Set<DBColumn> columns : tableColumns) {
            tableSensitiveColumns.add(columns.stream().filter(dbColumn2SensitiveColumn::containsKey)
                    .map(dbColumn2SensitiveColumn::get).collect(Collectors.toSet()));
        }
        return tableSensitiveColumns;
    }

    @SkipAuthorize("odc internal usages")
    public List<SensitiveColumn> listByDatabaseAndTable(Collection<Long> databaseIds, Collection<String> tables) {
        Specification<SensitiveColumnEntity> spec = Specification
                .where(SensitiveColumnSpecs.databaseIdIn(databaseIds))
                .and(SensitiveColumnSpecs.tableNameIn(tables))
                .and(SensitiveColumnSpecs.enabledEqual(true));
        return repository.findAll(spec).stream().map(mapper::entityToModel).collect(Collectors.toList());
    }

    private void checkProjectDatabases(@NotNull Long projectId, @NotEmpty Collection<Long> databaseIds) {
        Set<Long> founds = databaseService.listDatabaseIdsByProjectId(projectId);
        List<Long> notFounds = databaseIds.stream().filter(id -> !founds.contains(id)).collect(Collectors.toList());
        if (notFounds.size() > 0) {
            throw new NotFoundException(ResourceType.ODC_DATABASE, "id",
                    notFounds.stream().map(Object::toString).collect(Collectors.joining(", ")));
        }
    }

    private void checkoutSensitiveRules(@NotNull Long projectId, @NotEmpty Collection<SensitiveRule> rules) {
        List<Long> invalids = rules.stream().filter(rule -> !Objects.equals(rule.getProjectId(), projectId))
                .map(SensitiveRule::id).collect(Collectors.toList());
        if (invalids.size() > 0) {
            throw new NotFoundException(ResourceType.ODC_SENSITIVE_RULE, "id",
                    invalids.stream().map(Object::toString).collect(Collectors.joining(", ")));
        }
    }

    @Data
    private static class SensitiveColumnMeta {
        private Long databaseId;
        private String tableName;
        private String columnName;

        public SensitiveColumnMeta(SensitiveColumnEntity entity) {
            this.databaseId = entity.getDatabaseId();
            this.tableName = entity.getTableName();
            this.columnName = entity.getColumnName();
        }

        @Override
        public int hashCode() {
            return Objects.hash(databaseId, tableName.toLowerCase(), columnName.toLowerCase());
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof SensitiveColumnMeta) {
                SensitiveColumnMeta other = (SensitiveColumnMeta) obj;
                return Objects.equals(databaseId, other.databaseId)
                        && Objects.equals(tableName.toLowerCase(), other.tableName.toLowerCase())
                        && Objects.equals(columnName.toLowerCase(), other.columnName.toLowerCase());
            }
            return false;
        }

    }

}
