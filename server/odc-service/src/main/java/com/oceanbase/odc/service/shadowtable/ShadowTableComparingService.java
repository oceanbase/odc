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

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.RejectedExecutionException;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.constant.OrganizationType;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.AccessDeniedException;
import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.metadb.iam.UserEntity;
import com.oceanbase.odc.metadb.shadowtable.ShadowTableComparingTaskEntity;
import com.oceanbase.odc.metadb.shadowtable.TableComparingEntity;
import com.oceanbase.odc.metadb.shadowtable.TableComparingRepository;
import com.oceanbase.odc.metadb.shadowtable.TableComparingTaskRepository;
import com.oceanbase.odc.service.connection.ConnectionService;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.db.DBTableService;
import com.oceanbase.odc.service.flow.ApprovalPermissionService;
import com.oceanbase.odc.service.flow.FlowInstanceService;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.shadowtable.model.SetSkippedReq;
import com.oceanbase.odc.service.shadowtable.model.ShadowTableSyncReq;
import com.oceanbase.odc.service.shadowtable.model.ShadowTableSyncReq.ShadowTableComparingMapper;
import com.oceanbase.odc.service.shadowtable.model.ShadowTableSyncResp;
import com.oceanbase.odc.service.shadowtable.model.ShadowTableSyncResp.TableComparing;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @Author: Lebie
 * @Date: 2022/9/19 下午3:45
 * @Description: []
 */
@Service
@Slf4j
@SkipAuthorize("permission check inside this.checkPermission")
public class ShadowTableComparingService {
    @Autowired
    @Qualifier("shadowTableComparingExecutor")
    private ThreadPoolTaskExecutor executor;

    @Autowired
    private ConnectionService connectionService;

    @Autowired
    private DBTableService dbTableService;

    @Autowired
    private TableComparingRepository comparingRepository;

    @Autowired
    private TableComparingTaskRepository comparingTaskRepository;

    @Autowired
    private AuthenticationFacade authenticationFacade;

    @Autowired
    private FlowInstanceService flowInstanceService;

    @Autowired
    private DatabaseService databaseService;

    @Autowired
    private ApprovalPermissionService approvalPermissionService;

    @Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRED)
    public void updateFlowInstanceId(@NonNull Long flowInstanceId, @NonNull Long id) {
        comparingTaskRepository.updateFlowInstanceIdById(id, flowInstanceId);
    }

    @PostConstruct
    public void init() {
        flowInstanceService.addShadowTableComparingHook(
                event -> updateFlowInstanceId(event.getFlowInstanceId(), event.getComparingTaskId()));
    }

    public String createShadowTableSync(@NonNull ShadowTableSyncReq shadowTableSyncReq) {
        PreConditions.validArgumentState(
                shadowTableSyncReq.getDestTableNames().size() == shadowTableSyncReq.getOriginTableNames().size(),
                ErrorCodes.BadArgument, new Object[] {}, "originalTableCount should equal to destTableCount");
        Database database = databaseService.detail(shadowTableSyncReq.getDatabaseId());
        if (Objects.isNull(database.getProject())
                && authenticationFacade.currentUser().getOrganizationType() == OrganizationType.TEAM) {
            throw new AccessDeniedException();
        }
        shadowTableSyncReq.setSchemaName(database.getName());
        ConnectionConfig connectionConfig = database.getDataSource();
        shadowTableSyncReq.setConnectionConfig(connectionConfig);

        ShadowTableComparingTaskEntity taskEntity =
                comparingTaskRepository.saveAndFlush(ShadowTableComparingMapper.ofTaskEntity(shadowTableSyncReq,
                        currentUserId()));
        List<TableComparingEntity> comparingEntities = ShadowTableComparingMapper.ofComparingEntity(shadowTableSyncReq,
                currentUserId());
        comparingEntities
                .forEach(tableComparingEntity -> tableComparingEntity.setComparingTaskId(taskEntity.getId()));
        comparingRepository.saveAll(comparingEntities);
        comparingRepository.flush();

        ShadowTableComparingTask comparingTask =
                new ShadowTableComparingTask(shadowTableSyncReq, taskEntity.getId(), dbTableService,
                        comparingRepository);
        try {
            executor.submit(comparingTask);
        } catch (RejectedExecutionException ex) {
            log.warn("submit shadowtable comparing task failed, reason=", ex);
            throw new BadRequestException("create shadowtable comparing task failed, please try again later");
        }
        return String.valueOf(taskEntity.getId());
    }

    public ShadowTableSyncResp listShadowTableSyncs(@NonNull Long id) {
        ShadowTableComparingTaskEntity taskEntity =
                comparingTaskRepository.findById(id).orElseThrow(
                        () -> new NotFoundException(ResourceType.ODC_SHADOWTABLE_COMPARING_TASK, "taskId", id));

        checkPermission(taskEntity);

        List<TableComparingEntity> comparingEntities =
                comparingRepository.findByComparingTaskId(taskEntity.getId());
        ShadowTableSyncResp resp = new ShadowTableSyncResp();
        resp.setId(taskEntity.getId());
        if (CollectionUtils.isEmpty(comparingEntities)) {
            resp.setProgressPercentage(1D);
            resp.setCompleted(true);
            return resp;
        }
        resp.setTables(comparingEntities.stream().map(TableComparing::toTableComparingWithoutDDL)
                .collect(Collectors.toList()));

        long totalTableCount = resp.getTables().size();
        long completedTableCount =
                resp.getTables().stream().filter(comparing -> comparing.getComparingResult().isDone()).count();
        resp.setProgressPercentage(completedTableCount * 100.0D / totalTableCount);
        resp.setCompleted(new BigDecimal(String.valueOf(resp.getProgressPercentage()))
                .compareTo(new BigDecimal(String.valueOf(100.0D))) == 0);
        if (resp.isCompleted()) {
            StringBuilder allDDL = new StringBuilder();
            comparingEntities.forEach(comparing -> {
                allDDL.append("-- originalTable: ").append(comparing.getOriginalTableName()).append(", destTable: ")
                        .append(comparing.getDestTableName()).append("\n");
                allDDL.append(comparing.getComparingDDL());
                allDDL.append("\n");
            });
            resp.setAllDDL(allDDL.toString());
        }
        return resp;
    }

    public TableComparing getTableComparing(@NonNull Long id, @NonNull Long tableComparingId) {
        List<TableComparingEntity> entities = innerGetTableComparing(id, Arrays.asList(tableComparingId));
        Verify.verify(entities.size() == 1, "tableComparing is not unique");
        return TableComparing.toTableComparing(entities.get(0));
    }

    public List<TableComparing> setSkipTableComparing(@NonNull Long id,
            @NonNull SetSkippedReq setSkippedReq) {
        List<TableComparingEntity> entities = innerGetTableComparing(id, setSkippedReq.getTableComparingIds());
        entities.forEach(tableComparingEntity -> tableComparingEntity.setSkipped(setSkippedReq.getSetSkip()));
        comparingRepository.saveAll(entities);
        comparingRepository.flush();
        return entities.stream().map(TableComparing::toTableComparing).collect(Collectors.toList());
    }

    private List<TableComparingEntity> innerGetTableComparing(@NonNull Long id, @NonNull List<Long> tableComparingId) {
        ShadowTableComparingTaskEntity taskEntity = comparingTaskRepository.findById(id).orElseThrow(
                () -> new NotFoundException(ResourceType.ODC_SHADOWTABLE_COMPARING_TASK, "taskId", id));

        checkPermission(taskEntity);

        List<TableComparingEntity> comparingEntity =
                comparingRepository.findAllById(tableComparingId);
        return comparingEntity;
    }

    private Long currentUserId() {
        return authenticationFacade.currentUserId();
    }

    /**
     * 权限校验，以下两种情况允许查看影子表对比结果： 1. 当前用户是影子表结构对比任务的创建者 2. 当前用户是影子表同步任务的审批人
     */
    private void checkPermission(ShadowTableComparingTaskEntity taskEntity) {
        if (currentUserId().equals(taskEntity.getCreatorId())) {
            return;
        }
        Map<Long, Set<UserEntity>> flowInstanceId2Users = approvalPermissionService
                .getApproverByFlowInstanceIds(Collections.singleton(taskEntity.getFlowInstanceId()));
        Set<Long> approvalUserIds =
                flowInstanceId2Users.get(taskEntity.getFlowInstanceId()).stream().filter(Objects::nonNull)
                        .map(UserEntity::getId).collect(
                                Collectors.toSet());
        if (approvalUserIds.contains(currentUserId())) {
            return;
        }
        throw new AccessDeniedException();
    }
}
