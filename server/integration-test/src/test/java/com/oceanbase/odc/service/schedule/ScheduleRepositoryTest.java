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
package com.oceanbase.odc.service.schedule;

import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.metadb.schedule.ScheduleChangeLogEntity;
import com.oceanbase.odc.metadb.schedule.ScheduleChangeLogRepository;
import com.oceanbase.odc.metadb.schedule.ScheduleEntity;
import com.oceanbase.odc.metadb.schedule.ScheduleRepository;
import com.oceanbase.odc.service.quartz.model.MisfireStrategy;
import com.oceanbase.odc.service.schedule.model.OperationType;
import com.oceanbase.odc.service.schedule.model.QueryScheduleParams;
import com.oceanbase.odc.service.schedule.model.ScheduleChangeStatus;
import com.oceanbase.odc.service.schedule.model.ScheduleStatus;
import com.oceanbase.odc.service.schedule.model.ScheduleType;

/**
 * @author longpeng.zlp
 * @date 2025/07/17 15:35
 */
public class ScheduleRepositoryTest extends ServiceTestEnv {

    @Autowired
    private ScheduleRepository scheduleRepository;
    @Autowired
    private ScheduleChangeLogRepository scheduleChangeLogRepository;
    private Long dateDeleteApproveScheduleID;
    private Long dateArchiveApproveScheduleID;


    @Before
    public void setUp() {
        // Clear existing data
        scheduleRepository.deleteAll();
        scheduleRepository.flush();
        scheduleChangeLogRepository.deleteAll();
        scheduleRepository.flush();
        // insert data
        createScheduleEntity(ScheduleType.DATA_DELETE, ScheduleChangeStatus.SUCCESS, true);
        dateDeleteApproveScheduleID =
                createScheduleEntity(ScheduleType.DATA_DELETE, ScheduleChangeStatus.APPROVING, true);
        createScheduleEntity(ScheduleType.DATA_ARCHIVE, ScheduleChangeStatus.SUCCESS, true);
        dateArchiveApproveScheduleID =
                createScheduleEntity(ScheduleType.DATA_ARCHIVE, ScheduleChangeStatus.APPROVING, true);
        createScheduleEntity(ScheduleType.DATA_ARCHIVE, ScheduleChangeStatus.APPROVING, false);
    }

    private Long createScheduleEntity(ScheduleType scheduleType, ScheduleChangeStatus changeStatus,
            boolean shouldRelated) {
        ScheduleEntity schedule = new ScheduleEntity();
        schedule.setName("test");
        schedule.setOrganizationId(1L);
        schedule.setDataSourceId(1L);
        schedule.setDatabaseId(1L);
        schedule.setDatabaseName("test");
        schedule.setProjectId(1L);
        schedule.setType(scheduleType);
        schedule.setAllowConcurrent(true);
        schedule.setMisfireStrategy(MisfireStrategy.MISFIRE_INSTRUCTION_DO_NOTHING);
        schedule.setStatus(ScheduleStatus.CREATING);
        schedule.setCreateTime(new Date(Instant.now().toEpochMilli()));
        schedule.setUpdateTime(new Date(Instant.now().toEpochMilli()));
        schedule.setCreatorId(1L);
        schedule.setModifierId(1L);
        schedule.setDescription("test");
        schedule.setIsInner(false);
        schedule.setJobParametersJson("{}");
        schedule.setTriggerConfigJson("{}");
        ScheduleChangeLogEntity changeLog = new ScheduleChangeLogEntity();

        ScheduleEntity scheduleEntity = scheduleRepository.saveAndFlush(schedule);
        changeLog.setScheduleId(scheduleEntity.getId());
        changeLog.setFlowInstanceId(scheduleEntity.getId());
        changeLog.setType(OperationType.CREATE);
        changeLog.setStatus(changeStatus);
        changeLog.setCreateTime(new Date(Instant.now().toEpochMilli()));
        changeLog.setUpdateTime(new Date(Instant.now().toEpochMilli()));
        ScheduleChangeLogEntity scheduleChangeLogEntity = scheduleChangeLogRepository.saveAndFlush(changeLog);
        if (shouldRelated) {
            scheduleRepository.updateLatestScheduleChangeLogIdById(scheduleEntity.getId(),
                    scheduleChangeLogEntity.getId());
        }
        return scheduleEntity.getId();
    }

    @Test
    public void test_list_dataDelete_with_approving() {
        QueryScheduleParams queryScheduleParams = QueryScheduleParams.builder()
                .latestScheduleChangeStatuses(Collections.singleton(ScheduleChangeStatus.APPROVING))
                .organizationId(1L)
                .projectIds(Collections.singleton(1L))
                .type(ScheduleType.DATA_DELETE)
                .build();
        List<ScheduleEntity> entityPageable =
                scheduleRepository.findWithJoinScheduleChangeLog(Pageable.ofSize(1), queryScheduleParams).toList();
        Assert.assertEquals(entityPageable.size(), 1);
        Assert.assertEquals(entityPageable.get(0).getId().longValue(), dateDeleteApproveScheduleID.longValue());
    }

    @Test
    public void test_list_dataArchive_with_approving() {
        QueryScheduleParams queryScheduleParams = QueryScheduleParams.builder()
                .latestScheduleChangeStatuses(Collections.singleton(ScheduleChangeStatus.APPROVING))
                .organizationId(1L)
                .projectIds(Collections.singleton(1L))
                .type(ScheduleType.DATA_ARCHIVE)
                .build();
        List<ScheduleEntity> entityPageable =
                scheduleRepository.findWithJoinScheduleChangeLog(Pageable.ofSize(1), queryScheduleParams).toList();
        Assert.assertEquals(entityPageable.size(), 1);
        Assert.assertEquals(entityPageable.get(0).getId().longValue(), dateArchiveApproveScheduleID.longValue());
    }

    @Test
    public void test_list_dataArchive_with_approving_not_match() {
        QueryScheduleParams queryScheduleParams = QueryScheduleParams.builder()
                .latestScheduleChangeStatuses(Collections.singleton(ScheduleChangeStatus.APPROVING))
                .organizationId(1L)
                .projectIds(Collections.singleton(2L))
                .type(ScheduleType.DATA_ARCHIVE)
                .build();
        List<ScheduleEntity> entityPageable =
                scheduleRepository.findWithJoinScheduleChangeLog(Pageable.ofSize(1), queryScheduleParams).toList();
        Assert.assertEquals(entityPageable.size(), 0);
    }

}
