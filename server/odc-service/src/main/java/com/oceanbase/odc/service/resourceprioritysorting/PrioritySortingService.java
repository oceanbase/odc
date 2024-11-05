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
package com.oceanbase.odc.service.resourceprioritysorting;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.persistence.PersistenceException;

import org.apache.commons.collections4.CollectionUtils;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.metadb.resourceprioritysorting.PrioritySortingEntity;
import com.oceanbase.odc.metadb.resourceprioritysorting.PrioritySortingRepository;
import com.oceanbase.odc.service.resourceprioritysorting.model.SortedResourceId;
import com.oceanbase.odc.service.resourceprioritysorting.model.SortedResourceType;

/**
 * @author keyang
 * @date 2024/11/01
 * @since 4.3.2
 */
@Service
public class PrioritySortingService {
    private static final Long STEP = 2L ^ 16;
    private static final Integer MAX_RETRY_COUNT = 3;

    @Autowired
    private PrioritySortingRepository prioritySortingRepository;
    @Autowired
    private TransactionTemplate transactionTemplate;

    @Transactional(rollbackFor = Throwable.class)
    public PrioritySortingEntity add(SortedResourceId sortedResourceId, Long prevSortedResourceId) {
        PreConditions.notNull(sortedResourceId, "sortedResourceId");
        if (prevSortedResourceId == null) {
            return addToHighestPriority(sortedResourceId);
        }
        return addAfterSortedResource(sortedResourceId, prevSortedResourceId);
    }

    public void dragAfter(SortedResourceType sortedResourceType, Long draggedSortedResourceId,
            Long prevSortedResourceId) {
        PreConditions.notNull(sortedResourceType, "sortedResourceType");
        PreConditions.notNull(draggedSortedResourceId, "draggedSortedResourceId");
        if (Objects.equals(draggedSortedResourceId, prevSortedResourceId)) {
            // when drag after itself,do nothing
            return;
        }
        List<Long> sortedResourceIds = new ArrayList<>();
        sortedResourceIds.add(draggedSortedResourceId);
        if (prevSortedResourceId != null) {
            sortedResourceIds.add(prevSortedResourceId);
        }
        List<PrioritySortingEntity> entities =
                prioritySortingRepository.listBySortedResourceIds(sortedResourceType, sortedResourceIds);
        Map<Long, PrioritySortingEntity> idToEntityMap = entities.stream().collect(
                Collectors.toMap(PrioritySortingEntity::getSortedResourceId, Function.identity()));
        assertSortedResourceExist(sortedResourceType, draggedSortedResourceId,
                idToEntityMap.containsKey(draggedSortedResourceId));
        if (prevSortedResourceId != null) {
            assertSortedResourceExist(sortedResourceType, prevSortedResourceId,
                    idToEntityMap.containsKey(prevSortedResourceId));
        }
        PrioritySortingEntity sortedResourceEntity = idToEntityMap.get(draggedSortedResourceId);
        PrioritySortingEntity afterSortedResourceEntity = idToEntityMap.get(prevSortedResourceId);
        uniqueConstraintViolationRetry(
                () -> doDragAfter(sortedResourceType, sortedResourceEntity, afterSortedResourceEntity));
    }

    public Page<PrioritySortingEntity> pageInSortedResourceType(SortedResourceType sortedResourceType,
            Integer pageNum, Integer pageSize) {
        PreConditions.notNull(sortedResourceType, "sortedResourceType");
        PreConditions.notNull(pageNum, "pageNum");
        PreConditions.notNull(pageSize, "pageSize");
        return prioritySortingRepository.pageBySortedResourceTypeAndSortByPriorityDesc(
                sortedResourceType, pageNum, pageSize);
    }

    private void doDragAfter(SortedResourceType sortedResourceType, PrioritySortingEntity draggedSortedResourceEntity,
            PrioritySortingEntity afterSortedResourceEntity) {
        Long prevPriority =
                afterSortedResourceEntity == null ? null : afterSortedResourceEntity.getPriority();
        Long candidatePriority = calculateAfterResourcePriority(sortedResourceType, prevPriority);
        if (afterSortedResourceEntity != null && prevPriority.equals(candidatePriority + 1L)) {
            // no more priority value assigned to the dragged resource, reset all priority values in resource
            transactionTemplate.executeWithoutResult(status -> {
                resetPriorityInResource(sortedResourceType, draggedSortedResourceEntity.getSortedResourceId(),
                        afterSortedResourceEntity.getSortedResourceId(), true);
            });
            return;
        }

        if (!candidatePriority.equals(draggedSortedResourceEntity.getPriority())) {
            draggedSortedResourceEntity.setPriority(candidatePriority);
            prioritySortingRepository.save(draggedSortedResourceEntity);
        }
    }

    private PrioritySortingEntity addAfterSortedResource(SortedResourceId sortedResourceId,
            Long prevSortedResourceId) {
        PreConditions.notNull(prevSortedResourceId, "prevSortedResourceId");
        Optional<PrioritySortingEntity> afterSortedResourceOptional =
                prioritySortingRepository
                        .findBySortedResourceId(new SortedResourceId(sortedResourceId, prevSortedResourceId));
        assertSortedResourceExist(sortedResourceId, prevSortedResourceId,
                afterSortedResourceOptional.isPresent());
        Long prevPriority = afterSortedResourceOptional.get().getPriority();
        Long candidatePriority =
                calculateAfterResourcePriority(sortedResourceId, prevPriority);
        if (prevPriority.equals(candidatePriority + 1L)) {
            // no more priority value assigned to the dragged resource, reset all priority values in resource
            return resetPriorityInResource(sortedResourceId, sortedResourceId.getSortedResourceId(),
                    prevSortedResourceId, false);
        }

        PrioritySortingEntity prioritySortingEntity = PrioritySortingEntity.builder()
                .resourceType(sortedResourceId.getResourceType())
                .resourceId(sortedResourceId.getResourceId())
                .sortedResourceType(sortedResourceId.getSortedResourceType())
                .sortedResourceId(sortedResourceId.getSortedResourceId())
                .priority(candidatePriority)
                .build();
        prioritySortingRepository.save(prioritySortingEntity);
        return prioritySortingEntity;
    }

    private PrioritySortingEntity addToHighestPriority(SortedResourceId sortedResourceId) {
        Optional<PrioritySortingEntity> maxPriorityOptional =
                prioritySortingRepository.maxPriorityInSortedResourceType(sortedResourceId);
        Long priority = maxPriorityOptional.map(PrioritySortingEntity::getPriority)
                .orElse(0L) + STEP;

        PrioritySortingEntity prioritySortingEntity = PrioritySortingEntity.builder()
                .resourceType(sortedResourceId.getResourceType())
                .resourceId(sortedResourceId.getResourceId())
                .sortedResourceType(sortedResourceId.getSortedResourceType())
                .sortedResourceId(sortedResourceId.getSortedResourceId())
                .priority(priority)
                .build();

        uniqueConstraintViolationRetry(() -> {
            prioritySortingRepository.save(prioritySortingEntity);
            prioritySortingEntity.setPriority(priority + STEP);
        });
        return prioritySortingEntity;
    }

    /**
     *
     * @param sortedResourceType
     * @param opSortedResourceId dragged/added resource id
     * @param prevSortedResourceId prev resource id dragged/added after
     * @param isDragged true: drag after,false: add after
     */
    private PrioritySortingEntity resetPriorityInResource(SortedResourceType sortedResourceType,
            Long opSortedResourceId, Long prevSortedResourceId, boolean isDragged) {
        // prevSortedResourceId must not be null.
        PreConditions.notNull(prevSortedResourceId, "prevSortedResourceId");

        // Get and lock all sorted resources in resource
        List<PrioritySortingEntity> prioritySortingEntities =
                prioritySortingRepository.listAllInSortedResourceTypeWithWriteLock(sortedResourceType);
        if (CollectionUtils.isEmpty(prioritySortingEntities)) {
            Optional<ResourceType> resourceTypeOptional =
                    ResourceType.getByName(sortedResourceType.getSortedResourceType());
            throw new BadRequestException(ErrorCodes.NotFound,
                    new Object[] {
                            resourceTypeOptional.map(ResourceType::getLocalizedMessage)
                                    .orElse(sortedResourceType.getSortedResourceType()),
                            "prevSortedResourceId", prevSortedResourceId},
                    "can't find prevSortedResourceId:" + prevSortedResourceId + " in sortedResourceType:"
                            + sortedResourceType);
        }

        // Sort by priority asc
        prioritySortingEntities.sort(Comparator.comparing(
                PrioritySortingEntity::getPriority));

        // Drag resource to after
        List<PrioritySortingEntity> draggedEntities = new ArrayList<>();
        Integer draggedResourceIndex = null;
        PrioritySortingEntity draggedEntity = null;
        int i = 0;
        for (PrioritySortingEntity prioritySortingEntity : prioritySortingEntities) {
            if (prioritySortingEntity.getSortedResourceId().equals(opSortedResourceId)) {
                draggedEntity = prioritySortingEntity;
                continue;
            }
            if (prioritySortingEntity.getSortedResourceId().equals(prevSortedResourceId)) {
                draggedResourceIndex = i;
                draggedEntities.add(null);
                i++;
            }
            draggedEntities.add(prioritySortingEntity);
            i++;
        }
        if (isDragged) {
            // draggedEntity==null means draggedSortedResourceId is not exist in prioritySortingEntities,
            assertSortedResourceExist(sortedResourceType, opSortedResourceId,
                    draggedEntity != null);
        } else {
            assertSortedResourceNotExist(sortedResourceType, opSortedResourceId,
                    draggedEntity != null);
            draggedEntity = PrioritySortingEntity.builder()
                    .resourceType(sortedResourceType.getResourceType())
                    .resourceId(sortedResourceType.getResourceId())
                    .sortedResourceType(sortedResourceType.getSortedResourceType())
                    .sortedResourceId(opSortedResourceId)
                    .priority(0L)
                    .build();
        }
        // draggedResourceIndex==null means prevSortedResourceId is not found in prioritySortingEntities,
        assertSortedResourceExist(sortedResourceType, prevSortedResourceId,
                draggedResourceIndex != null);
        draggedEntities.set(draggedResourceIndex, draggedEntity);

        // Reset all priority values in resource
        long candidatePriority = STEP;
        for (PrioritySortingEntity entity : draggedEntities) {
            entity.setPriority(candidatePriority);
            candidatePriority += STEP;
        }

        // save all sorted resources in resource
        prioritySortingRepository.saveAllAndFlush(draggedEntities);

        return draggedEntity;
    }

    private Long calculateAfterResourcePriority(SortedResourceType sortedResourceType, Long prevPriority) {
        Optional<PrioritySortingEntity> entityOptional =
                prevPriority == null ? prioritySortingRepository.maxPriorityInSortedResourceType(sortedResourceType)
                        : prioritySortingRepository.maxPriorityInSortedResourceTypeLessThanPriority(sortedResourceType,
                                prevPriority);

        Long nextPriority = entityOptional.map(PrioritySortingEntity::getPriority).orElse(0L);
        if (prevPriority == null) {
            prevPriority = nextPriority + 2 * STEP;
        }
        return (nextPriority + prevPriority) / 2;
    }

    private void assertSortedResourceExist(
            SortedResourceType sortedResourceType, Long sortedResourceId,
            boolean isExist) {
        if (isExist) {
            return;
        }
        Optional<ResourceType> resourceTypeOptional =
                ResourceType.getByName(sortedResourceType.getSortedResourceType());
        throw new BadRequestException(ErrorCodes.NotFound,
                new Object[] {
                        resourceTypeOptional.map(ResourceType::getLocalizedMessage)
                                .orElse(sortedResourceType.getSortedResourceType()),
                        "sortedResourceId", sortedResourceId},
                "can't find sortedResourceId:" + sortedResourceId + " in sortedResourceType:" + sortedResourceType);
    }

    private void assertSortedResourceNotExist(
            SortedResourceType sortedResourceType, Long sortedResourceId,
            boolean isExist) {
        if (!isExist) {
            return;
        }
        Optional<ResourceType> resourceTypeOptional =
                ResourceType.getByName(sortedResourceType.getSortedResourceType());
        throw new BadRequestException(ErrorCodes.DuplicatedExists,
                new Object[] {
                        resourceTypeOptional.map(ResourceType::getLocalizedMessage)
                                .orElse(sortedResourceType.getSortedResourceType()),
                        "sortedResourceId", sortedResourceId},
                "Already exist sortedResourceId:" + sortedResourceId + " in sortedResourceType:" + sortedResourceType);
    }

    private boolean isUniqueConstraintViolation(Exception ex) {
        if (!(ex instanceof PersistenceException)) {
            return false;
        }

        Throwable cause = ex;
        while (cause != null) {
            if (cause instanceof ConstraintViolationException) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private void uniqueConstraintViolationRetry(Runnable r) {
        int retry = 1;
        while (true) {
            try {
                r.run();
                break;
            } catch (Exception e) {
                retry++;
                if (!isUniqueConstraintViolation(e) || retry >= MAX_RETRY_COUNT) {
                    throw e;
                }
            }
        }
    }
}
