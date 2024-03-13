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

import javax.sql.DataSource;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.core.migrate.JdbcMigratable;
import com.oceanbase.odc.metadb.partitionplan.PartitionPlanRepository;
import com.oceanbase.odc.metadb.partitionplan.PartitionPlanTablePartitionKeyRepository;
import com.oceanbase.odc.metadb.partitionplan.PartitionPlanTableRepository;

/**
 * Test cases for {@link V42417HistoricalPartitionPlanMigrate}
 *
 * @author yh263208
 * @date 2024-03-12 11:22
 * @since ODC_release_4.2.4
 */
public class V42417HistoricalPartitionPlanMigrateTest extends ServiceTestEnv {

    @Autowired
    private DataSource dataSource;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private PartitionPlanRepository partitionPlanRepository;
    @Autowired
    private PartitionPlanTablePartitionKeyRepository planTablePartitionKeyRepository;
    @Autowired
    private PartitionPlanTableRepository partitionPlanTableRepository;

    @Before
    public void setUp() {
        this.partitionPlanRepository.deleteAll();
        this.partitionPlanTableRepository.deleteAll();
        this.planTablePartitionKeyRepository.deleteAll();
        this.jdbcTemplate.execute("delete from table_partition_plan where 1=1");
        this.jdbcTemplate.execute("delete from connection_partition_plan where 1=1");
        this.jdbcTemplate.execute("INSERT INTO `connection_partition_plan` (`id`, `connection_id`, "
                + "`organization_id`, `flow_instance_id`, `inspect_trigger_strategy`, `is_inspect_enabled`, "
                + "`is_config_enabled`, `creator_id`, `modifier_id`, `database_id`, `schedule_id`) "
                + "VALUES (1, 1, 1, 1, '3', 0, 1, 1, 1, 4, 8), (2, 1, 1, 2, '3', 0, 1, 1, 1, 4, 20)");
        this.jdbcTemplate.execute("INSERT INTO `table_partition_plan` (`id`, `connection_id`, `organization_id`, "
                + "`flow_instance_id`, `schema_name`, `table_name`, `is_config_enabled`, `is_auto_partition`, "
                + "`partition_interval`, `partition_interval_unit`, `pre_create_partition_count`, `expire_period`, "
                + "`expire_period_unit`, `partition_naming_prefix`, `partition_naming_suffix_expression`, "
                + "`creator_id`, `modifier_id`, `database_id`, `database_partition_plan_id`) VALUES "
                + "(1, 1, 1, 1, 'test', 'datetime_range_parti_tbl', 0, 1, 12, '1', 5, 12, '1', 'p', 'yyyy_MM', 1, 1, 4, 1), "
                + "(2, 1, 1, 1, 'test', 'date_range_parti_tbl', 0, 1, 24, '1', 10, 24, '1', 'p', 'yyyy_MM', 1, 1, 4, 1), "
                + "(3, 1, 1, 1, 'test', 'unixtimestamp_range_parti_tbl', 0, 1, 36, '1', 15, 36, '1', 'p', 'yyyyMMdd', 1, 1, 4, 1), "
                + "(4, 1, 1, 2, 'test', 'datetime_range_parti_tbl', 1, 1, 12, '1', 5, 12, '1', 'p', 'yyyy_MM', 1, 1, 4, 2), "
                + "(5, 1, 1, 2, 'test', 'date_range_parti_tbl', 1, 1, 24, '1', 10, 24, '1', 'p', 'yyyy_MM', 1, 1, 4, 2), "
                + "(6, 1, 1, 2, 'test', 'unixtimestamp_range_parti_tbl', 1, 1, 36, '1', 15, 36, '1', 'p', 'yyyyMMdd', 1, 1, 4, 2)");
    }

    @Test
    public void migrate_historicalPartiPlanExists_succeed() {
        JdbcMigratable jdbcMigratable = new V42417HistoricalPartitionPlanMigrate();
        jdbcMigratable.migrate(dataSource);
        Assert.assertEquals(2, this.partitionPlanRepository.findAll().size());
        Assert.assertEquals(6, this.partitionPlanTableRepository.findAll().size());
        Assert.assertEquals(12, this.planTablePartitionKeyRepository.findAll().size());
    }

}
