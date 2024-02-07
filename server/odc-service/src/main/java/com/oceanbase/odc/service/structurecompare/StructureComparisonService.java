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
package com.oceanbase.odc.service.structurecompare;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.AccessDeniedException;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.metadb.iam.UserEntity;
import com.oceanbase.odc.metadb.structurecompare.StructureComparisonEntitySpecs;
import com.oceanbase.odc.metadb.structurecompare.StructureComparisonTaskEntity;
import com.oceanbase.odc.metadb.structurecompare.StructureComparisonTaskRepository;
import com.oceanbase.odc.metadb.structurecompare.StructureComparisonTaskResultEntity;
import com.oceanbase.odc.metadb.structurecompare.StructureComparisonTaskResultRepository;
import com.oceanbase.odc.service.common.response.Responses;
import com.oceanbase.odc.service.flow.ApprovalPermissionService;
import com.oceanbase.odc.service.flow.task.model.DBObjectStructureComparisonResp;
import com.oceanbase.odc.service.flow.task.model.DBStructureComparisonResp;
import com.oceanbase.odc.service.flow.task.model.DBStructureComparisonResp.ObjectComparisonResult;
import com.oceanbase.odc.service.flow.task.model.DBStructureComparisonResp.OperationType;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.objectstorage.ObjectStorageFacade;
import com.oceanbase.odc.service.objectstorage.model.StorageObject;
import com.oceanbase.odc.service.structurecompare.model.DBObjectComparisonResult;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @author jingtian
 * @date 2024/1/18
 * @since ODC_release_4.2.4
 */
@Slf4j
@Service
@SkipAuthorize("permission check inside")
public class StructureComparisonService {
    @Autowired
    private StructureComparisonTaskRepository structureComparisonTaskRepository;
    @Autowired
    private StructureComparisonTaskResultRepository structureComparisonTaskResultRepository;
    @Autowired
    private ObjectStorageFacade objectStorageFacade;
    @Autowired
    private AuthenticationFacade authenticationFacade;
    @Autowired
    private ApprovalPermissionService approvalPermissionService;
    /**
     * Maximum number of bytes returned by total sql change script, default value 1 MB
     */
    private final Long MAX_TOTAL_SCRIPT_SIZE_BYTES = 1048576L;

    public DBStructureComparisonResp getDBStructureComparisonResult(@NonNull Long id, OperationType operationType,
            String dbObjectName, @NotNull Pageable pageable) {
        StructureComparisonTaskEntity taskEntity = structureComparisonTaskRepository.findById(id).orElseThrow(
                () -> new NotFoundException(ResourceType.ODC_STRUCTURE_COMPARISON_TASK, "id", id));
        checkPermission(taskEntity);

        Page<StructureComparisonTaskResultEntity> entities;
        Specification<StructureComparisonTaskResultEntity> specification =
                Specification.where(StructureComparisonEntitySpecs.comparisonTaskIdEquals(id));
        if (operationType != null) {
            specification = specification
                    .and(StructureComparisonEntitySpecs.comparisonResultEquals(operationType.getComparisonResult()));
        }
        if (dbObjectName != null) {
            specification = specification.and(StructureComparisonEntitySpecs.dbObjectNameLike(dbObjectName));
        }
        entities = structureComparisonTaskResultRepository.findAll(specification, pageable);

        DBStructureComparisonResp resp = new DBStructureComparisonResp();
        resp.setId(id);
        resp.setOverSizeLimit(false);
        try {
            StorageObject storageObject =
                    objectStorageFacade.loadObject("structure-comparison".concat(File.separator)
                            .concat(authenticationFacade.currentUserIdStr()), taskEntity.getStorageObjectId());
            Validate.notNull(storageObject, "StorageObject can not be null");
            Validate.notNull(storageObject.getMetadata(), "ObjectMetadata can not be null");
            if (storageObject.getMetadata().getTotalLength() > MAX_TOTAL_SCRIPT_SIZE_BYTES) {
                resp.setOverSizeLimit(true);
            } else {
                resp.setTotalChangeScript(
                        IOUtils.toString(storageObject.getContent(), StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            log.warn(
                    "StructureComparisonService failed to load total sql script from objectStorageFacade", e);
        }
        resp.setStorageObjectId(taskEntity.getStorageObjectId());
        if (!entities.isEmpty()) {
            resp.setComparisonResults(Responses.paginated(entities.map(ObjectComparisonResult::fromEntity)));
        }

        return resp;
    }

    public DBObjectStructureComparisonResp getDBObjectStructureComparisonResult(@NonNull Long comparisonTaskId,
            @NonNull Long id) {
        StructureComparisonTaskEntity taskEntity =
                structureComparisonTaskRepository.findById(comparisonTaskId).orElseThrow(
                        () -> new NotFoundException(ResourceType.ODC_STRUCTURE_COMPARISON_TASK, "id", id));
        checkPermission(taskEntity);
        StructureComparisonTaskResultEntity entity =
                structureComparisonTaskResultRepository.findById(id).orElseThrow(() -> new NotFoundException(
                        ErrorCodes.NotFound, null, "StructureComparisonTaskResultEntity not found, id=" + id));
        return DBObjectStructureComparisonResp.fromEntity(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public List<StructureComparisonTaskResultEntity> batchCreateTaskResults(List<DBObjectComparisonResult> results,
            DialectType dialectType, Long taskId) {
        return structureComparisonTaskResultRepository.batchCreate(
                results.stream().map(result -> result.toEntity(taskId, dialectType)).collect(Collectors.toList()));
    }

    /**
     * Permission verification, the following two situations allow you to view the structure comparison
     * results: 1. The current user is the creator of the structure comparison task 2. The current user
     * is the approver of the structure comparison task
     */
    private void checkPermission(@NonNull StructureComparisonTaskEntity taskEntity) {
        if (Objects.equals(currentUserId(), taskEntity.getCreatorId())) {
            return;
        }
        Map<Long, Set<UserEntity>> flowInstanceId2Users = approvalPermissionService
                .getApproverByFlowInstanceIds(Collections.singleton(taskEntity.getFlowInstanceId()));
        Set<Long> approvalUserIds = flowInstanceId2Users.get(taskEntity.getFlowInstanceId()).stream()
                .filter(Objects::nonNull).map(UserEntity::getId).collect(Collectors.toSet());
        if (approvalUserIds.contains(currentUserId())) {
            return;
        }
        throw new AccessDeniedException("The permission verification of the structure comparison task failed.");
    }

    private Long currentUserId() {
        return authenticationFacade.currentUserId();
    }

}
