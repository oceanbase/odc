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
package com.oceanbase.odc.migrate.jdbc.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.sql.DataSource;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import com.oceanbase.odc.common.jpa.InsertSqlTemplateBuilder;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.util.JdbcOperationsUtil;
import com.oceanbase.odc.config.jpa.OdcJpaRepository.ValueGetterBuilder;
import com.oceanbase.odc.core.migrate.JdbcMigratable;
import com.oceanbase.odc.core.migrate.Migratable;
import com.oceanbase.odc.core.shared.constant.TaskErrorStrategy;
import com.oceanbase.odc.metadb.partitionplan.PartitionPlanEntity;
import com.oceanbase.odc.metadb.partitionplan.PartitionPlanEntity_;
import com.oceanbase.odc.metadb.partitionplan.PartitionPlanTableEntity;
import com.oceanbase.odc.metadb.partitionplan.PartitionPlanTableEntity_;
import com.oceanbase.odc.metadb.partitionplan.PartitionPlanTablePartitionKeyEntity;
import com.oceanbase.odc.metadb.partitionplan.PartitionPlanTablePartitionKeyEntity_;
import com.oceanbase.odc.plugin.task.api.partitionplan.datatype.TimeDataType;
import com.oceanbase.odc.plugin.task.api.partitionplan.invoker.create.PartitionExprGenerator;
import com.oceanbase.odc.plugin.task.api.partitionplan.invoker.partitionname.DateBasedPartitionNameGenerator;
import com.oceanbase.odc.plugin.task.api.partitionplan.invoker.partitionname.PartitionNameGenerator;
import com.oceanbase.odc.plugin.task.api.partitionplan.model.DateBasedPartitionNameGeneratorConfig;
import com.oceanbase.odc.plugin.task.api.partitionplan.model.TimeIncreaseGeneratorConfig;
import com.oceanbase.odc.service.partitionplan.model.PartitionPlanConfig;
import com.oceanbase.odc.service.partitionplan.model.PartitionPlanStrategy;
import com.oceanbase.odc.service.partitionplan.model.PartitionPlanTableConfig;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link V42417HistoricalPartitionPlanMigrate}
 *
 * @author yh263208
 * @date 2024-03-11 17:07
 * @since ODC_release_4.2.4
 */
@Slf4j
@Migratable(version = "4.2.4.17", description = "migrate historical partition plan data")
public class V42417HistoricalPartitionPlanMigrate implements JdbcMigratable {

    @Override
    public void migrate(DataSource dataSource) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        List<PartitionPlanEntityWrapper> partitionPlanWrappers = generatePartitionPlanEntities(jdbcTemplate);
        if (CollectionUtils.isEmpty(partitionPlanWrappers)) {
            log.info("No historical partition plan exists and the migration is complete");
            return;
        }
        TransactionTemplate transactionTemplate = JdbcOperationsUtil.getTransactionTemplate(dataSource);
        transactionTemplate.execute(status -> {
            try {
                batchCreatePartiPlans(jdbcTemplate, new ArrayList<>(partitionPlanWrappers));
                partitionPlanWrappers.forEach(i -> i.getPartitionPlanTableEntityWrappers()
                        .forEach(j -> j.setPartitionPlanId(i.getId())));
                batchCreatePartiPlanTables(jdbcTemplate, partitionPlanWrappers.stream()
                        .flatMap(i -> i.getPartitionPlanTableEntityWrappers().stream())
                        .collect(Collectors.toList()));
                partitionPlanWrappers.forEach(i -> i.getPartitionPlanTableEntityWrappers()
                        .forEach(j -> j.getPartitionKeyEntities()
                                .forEach(k -> k.setPartitionplanTableId(j.getId()))));
                batchCreatePartiPlanTableKeys(jdbcTemplate, partitionPlanWrappers.stream()
                        .flatMap(i -> i.getPartitionPlanTableEntityWrappers().stream()
                                .flatMap(j -> j.getPartitionKeyEntities().stream()))
                        .collect(Collectors.toList()));
                batchUpdateScheduleParameterJson(jdbcTemplate, partitionPlanWrappers);
            } catch (Exception e) {
                log.warn("Failed to migrate historical partition plan", e);
                status.setRollbackOnly();
                throw new IllegalStateException("failed to migrate historical partition plan");
            }
            return null;
        });
    }

    private List<PartitionPlanEntityWrapper> generatePartitionPlanEntities(JdbcTemplate jdbcTemplate) {
        List<PartitionPlanEntityWrapper> wrappers = jdbcTemplate.query("select id, "
                + "flow_instance_id, creator_id, modifier_id, database_id, schedule_id "
                + "from connection_partition_plan where is_config_enabled=1", (rs, rowNum) -> {
                    PartitionPlanEntityWrapper entity = new PartitionPlanEntityWrapper();
                    entity.setEnabled(true);
                    entity.setScheduleId(rs.getLong("schedule_id"));
                    entity.setHistoricalPartiId(rs.getLong("id"));
                    entity.setLastModifierId(rs.getLong("modifier_id"));
                    entity.setFlowInstanceId(rs.getLong("flow_instance_id"));
                    entity.setCreatorId(rs.getLong("creator_id"));
                    entity.setDatabaseId(rs.getLong("database_id"));
                    return entity;
                });
        if (CollectionUtils.isEmpty(wrappers)) {
            return wrappers;
        }
        setPartitionPlanTables(jdbcTemplate, wrappers);
        return wrappers;
    }

    private void setPartitionPlanTables(JdbcTemplate jdbcTemplate, List<PartitionPlanEntityWrapper> entityWrappers) {
        String ids = entityWrappers.stream().map(p -> p.getHistoricalPartiId() + "").collect(Collectors.joining(","));
        Map<Long, PartitionPlanEntityWrapper> historicalId2Entity = entityWrappers.stream()
                .collect(Collectors.toMap(PartitionPlanEntityWrapper::getHistoricalPartiId, w -> w));
        Map<Long, List<PartitionPlanTableEntityWrapper>> tableWrappers = jdbcTemplate.query("select table_name, "
                + "partition_interval, partition_interval_unit, pre_create_partition_count, "
                + "expire_period, expire_period_unit, partition_naming_prefix, partition_naming_suffix_expression, "
                + "database_partition_plan_id from table_partition_plan "
                + "where database_partition_plan_id in (" + ids + ")", (rs, rowNum) -> {
                    PartitionPlanTableEntityWrapper entity = new PartitionPlanTableEntityWrapper();
                    entity.setEnabled(true);
                    entity.setHistoricalPartiId(rs.getLong("database_partition_plan_id"));
                    entity.setScheduleId(historicalId2Entity.get(entity.getHistoricalPartiId()).getScheduleId());
                    entity.setTableName(rs.getString("table_name"));
                    int timePrecision = getTimePrecision(Integer.parseInt(rs.getString("partition_interval_unit")));
                    DateBasedPartitionNameGeneratorConfig config = new DateBasedPartitionNameGeneratorConfig();
                    config.setRefUpperBoundIndex(0);
                    config.setNamingSuffixExpression(rs.getString("partition_naming_suffix_expression"));
                    config.setNamingPrefix(rs.getString("partition_naming_prefix"));
                    Map<String, Object> parameters = new HashMap<>();
                    parameters.put(PartitionNameGenerator.PARTITION_NAME_GENERATOR_KEY, config);
                    entity.setPartitionNameInvoker(DateBasedPartitionNameGenerator.GENERATOR_NAME);
                    entity.setPartitionNameInvokerParameters(JsonUtils.toJson(parameters));

                    PartitionPlanTablePartitionKeyEntity createEntity = new PartitionPlanTablePartitionKeyEntity();
                    createEntity.setStrategy(PartitionPlanStrategy.CREATE);
                    TimeIncreaseGeneratorConfig createConfig = new TimeIncreaseGeneratorConfig();
                    createConfig.setFromCurrentTime(true);
                    createConfig.setIntervalPrecision(timePrecision);
                    createConfig.setInterval(rs.getInt("partition_interval"));
                    Map<String, Object> createParameters = new HashMap<>();
                    createParameters.put(PartitionExprGenerator.GENERATE_COUNT_KEY,
                            rs.getInt("pre_create_partition_count"));
                    createParameters.put(PartitionExprGenerator.GENERATOR_PARAMETER_KEY, createConfig);
                    createEntity.setPartitionKeyInvoker("HISTORICAL_PARTITION_PLAN_CREATE_GENERATOR");
                    createEntity.setPartitionKeyInvokerParameters(JsonUtils.toJson(createParameters));

                    PartitionPlanTablePartitionKeyEntity dropEntity = new PartitionPlanTablePartitionKeyEntity();
                    dropEntity.setStrategy(PartitionPlanStrategy.DROP);
                    Map<String, Object> dropParameters = new HashMap<>();
                    dropParameters.put("periodUnit",
                            getCalendarUnit(Integer.parseInt(rs.getString("expire_period_unit"))));
                    dropParameters.put("expirePeriod", rs.getInt("expire_period"));
                    dropEntity.setPartitionKeyInvoker("HISTORICAL_PARTITION_PLAN_DROP_GENERATOR");
                    dropEntity.setPartitionKeyInvokerParameters(JsonUtils.toJson(dropParameters));
                    entity.setPartitionKeyEntities(Arrays.asList(createEntity, dropEntity));
                    return entity;
                }).stream().collect(Collectors.groupingBy(PartitionPlanTableEntityWrapper::getHistoricalPartiId));
        entityWrappers.forEach(w -> w.setPartitionPlanTableEntityWrappers(tableWrappers.get(w.getHistoricalPartiId())));
    }

    private int getTimePrecision(int periodUnit) {
        switch (periodUnit) {
            case 0:
                return TimeDataType.YEAR;
            case 1:
                return TimeDataType.MONTH;
            case 2:
                return TimeDataType.DAY;
            default:
                throw new IllegalArgumentException("Illegal period unit, " + periodUnit);
        }
    }

    private int getCalendarUnit(int periodUnit) {
        switch (periodUnit) {
            case 0:
                return 1;
            case 1:
                return 2;
            case 2:
                return 5;
            default:
                throw new IllegalArgumentException("Illegal period unit, " + periodUnit);
        }
    }

    private void batchCreatePartiPlans(JdbcTemplate jdbcTemplate, List<PartitionPlanEntity> entities) {
        String sql = InsertSqlTemplateBuilder.from("partitionplan")
                .field(PartitionPlanEntity_.flowInstanceId)
                .field("is_enabled")
                .field(PartitionPlanEntity_.creatorId)
                .field(PartitionPlanEntity_.lastModifierId)
                .field(PartitionPlanEntity_.databaseId)
                .build();
        ValueGetterBuilder<PartitionPlanEntity> valueGetter = new ValueGetterBuilder<>();
        List<Function<PartitionPlanEntity, Object>> getter = valueGetter
                .add(PartitionPlanEntity::getFlowInstanceId)
                .add(PartitionPlanEntity::getEnabled)
                .add(PartitionPlanEntity::getCreatorId)
                .add(PartitionPlanEntity::getLastModifierId)
                .add(PartitionPlanEntity::getDatabaseId)
                .build();
        Map<Integer, Function<PartitionPlanEntity, Object>> valueGetterMap = new HashMap<>();
        IntStream.range(1, getter.size() + 1).forEach(i -> valueGetterMap.put(i, getter.get(i - 1)));
        JdbcOperationsUtil.batchCreate(jdbcTemplate, entities, sql, valueGetterMap, PartitionPlanEntity::setId);
        log.info("[1/4] Partition plan table migration completed, "
                + "tableName=partitionplan, entityCount={}", entities.size());
    }

    private void batchCreatePartiPlanTables(JdbcTemplate jdbcTemplate, List<PartitionPlanTableEntity> entities) {
        String sql = InsertSqlTemplateBuilder.from("partitionplan_table")
                .field(PartitionPlanTableEntity_.tableName)
                .field("partitionplan_id")
                .field(PartitionPlanTableEntity_.scheduleId)
                .field("is_enabled")
                .field(PartitionPlanTableEntity_.partitionNameInvoker)
                .field(PartitionPlanTableEntity_.partitionNameInvokerParameters)
                .build();
        ValueGetterBuilder<PartitionPlanTableEntity> valueGetter = new ValueGetterBuilder<>();
        List<Function<PartitionPlanTableEntity, Object>> getter = valueGetter
                .add(PartitionPlanTableEntity::getTableName)
                .add(PartitionPlanTableEntity::getPartitionPlanId)
                .add(PartitionPlanTableEntity::getScheduleId)
                .add(PartitionPlanTableEntity::getEnabled)
                .add(PartitionPlanTableEntity::getPartitionNameInvoker)
                .add(PartitionPlanTableEntity::getPartitionNameInvokerParameters)
                .build();
        Map<Integer, Function<PartitionPlanTableEntity, Object>> valueGetterMap = new HashMap<>();
        IntStream.range(1, getter.size() + 1).forEach(i -> valueGetterMap.put(i, getter.get(i - 1)));
        JdbcOperationsUtil.batchCreate(jdbcTemplate, entities, sql, valueGetterMap, PartitionPlanTableEntity::setId);
        log.info("[2/4] Partition plan table migration completed, "
                + "tableName=partitionplan_table, entityCount={}", entities.size());
    }

    private void batchCreatePartiPlanTableKeys(JdbcTemplate jdbcTemplate,
            List<PartitionPlanTablePartitionKeyEntity> entities) {
        String sql = InsertSqlTemplateBuilder.from("partitionplan_table_partitionkey")
                .field(PartitionPlanTablePartitionKeyEntity_.partitionKey)
                .field(PartitionPlanTablePartitionKeyEntity_.strategy)
                .field(PartitionPlanTablePartitionKeyEntity_.partitionplanTableId)
                .field(PartitionPlanTablePartitionKeyEntity_.partitionKeyInvoker)
                .field(PartitionPlanTablePartitionKeyEntity_.partitionKeyInvokerParameters)
                .build();
        ValueGetterBuilder<PartitionPlanTablePartitionKeyEntity> valueGetter = new ValueGetterBuilder<>();
        List<Function<PartitionPlanTablePartitionKeyEntity, Object>> getter = valueGetter
                .add(PartitionPlanTablePartitionKeyEntity::getPartitionKey)
                .add(p -> p.getStrategy().name())
                .add(PartitionPlanTablePartitionKeyEntity::getPartitionplanTableId)
                .add(PartitionPlanTablePartitionKeyEntity::getPartitionKeyInvoker)
                .add(PartitionPlanTablePartitionKeyEntity::getPartitionKeyInvokerParameters)
                .build();
        Map<Integer, Function<PartitionPlanTablePartitionKeyEntity, Object>> valueGetterMap = new HashMap<>();
        IntStream.range(1, getter.size() + 1).forEach(i -> valueGetterMap.put(i, getter.get(i - 1)));
        JdbcOperationsUtil.batchCreate(jdbcTemplate, entities, sql, valueGetterMap,
                PartitionPlanTablePartitionKeyEntity::setId);
        log.info("[3/4] Partition plan table migration completed, "
                + "tableName=partitionplan_table_partitionkey, entityCount={}", entities.size());
    }

    private void batchUpdateScheduleParameterJson(JdbcTemplate jdbcTemplate,
            List<PartitionPlanEntityWrapper> wrappers) {
        List<Object[]> params = wrappers.stream().map(wrapper -> {
            PartitionPlanConfig config = mapToModel(wrapper);
            return new Object[] {JsonUtils.toJson(config), wrapper.getScheduleId()};
        }).collect(Collectors.toList());
        jdbcTemplate.batchUpdate("update schedule_schedule set job_parameters_json=? where id=?", params);
        log.info("[4/4] Zoning plan parameter revision completed, tableName=schedule_schedule");
    }

    private PartitionPlanConfig mapToModel(PartitionPlanEntityWrapper wrapper) {
        PartitionPlanConfig parameter = new PartitionPlanConfig();
        parameter.setErrorStrategy(TaskErrorStrategy.ABORT);
        parameter.setTimeoutMillis(172800000L);
        parameter.setId(wrapper.getId());
        parameter.setTaskId(0L);
        parameter.setFlowInstanceId(wrapper.getFlowInstanceId());
        parameter.setPartitionTableConfigs(wrapper.getPartitionPlanTableEntityWrappers().stream().map(i -> {
            PartitionPlanTableConfig cfg = new PartitionPlanTableConfig();
            cfg.setId(i.getId());
            return cfg;
        }).collect(Collectors.toList()));
        return parameter;
    }

    @Getter
    @Setter
    private static class PartitionPlanEntityWrapper extends PartitionPlanEntity {
        private Long historicalPartiId;
        private Long scheduleId;
        private List<PartitionPlanTableEntityWrapper> partitionPlanTableEntityWrappers;
    }

    @Getter
    @Setter
    private static class PartitionPlanTableEntityWrapper extends PartitionPlanTableEntity {
        private Long historicalPartiId;
        private List<PartitionPlanTablePartitionKeyEntity> partitionKeyEntities;
    }

}
