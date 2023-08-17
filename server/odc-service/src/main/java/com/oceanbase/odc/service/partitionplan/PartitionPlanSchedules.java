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
package com.oceanbase.odc.service.partitionplan;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.jdbc.lock.JdbcLockRegistry;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.core.shared.constant.FlowStatus;
import com.oceanbase.odc.metadb.flow.FlowInstanceEntity;
import com.oceanbase.odc.metadb.flow.FlowInstanceRepository;
import com.oceanbase.odc.metadb.iam.UserEntity;
import com.oceanbase.odc.metadb.partitionplan.ConnectionPartitionPlanEntity;
import com.oceanbase.odc.metadb.partitionplan.ConnectionPartitionPlanRepository;
import com.oceanbase.odc.metadb.partitionplan.TablePartitionPlanEntity;
import com.oceanbase.odc.metadb.partitionplan.TablePartitionPlanRepository;
import com.oceanbase.odc.service.flow.FlowInstanceService;
import com.oceanbase.odc.service.iam.UserService;
import com.oceanbase.odc.service.iam.model.User;
import com.oceanbase.odc.service.iam.util.SecurityContextUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author：tianke
 * @Date: 2022/10/9 14:17
 * @Descripition:
 */
@Slf4j
@Component
public class PartitionPlanSchedules {

    @Autowired
    private ConnectionPartitionPlanRepository connectionPartitionPlanRepository;
    @Autowired
    private TablePartitionPlanRepository tablePartitionPlanRepository;

    @Autowired
    private FlowInstanceService flowInstanceService;

    @Autowired
    private FlowInstanceRepository flowInstanceRepository;
    @Autowired
    private PartitionPlanTaskService partitionPlanTaskService;

    @Autowired
    private UserService userService;

    @Autowired
    @Qualifier("partitionPlanJdbcLockRegistry")
    private JdbcLockRegistry jdbcLockRegistry;

    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");

    private final String resourcePrefix = "partition_plan_schedule";

    // @Scheduled(cron = "0 0 0 * * ?")
    public void alterPartitionTask() throws Exception {
        // 查找所有生效计划
        List<ConnectionPartitionPlanEntity> allValidPlan = connectionPartitionPlanRepository.findAllValidPlan();
        for (ConnectionPartitionPlanEntity connectionPartitionPlan : allValidPlan) {
            try {
                List<TablePartitionPlanEntity> tablePlans = tablePartitionPlanRepository
                        .findValidPlanByFlowInstanceId(connectionPartitionPlan.getFlowInstanceId());

                if (tablePlans.isEmpty()) {
                    continue;
                }
                // 加全局锁，避免多个实例重复执行报错。锁粒度到每个连接的分区计划。
                if (!tryLock(connectionPartitionPlan.getConnectionId())) {
                    continue;
                }
                UserEntity userEntity = userService.nullSafeGet(connectionPartitionPlan.getCreatorId());
                User taskCreator = new User(userEntity);
                SecurityContextUtils.setCurrentUser(taskCreator);

                // 查找正在审批中的子流程，并终止。
                cancelApprovingTask(connectionPartitionPlan.getFlowInstanceId());

                partitionPlanTaskService.executePartitionPlan(connectionPartitionPlan.getConnectionId(),
                        connectionPartitionPlan.getFlowInstanceId(), tablePlans, taskCreator);
            } catch (Exception e) {
                log.warn("Partition planning daily task creation failed,connectionId={},error message={}",
                        connectionPartitionPlan.getConnectionId(), e.getMessage());
            }

        }

    }

    private boolean tryLock(Long connectionId) throws InterruptedException {
        String lockKey = String.format("%s_%s_%s", resourcePrefix, connectionId,
                sdf.format(System.currentTimeMillis()));
        Lock lock = jdbcLockRegistry.obtain(lockKey);
        if (!lock.tryLock(3, TimeUnit.SECONDS)) {
            log.info("get lock failed:{}", lockKey);
            return false;
        }
        log.info("get lock:{}", lockKey);
        return true;
    }

    private void cancelApprovingTask(Long flowInstanceId) {
        List<FlowInstanceEntity> entities =
                flowInstanceRepository.findByParentInstanceId(flowInstanceId);
        for (FlowInstanceEntity entity : entities) {
            if (entity.getStatus() == FlowStatus.APPROVING) {
                log.info("sub task expired:{}", entity.getId());
                flowInstanceService.cancel(entity.getId(), false);
            }
        }
    }
}
