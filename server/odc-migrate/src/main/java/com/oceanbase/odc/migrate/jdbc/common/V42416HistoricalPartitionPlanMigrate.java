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
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.jdbc.core.JdbcTemplate;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.migrate.JdbcMigratable;
import com.oceanbase.odc.core.migrate.Migratable;
import com.oceanbase.odc.metadb.partitionplan.PartitionPlanEntity;
import com.oceanbase.odc.metadb.partitionplan.PartitionPlanRepository;
import com.oceanbase.odc.metadb.partitionplan.PartitionPlanTableEntity;
import com.oceanbase.odc.metadb.partitionplan.PartitionPlanTablePartitionKeyEntity;
import com.oceanbase.odc.metadb.partitionplan.PartitionPlanTablePartitionKeyRepository;
import com.oceanbase.odc.metadb.partitionplan.PartitionPlanTableRepository;
import com.oceanbase.odc.plugin.task.api.partitionplan.datatype.TimeDataType;
import com.oceanbase.odc.plugin.task.api.partitionplan.invoker.create.PartitionExprGenerator;
import com.oceanbase.odc.plugin.task.api.partitionplan.invoker.create.TimeIncreasePartitionExprGenerator;
import com.oceanbase.odc.plugin.task.api.partitionplan.invoker.partitionname.DateBasedPartitionNameGenerator;
import com.oceanbase.odc.plugin.task.api.partitionplan.invoker.partitionname.PartitionNameGenerator;
import com.oceanbase.odc.plugin.task.api.partitionplan.model.DateBasedPartitionNameGeneratorConfig;
import com.oceanbase.odc.plugin.task.api.partitionplan.model.TimeIncreaseGeneratorConfig;
import com.oceanbase.odc.service.common.util.SpringContextUtil;
import com.oceanbase.odc.service.partitionplan.model.PartitionPlanStrategy;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link V42416HistoricalPartitionPlanMigrate}
 *
 * @author yh263208
 * @date 2024-03-11 17:07
 * @since ODC_release_4.2.4
 */
@Slf4j
@Migratable(version = "4.2.4.17", description = "migrate historical partition plan data")
public class V42416HistoricalPartitionPlanMigrate implements JdbcMigratable {

    private final PartitionPlanRepository partitionPlanRepository =
            SpringContextUtil.getBean(PartitionPlanRepository.class);
    private final PartitionPlanTablePartitionKeyRepository planTablePartitionKeyRepository =
            SpringContextUtil.getBean(PartitionPlanTablePartitionKeyRepository.class);
    private final PartitionPlanTableRepository partitionPlanTableRepository =
            SpringContextUtil.getBean(PartitionPlanTableRepository.class);

    @Override
    public void migrate(DataSource dataSource) {
        clean();
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        List<PartitionPlanEntityWrapper> partitionPlanWrappers = generatePartitionPlanEntities(jdbcTemplate);
        if (CollectionUtils.isEmpty(partitionPlanWrappers)) {
            return;
        }
        try {
            partitionPlanRepository.batchCreate(new ArrayList<>(partitionPlanWrappers));
            partitionPlanWrappers.forEach(i -> i.getPartitionPlanTableEntityWrappers()
                    .forEach(j -> j.setPartitionPlanId(i.getId())));
            partitionPlanTableRepository.batchCreate(partitionPlanWrappers.stream()
                    .flatMap(i -> i.getPartitionPlanTableEntityWrappers().stream())
                    .collect(Collectors.toList()));
            partitionPlanWrappers.forEach(i -> i.getPartitionPlanTableEntityWrappers()
                    .forEach(j -> j.getPartitionKeyEntities()
                            .forEach(k -> k.setPartitionplanTableId(j.getId()))));
            planTablePartitionKeyRepository.batchCreate(partitionPlanWrappers.stream()
                    .flatMap(i -> i.getPartitionPlanTableEntityWrappers().stream()
                            .flatMap(j -> j.getPartitionKeyEntities().stream()))
                    .collect(Collectors.toList()));
        } catch (Exception e) {
            log.warn("Failed to migrate historical partition plan", e);
            clean();
            throw new IllegalStateException("failed to migrate historical partition plan");
        }
    }

    private void clean() {
        this.partitionPlanRepository.deleteAll();
        this.partitionPlanTableRepository.deleteAll();
        this.planTablePartitionKeyRepository.deleteAll();
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
                + "database_partition_plan_id, from table_partition_plan "
                + "where database_partition_plan_id in (" + ids + ")", (rs, rowNum) -> {
                    PartitionPlanTableEntityWrapper entity = new PartitionPlanTableEntityWrapper();
                    entity.setEnabled(true);
                    entity.setHistoricalPartiId(rs.getLong("database_partition_plan_id"));
                    entity.setScheduleId(historicalId2Entity.get(entity.getHistoricalPartiId()).getScheduleId());
                    entity.setTableName(rs.getString("table_name"));
                    int timePrecision =
                            getTimePrecision(Integer.parseInt(rs.getString("partition_interval_unit")));
                    DateBasedPartitionNameGeneratorConfig config = new DateBasedPartitionNameGeneratorConfig();
                    config.setFromCurrentTime(true);
                    config.setIntervalPrecision(timePrecision);
                    config.setInterval(rs.getInt("partition_interval"));
                    config.setNamingSuffixExpression(rs.getString("partition_naming_suffix_expression"));
                    config.setNamingPrefix(rs.getString("partition_naming_prefix"));
                    Map<String, Object> parameters = new HashMap<>();
                    parameters.put(PartitionNameGenerator.PARTITION_NAME_GENERATOR_KEY, config);
                    entity.setPartitionNameInvoker(DateBasedPartitionNameGenerator.GENERATOR_NAME);
                    entity.setPartitionNameInvokerParameters(JsonUtils.toJson(parameters));

                    PartitionPlanTablePartitionKeyEntity createEntity =
                            new PartitionPlanTablePartitionKeyEntity();
                    createEntity.setStrategy(PartitionPlanStrategy.CREATE);
                    TimeIncreaseGeneratorConfig createConfig = new TimeIncreaseGeneratorConfig();
                    createConfig.setFromCurrentTime(true);
                    createConfig.setIntervalPrecision(timePrecision);
                    createConfig.setInterval(rs.getInt("partition_interval"));
                    Map<String, Object> createParameters = new HashMap<>();
                    createParameters.put(PartitionExprGenerator.GENERATE_COUNT_KEY,
                            rs.getInt("pre_create_partition_count"));
                    createParameters.put(PartitionExprGenerator.GENERATOR_PARAMETER_KEY, createConfig);
                    createEntity.setPartitionKeyInvoker(TimeIncreasePartitionExprGenerator.GENERATOR_NAME);
                    createEntity.setPartitionKeyInvokerParameters(JsonUtils.toJson(createParameters));

                    PartitionPlanTablePartitionKeyEntity dropEntity =
                            new PartitionPlanTablePartitionKeyEntity();
                    dropEntity.setStrategy(PartitionPlanStrategy.DROP);
                    Map<String, Object> dropParameters = new HashMap<>();
                    dropParameters.put("periodUnit",
                            getCalendarUnit(Integer.parseInt(rs.getString("expire_period_unit"))));
                    dropParameters.put("expirePeriod", rs.getInt("expire_period"));
                    dropEntity.setPartitionKeyInvoker("KEEP_MOST_LATEST_BY_TIME_GENERATOR");
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
