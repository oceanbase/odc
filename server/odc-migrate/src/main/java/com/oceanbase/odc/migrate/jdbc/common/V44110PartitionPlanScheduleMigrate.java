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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.nimbusds.oauth2.sdk.util.CollectionUtils;
import com.oceanbase.odc.core.migrate.JdbcMigratable;
import com.oceanbase.odc.core.migrate.Migratable;
import com.oceanbase.odc.service.partitionplan.model.PartitionPlanStrategy;
import com.oceanbase.odc.service.schedule.model.ScheduleType;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Migratable(version = "4.4.1.10", description = "migrate the partition plan to schedule_relations")
public class V44110PartitionPlanScheduleMigrate implements JdbcMigratable {

    private JdbcTemplate jdbcTemplate;
    private TransactionTemplate transactionTemplate;

    @Override
    public void migrate(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.transactionTemplate = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        transactionTemplate.setTimeout(3600);
        List<PartitionPlanToScheduleEntity> partitionPlanToScheduleList = listPartitionPlanToScheduleEntities();
        if (CollectionUtils.isEmpty(partitionPlanToScheduleList)) {
            log.info("partition plan schedule migrate skipped, partitionPlanToScheduleList is empty");
            return;
        }

        List<Object[]> resultList = filterMigrateSchedule(partitionPlanToScheduleList);

        if (resultList.isEmpty()) {
            log.info("partition plan schedule migrate skipped, resultList is empty");
            return;
        }
        log.info("partition plan schedule migrate started, resultList size={}", resultList.size());
        transactionTemplate.execute(status -> {
            try {
                // batch update schedule_relations
                int affectRows = batchInsertScheduleRelations(resultList);
                log.info("partition plan schedule migrate affectRows={}", affectRows);
                // mark child schedule is_inner = true
                batchUpdateScheduleIsInner();
                log.info("mark child schedule is_inner = true finished");
            } catch (Exception e) {
                log.error("partition plan schedule migrate failed", e);
                status.setRollbackOnly();
                throw new RuntimeException("partition plan schedule migrate failed", e);
            }
            return null;
        });

    }

    private List<PartitionPlanToScheduleEntity> listPartitionPlanToScheduleEntities() {
        String sql = "SELECT distinct a.schedule_id, a.partitionplan_id, ptp.strategy  "
                + "FROM ( "
                + "    SELECT pt.id, pt.schedule_id, pt.partitionplan_id "
                + "    FROM partitionplan_table pt "
                + "    WHERE pt.partitionplan_id IN ( "
                + "        SELECT id  "
                + "        FROM partitionplan "
                + "        WHERE is_enabled = 1 "
                + "    ) and pt.is_enabled = 1 "
                + ") a  "
                + "LEFT JOIN partitionplan_table_partitionkey ptp "
                + "ON a.id = ptp.partitionplan_table_id;";
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(PartitionPlanToScheduleEntity.class));
    }

    private int batchInsertScheduleRelations(List<Object[]> resultList) {
        String sql = "insert into schedule_relations (parent_id, child_id, schedule_type) values (?, ?, ?)";
        return jdbcTemplate.batchUpdate(sql, resultList).length;
    }

    private void batchUpdateScheduleIsInner() {
        final String selectSql = "SELECT child_id FROM schedule_relations";
        List<Long> childIds = jdbcTemplate.queryForList(selectSql, Long.class);
        if (!childIds.isEmpty()) {
            final String updateSql = "UPDATE schedule_schedule SET is_inner = 1 WHERE id = ?";
            jdbcTemplate.batchUpdate(updateSql, childIds, childIds.size(),
                    (ps, id) -> ps.setLong(1, id));
        }
    }

    private List<Object[]> filterMigrateSchedule(List<PartitionPlanToScheduleEntity> partitionPlanToScheduleList) {
        Map<Long, List<PartitionPlanToScheduleEntity>> groupedByPartitionPlanId = partitionPlanToScheduleList.stream()
                .collect(Collectors.groupingBy(
                        PartitionPlanToScheduleEntity::getPartitionplanId,
                        Collectors.collectingAndThen(
                                Collectors.toMap(PartitionPlanToScheduleEntity::getScheduleId, entity -> entity,
                                        (existing, replacement) -> existing),
                                map -> new ArrayList<>(map.values()))));

        List<Object[]> resultList = new ArrayList<>();

        for (Entry<Long, List<PartitionPlanToScheduleEntity>> entry : groupedByPartitionPlanId.entrySet()) {
            List<PartitionPlanToScheduleEntity> plans = entry.getValue();
            if (plans.size() == 2) {
                PartitionPlanToScheduleEntity entity0 = plans.get(0);
                PartitionPlanToScheduleEntity entity1 = plans.get(1);
                if (entity0.getStrategy() != entity1.getStrategy()) {
                    Long parentId = entity0.getStrategy() == PartitionPlanStrategy.CREATE ? entity0.getScheduleId()
                            : entity1.getScheduleId();
                    Long childId = entity0.getStrategy() == PartitionPlanStrategy.CREATE ? entity1.getScheduleId()
                            : entity0.getScheduleId();
                    resultList.add(new Object[] {parentId, childId, ScheduleType.PARTITION_PLAN});
                }
            }
        }
        return resultList;
    }

    @Getter
    @Setter
    private static class PartitionPlanToScheduleEntity {
        private Long scheduleId;
        private Long partitionplanId;
        private PartitionPlanStrategy strategy;
    }
}
