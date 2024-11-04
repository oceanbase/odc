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
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.persistence.PersistenceException;

import org.apache.commons.collections4.CollectionUtils;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
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
    private static final Integer MAX_RETRY_COUNT = 5;

    @Autowired
    private PrioritySortingRepository prioritySortingRepository;
    @Autowired
    private TransactionTemplate transactionTemplate;

    public PrioritySortingEntity add(SortedResourceId sortedResourceId) {
        PreConditions.notNull(sortedResourceId, "sortedResourceId");
        Optional<Long> maxPriorityOptional =
                prioritySortingRepository.maxPriorityInSortedResourceType(sortedResourceId);
        Long priority = maxPriorityOptional.orElse(0L) + STEP;

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

    public Page<PrioritySortingEntity> pageInSortedResourceType(SortedResourceType sortedResourceType,
            Integer pageNum, Integer pageSize) {
        PreConditions.notNull(sortedResourceType, "sortedResourceType");
        PreConditions.notNull(pageNum, "pageNum");
        PreConditions.notNull(pageSize, "pageSize");
        return prioritySortingRepository.pageBySortedResourceTypeAndSortByPriorityDesc(
                sortedResourceType, pageNum, pageSize);
    }

    public void dragBefore(SortedResourceType sortedResourceType, Long draggedSortedResourceId,
            Long beforeSortedResourceId) {
        PreConditions.notNull(sortedResourceType, "sortedResourceType");
        PreConditions.notNull(draggedSortedResourceId, "draggedSortedResourceId");
        List<Long> sortedResourceIds = new ArrayList<>();
        sortedResourceIds.add(draggedSortedResourceId);
        if (beforeSortedResourceId != null) {
            sortedResourceIds.add(beforeSortedResourceId);
        }
        List<PrioritySortingEntity> entities =
                prioritySortingRepository.listBySortedResourceIds(sortedResourceType, sortedResourceIds);
        Map<Long, PrioritySortingEntity> idToEntityMap = entities.stream().collect(
                Collectors.toMap(PrioritySortingEntity::getSortedResourceId, Function.identity()));
        assertSortedResourceExist(sortedResourceType, draggedSortedResourceId,
                idToEntityMap.containsKey(draggedSortedResourceId));
        if (beforeSortedResourceId != null) {
            assertSortedResourceExist(sortedResourceType, beforeSortedResourceId,
                    idToEntityMap.containsKey(beforeSortedResourceId));
        }
        PrioritySortingEntity sortedResourceEntity = idToEntityMap.get(draggedSortedResourceId);
        PrioritySortingEntity beforeSortedResourceEntity = idToEntityMap.get(beforeSortedResourceId);
        uniqueConstraintViolationRetry(
                () -> doDragBefore(sortedResourceType, sortedResourceEntity, beforeSortedResourceEntity));
    }

    private void doDragBefore(SortedResourceType sortedResourceType, PrioritySortingEntity draggedSortedResourceEntity,
            PrioritySortingEntity beforeSortedResourceEntity) {
        Long startPriority = beforeSortedResourceEntity == null ? 0L : beforeSortedResourceEntity.getPriority();
        Long candidatePriority = calculatePriority(sortedResourceType, startPriority);
        if (startPriority.equals(candidatePriority)) {
            // no more priority value assigned to the dragged resource, reset all priority values in resource
            resetPriorityInResource(sortedResourceType, draggedSortedResourceEntity.getSortedResourceId(),
                    beforeSortedResourceEntity == null ? null : beforeSortedResourceEntity.getSortedResourceId());
            return;
        }

        if (!candidatePriority.equals(draggedSortedResourceEntity.getPriority())) {
            draggedSortedResourceEntity.setPriority(candidatePriority);
            prioritySortingRepository.save(draggedSortedResourceEntity);
        }
    }

    private void resetPriorityInResource(SortedResourceType sortedResourceType, Long draggedSortedResourceId,
            Long beforeSortedResourceId) {
        transactionTemplate.executeWithoutResult(status -> {
            // Get and lock all sorted resources in resource
            List<PrioritySortingEntity> prioritySortingEntities =
                    prioritySortingRepository.listAllInSortedResourceTypeWithWriteLock(sortedResourceType);
            if (CollectionUtils.isEmpty(prioritySortingEntities)) {
                return;
            }

            // Sort by priority desc
            prioritySortingEntities.sort(Comparator.comparing(
                    PrioritySortingEntity::getPriority).reversed());

            // Drag resource to before
            Long maxPriority = STEP;
            List<PrioritySortingEntity> draggedEntities = new ArrayList<>();
            Integer draggedResourceIndex = null;
            PrioritySortingEntity draggedEntity = null;
            int i = 0;
            for (PrioritySortingEntity prioritySortingEntity : prioritySortingEntities) {
                if (prioritySortingEntity.getSortedResourceId().equals(draggedSortedResourceId)) {
                    draggedEntity = prioritySortingEntity;
                    continue;
                }
                if (prioritySortingEntity.getSortedResourceId().equals(beforeSortedResourceId)) {
                    draggedResourceIndex = i;
                    draggedEntities.add(null);
                    i++;
                }
                draggedEntities.add(prioritySortingEntity);
                i++;
            }
            // draggedEntity==null means draggedSortedResourceId is not exist in prioritySortingEntities,
            assertSortedResourceExist(sortedResourceType, draggedSortedResourceId,
                    draggedEntity != null);
            if (beforeSortedResourceId != null) {
                // draggedResourceIndex==null means beforeSortedResourceId is not found in prioritySortingEntities,
                assertSortedResourceExist(sortedResourceType, beforeSortedResourceId,
                        draggedResourceIndex != null);
            }
            if (draggedResourceIndex != null) {
                draggedEntities.set(draggedResourceIndex, draggedEntity);
            } else {
                draggedEntities.add(draggedEntity);
            }

            // Reset all priority values in resource
            long step = maxPriority / prioritySortingEntities.size();
            Long candidatePriority = maxPriority;
            for (PrioritySortingEntity entity : draggedEntities) {
                entity.setPriority(candidatePriority);
                candidatePriority = step;
            }

            // save all sorted resources in resource
            prioritySortingRepository.saveAllAndFlush(draggedEntities);
        });
    }

    private Long calculatePriority(SortedResourceType sortedResourceType, Long startPriority) {
        Long candidateEndPriority = startPriority + STEP;
        Optional<PrioritySortingEntity> entityOptional =
                prioritySortingRepository.getFirstBySortedResourceTypeBetweenPriorityWithPriorityAsc(sortedResourceType,
                        startPriority, candidateEndPriority);
        candidateEndPriority = entityOptional.map(PrioritySortingEntity::getPriority).orElse(candidateEndPriority);
        return (candidateEndPriority + startPriority) / 2;
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
