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
package com.oceanbase.odc.service.shadowtable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import org.springframework.util.CollectionUtils;

import com.oceanbase.odc.common.util.ObjectUtil;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.model.TableIdentity;
import com.oceanbase.odc.metadb.shadowtable.TableComparingEntity;
import com.oceanbase.odc.metadb.shadowtable.TableComparingRepository;
import com.oceanbase.odc.service.db.DBTableService;
import com.oceanbase.odc.service.db.browser.DBObjectEditorFactory;
import com.oceanbase.odc.service.db.browser.DBTableEditorFactory;
import com.oceanbase.odc.service.db.model.GenerateTableDDLResp;
import com.oceanbase.odc.service.db.model.GenerateUpdateTableDDLReq;
import com.oceanbase.odc.service.session.factory.DefaultConnectSessionFactory;
import com.oceanbase.odc.service.shadowtable.model.ShadowTableSyncReq;
import com.oceanbase.odc.service.shadowtable.model.TableComparingResult;
import com.oceanbase.tools.dbbrowser.editor.DBTableEditor;
import com.oceanbase.tools.dbbrowser.model.DBConstraintType;
import com.oceanbase.tools.dbbrowser.model.DBTable;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2022/9/20 下午2:43
 * @Description: []
 */
@Slf4j
public class ShadowTableComparingTask implements Callable<Void> {
    private final Long taskId;
    private final String schemaName;
    private final DBTableService dbTableService;
    private final ConnectionSession connectionSession;
    private final TableComparingRepository comparingRepository;


    public ShadowTableComparingTask(@NonNull ShadowTableSyncReq shadowTableSyncReq, @NonNull Long taskId,
            @NonNull DBTableService dbTableService, @NonNull TableComparingRepository comparingRepository) {
        this.taskId = taskId;
        this.schemaName = shadowTableSyncReq.getSchemaName();
        this.dbTableService = dbTableService;
        DefaultConnectSessionFactory factory =
                new DefaultConnectSessionFactory(shadowTableSyncReq.getConnectionConfig());
        this.connectionSession = factory.generateSession();
        this.comparingRepository = comparingRepository;
    }

    @Override
    public Void call() {
        try {
            log.info("start to run shadow table comparing task, taskId={}", taskId);
            List<TableComparingEntity> comparingEntities =
                    comparingRepository.findByComparingTaskId(taskId);
            if (CollectionUtils.isEmpty(comparingEntities)) {
                log.info("no tables need to compare, schemaName={}", schemaName);
                return null;
            }
            List<String> allRealTableNames = new ArrayList();
            comparingEntities.forEach(tableComparingEntity -> {
                if (dbTableService.isLowerCaseTableName(connectionSession)) {
                    String lowerCaseOriginalTableName =
                            StringUtils.lowerCase(tableComparingEntity.getOriginalTableName());
                    String lowerCaseDestTableName = StringUtils.lowerCase(tableComparingEntity.getDestTableName());
                    tableComparingEntity.setOriginalTableName(lowerCaseOriginalTableName);
                    tableComparingEntity.setDestTableName(lowerCaseDestTableName);
                    allRealTableNames.add(lowerCaseOriginalTableName);
                    allRealTableNames.add(lowerCaseDestTableName);
                } else {
                    allRealTableNames.add(tableComparingEntity.getOriginalTableName());
                    allRealTableNames.add(tableComparingEntity.getDestTableName());
                }
            });
            Map<String, DBTable> tableName2Tables;
            try {
                tableName2Tables =
                        dbTableService.getTables(connectionSession, schemaName).entrySet().stream()
                                .filter(entry -> allRealTableNames.contains(entry.getKey()))
                                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                if (connectionSession.getDialectType().isMysql()) {
                    tableName2Tables.values().forEach(StringUtils::quoteColumnDefaultValuesForMySQL);
                }

            } catch (Exception ex) {
                log.warn("fetch table meta information failed, ex={}", ex);
                comparingEntities.forEach(tableComparingEntity -> {
                    tableComparingEntity.setComparingResult(TableComparingResult.SKIP);
                    comparingRepository.saveAll(comparingEntities);
                });
                return null;
            }
            for (TableComparingEntity entity : comparingEntities) {
                try {
                    if (!tableName2Tables.containsKey(entity.getOriginalTableName())) {
                        log.warn("original table not exists, schemaName={}, tableName={}", schemaName,
                                entity.getOriginalTableName());
                        entity.setComparingResult(TableComparingResult.SKIP);
                        entity.setOriginalTableDDL(
                                String.format("-- original table %s not exists", entity.getOriginalTableName()));
                        comparingRepository.saveAndFlush(entity);
                        continue;
                    }
                    DBTable originalTable =
                            tableName2Tables.get(entity.getOriginalTableName());
                    entity.setOriginalTableDDL(originalTable.getDDL());
                    DBTable destTable;
                    if (!tableName2Tables.containsKey(entity.getDestTableName())) {
                        log.info(
                                "shadow table not exists, will generate create table ddl, schemaName={}, originalTableName={}, destTableName={}",
                                schemaName, entity.getOriginalTableName(), entity.getDestTableName());
                        destTable = ObjectUtil.deepCopy(originalTable, DBTable.class);
                        destTable.setName(entity.getDestTableName());

                        // 如果源表有外键，需要抹掉外键名 生成 DDL，因为 MySQL 同一个 database 下不允许重复外键名
                        ignoreForeignKeyName(destTable);

                        String createDDL = dbTableService.generateCreateDDL(connectionSession, destTable).getSql();

                        entity.setComparingResult(TableComparingResult.CREATE);
                        entity.setComparingDDL(createDDL);
                        entity.setDestTableDDL(
                                "-- shadow table not exists, shadow table name is " + entity.getDestTableName());
                    } else {
                        log.info(
                                "shadow table already existed, will generate update table ddl, schemaName={}, originalTableName={}, destTableName={}",
                                schemaName, entity.getOriginalTableName(), entity.getDestTableName());
                        destTable = tableName2Tables.get(entity.getDestTableName());
                        originalTable.setName(destTable.getName());
                        String comparingDDL =
                                generateUpdateDDLWithoutRenaming(connectionSession,
                                        GenerateUpdateTableDDLReq.builder().previous(destTable).current(originalTable)
                                                .build())
                                                        .getSql();

                        entity.setDestTableDDL(destTable.getDDL());
                        entity.setComparingDDL(comparingDDL);
                        entity.setComparingResult(StringUtils.isEmpty(comparingDDL) ? TableComparingResult.NO_ACTION
                                : TableComparingResult.UPDATE);
                    }
                    /**
                     * 如果影子表存在非 HASH/KEY 二级分区，则在注释中写明不支持
                     */
                    if (Objects.nonNull(destTable.getPartition())) {
                        String partitionWarning = destTable.getPartition().getWarning();
                        if (StringUtils.isNotEmpty(partitionWarning)) {
                            entity.setComparingDDL(
                                    "-- " + destTable.getPartition().getWarning() + "\n" + entity.getComparingDDL());
                        }
                    }

                    comparingRepository.saveAndFlush(entity);
                    log.info("save entity successfully, comparingId={}", entity.getId());
                } catch (Exception ex) {
                    log.warn("meets error when comparing table structure, originalTableName={}, destTableName={}",
                            entity.getOriginalTableName(), entity.getDestTableName());
                }
            }
            log.info("shadow table comparing task done, taskId={}, schemaName={}", taskId, schemaName);
            return null;
        } finally {
            try {
                connectionSession.expire();
            } catch (Exception ex) {
                // eat exception
            }
        }
    }

    private void ignoreForeignKeyName(DBTable table) {
        // MySQL 同一个 database 下不允许外键重名，这里使用数据库默认的名称
        table.getConstraints().stream()
                .filter(constraint -> constraint.getType() == DBConstraintType.FOREIGN_KEY)
                .forEach(constraint -> constraint.setName(StringUtils.EMPTY));
    }

    public GenerateTableDDLResp generateUpdateDDLWithoutRenaming(@NotNull ConnectionSession connectionSession,
            @NotNull GenerateUpdateTableDDLReq req) {
        DBObjectEditorFactory<DBTableEditor> tableEditorFactory =
                new DBTableEditorFactory(connectionSession.getConnectType(),
                        ConnectionSessionUtil.getVersion(connectionSession));
        DBTableEditor tableEditor = tableEditorFactory.create();
        String ddl = tableEditor.generateUpdateObjectDDLWithoutRenaming(req.getPrevious(), req.getCurrent());
        return GenerateTableDDLResp.builder()
                .sql(ddl)
                .currentIdentity(TableIdentity.of(req.getCurrent().getSchemaName(), req.getCurrent().getName()))
                .previousIdentity(TableIdentity.of(req.getPrevious().getSchemaName(), req.getPrevious().getName()))
                .build();
    }
}
