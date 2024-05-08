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
package com.oceanbase.odc.service.datasecurity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.oceanbase.odc.common.security.OdcBigDecimalChecker;
import com.oceanbase.odc.core.authority.util.Authenticated;
import com.oceanbase.odc.core.authority.util.PreAuthenticate;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.datamasking.config.MaskConfig;
import com.oceanbase.odc.core.datamasking.masker.AbstractDataMasker;
import com.oceanbase.odc.core.datamasking.masker.DataMaskerFactory;
import com.oceanbase.odc.core.datamasking.masker.MaskValueType;
import com.oceanbase.odc.core.datamasking.masker.ValueMeta;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.metadb.datasecurity.MaskingAlgorithmEntity;
import com.oceanbase.odc.metadb.datasecurity.MaskingAlgorithmRepository;
import com.oceanbase.odc.metadb.datasecurity.MaskingAlgorithmSpecs;
import com.oceanbase.odc.metadb.datasecurity.MaskingSegmentEntity;
import com.oceanbase.odc.metadb.datasecurity.MaskingSegmentRepository;
import com.oceanbase.odc.metadb.iam.UserEntity;
import com.oceanbase.odc.metadb.iam.UserRepository;
import com.oceanbase.odc.service.common.model.InnerUser;
import com.oceanbase.odc.service.datasecurity.model.MaskingAlgorithm;
import com.oceanbase.odc.service.datasecurity.model.MaskingAlgorithmType;
import com.oceanbase.odc.service.datasecurity.model.MaskingSegment;
import com.oceanbase.odc.service.datasecurity.model.MaskingSegmentsType;
import com.oceanbase.odc.service.datasecurity.model.QueryMaskingAlgorithmParams;
import com.oceanbase.odc.service.datasecurity.util.MaskingAlgorithmMapper;
import com.oceanbase.odc.service.datasecurity.util.MaskingAlgorithmUtil;
import com.oceanbase.odc.service.datasecurity.util.MaskingSegmentMapper;
import com.oceanbase.odc.service.iam.HorizontalDataPermissionValidator;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @author gaoda.xy
 * @date 2023/5/17 10:12
 */
@Slf4j
@Service
@Validated
@Authenticated
public class MaskingAlgorithmService {

    @Autowired
    private MaskingAlgorithmRepository algorithmRepository;

    @Autowired
    private MaskingSegmentRepository segmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthenticationFacade authenticationFacade;

    @Autowired
    private HorizontalDataPermissionValidator horizontalPermissionValidator;

    private final MaskingAlgorithmMapper algorithmMapper = MaskingAlgorithmMapper.INSTANCE;
    private final MaskingSegmentMapper segmentMapper = MaskingSegmentMapper.INSTANCE;
    private final LoadingCache<Long, List<MaskingAlgorithm>> organizationId2Algorithms = Caffeine.newBuilder()
            .maximumSize(10).expireAfterAccess(24, TimeUnit.HOURS).build(this::loadMaskingAlgorithmByOrganizationId);
    private static final String SYSTEM_DEFAULT_ALGORITHM_NAME =
            "${com.oceanbase.odc.builtin-resource.masking-algorithm.mask-all.name}";

    @Transactional(rollbackFor = Exception.class)
    @PreAuthenticate(actions = "create", resourceType = "ODC_MASKING_ALGORITHM", isForAll = true)
    public Boolean exists(@NotBlank String name) {
        MaskingAlgorithmEntity entity = new MaskingAlgorithmEntity();
        entity.setName(name);
        entity.setOrganizationId(authenticationFacade.currentOrganizationId());
        return algorithmRepository.exists(Example.of(entity));
    }

    @Transactional(rollbackFor = Exception.class)
    @PreAuthenticate(actions = "create", resourceType = "ODC_MASKING_ALGORITHM", isForAll = true)
    public MaskingAlgorithm create(@NotNull @Valid MaskingAlgorithm algorithm) {
        Long userId = authenticationFacade.currentUserId();
        Long organizationId = authenticationFacade.currentOrganizationId();
        PreConditions.validNoDuplicated(ResourceType.ODC_MASKING_ALGORITHM, "name", algorithm.getName(),
                () -> exists(algorithm.getName()));
        MaskingAlgorithmEntity entity = algorithmMapper.modelToEntity(algorithm);
        entity.setBuiltin(false);
        entity.setCreatorId(userId);
        entity.setOrganizationId(organizationId);
        entity.setMaskedContent(internalTest(algorithm).getMaskedContent());
        algorithmRepository.save(entity);
        if (algorithm.getSegmentsType() == MaskingSegmentsType.CUSTOM) {
            List<MaskingSegment> segments = algorithm.getSegments();
            for (int i = 0; i < segments.size(); i++) {
                MaskingSegmentEntity segmentEntity = segmentMapper.modelToEntity(segments.get(i));
                segmentEntity.setOrdinal(i);
                segmentEntity.setMaskingAlgorithmId(entity.getId());
                segmentEntity.setCreatorId(userId);
                segmentEntity.setOrganizationId(organizationId);
                segmentRepository.save(segmentEntity);
            }
        }
        log.info("Masking algorithm has been created, id={}, name={}", entity.getId(), entity.getName());
        return detail(entity.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    @SkipAuthorize("All users can access the masking algorithm")
    public MaskingAlgorithm detail(@NotNull Long id) {
        MaskingAlgorithmEntity entity = nullSafeGet(id);
        MaskingAlgorithm algorithm = algorithmMapper.entityToModel(entity);
        horizontalPermissionValidator.checkCurrentOrganization(algorithm);
        if (Objects.nonNull(entity.getCreatorId())) {
            Optional<UserEntity> userEntityOptional = userRepository.findById(entity.getCreatorId());
            userEntityOptional.ifPresent(user -> algorithm.setCreator(new InnerUser(user)));
        }
        if (entity.getSegmentsType() == MaskingSegmentsType.CUSTOM) {
            List<MaskingSegmentEntity> segmentEntities =
                    segmentRepository.findByMaskingAlgorithmIdOrderByOrdinalAsc(entity.getId());
            algorithm.setSegments(
                    segmentEntities.stream().map(segmentMapper::entityToModel).collect(Collectors.toList()));
        }
        return algorithm;
    }

    @Transactional(rollbackFor = Exception.class)
    @SkipAuthorize("All users can access the masking algorithm")
    public Page<MaskingAlgorithm> list(@NotNull QueryMaskingAlgorithmParams params, @NotNull Pageable pageable) {
        Long organizationId = authenticationFacade.currentOrganizationId();
        Specification<MaskingAlgorithmEntity> spec = Specification
                .where(MaskingAlgorithmSpecs.nameLike(params.getFuzzyName()))
                .and(MaskingAlgorithmSpecs.typeIn(params.getTypes()))
                .and(MaskingAlgorithmSpecs.organizationIdEqual(organizationId));
        return algorithmRepository.findAll(spec, pageable).map(algorithmMapper::entityToModel);
    }

    @SkipAuthorize("All users can access the masking algorithm")
    public MaskingAlgorithm test(@NotNull Long id, @NotBlank String sample) {
        MaskingAlgorithm algorithm = detail(id);
        algorithm.setSampleContent(sample);
        return internalTest(algorithm);
    }

    @SkipAuthorize("odc internal usages")
    public MaskingAlgorithmEntity nullSafeGet(@NonNull Long id) {
        return algorithmRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ResourceType.ODC_MASKING_ALGORITHM, "id", id));
    }

    @SkipAuthorize("odc internal usages")
    public List<MaskingAlgorithm> batchNullSafeGetModel(@NotNull Set<Long> ids) {
        List<MaskingAlgorithmEntity> entities = algorithmRepository.findByIdIn(ids);
        if (ids.size() > entities.size()) {
            Set<Long> presentIds = entities.stream().map(MaskingAlgorithmEntity::getId).collect(Collectors.toSet());
            String absentIds = ids.stream().filter(id -> !presentIds.contains(id)).map(Object::toString)
                    .collect(Collectors.joining(","));
            throw new NotFoundException(ResourceType.ODC_MASKING_ALGORITHM, "id", absentIds);
        }
        return entities.stream().map(algorithmMapper::entityToModel).collect(Collectors.toList());
    }

    @SkipAuthorize("odc internal usages")
    public Long getDefaultAlgorithmIdByOrganizationId(@NonNull Long organizationId) {
        List<MaskingAlgorithmEntity> entities =
                algorithmRepository.findByNameAndOrganizationId(SYSTEM_DEFAULT_ALGORITHM_NAME, organizationId);
        Verify.singleton(entities, "maskingAlgorithmEntities");
        return entities.get(0).getId();
    }

    @SkipAuthorize("odc internal usages")
    public List<MaskingAlgorithm> getMaskingAlgorithmsByOrganizationId(@NonNull Long organizationId) {
        return organizationId2Algorithms.get(organizationId);
    }

    private MaskingAlgorithm internalTest(@NotNull MaskingAlgorithm algorithm) {
        MaskConfig maskConfig = MaskingAlgorithmUtil.toSingleFieldMaskConfig(algorithm, "test_field");
        DataMaskerFactory maskerFactory = new DataMaskerFactory();
        AbstractDataMasker masker = maskerFactory.createDataMasker(MaskValueType.SINGLE_VALUE.name(), maskConfig);
        ValueMeta valueMeta = new ValueMeta("string", "test_field");
        if (algorithm.getType() == MaskingAlgorithmType.ROUNDING) {
            valueMeta.setDataType("double");
            Verify.verify(OdcBigDecimalChecker.checkBigDecimalDoS(algorithm.getSampleContent()),
                    "Invalid double value");
        }
        String result = masker.mask(algorithm.getSampleContent(), valueMeta);
        algorithm.setMaskedContent(result);
        return algorithm;
    }

    private List<MaskingAlgorithm> loadMaskingAlgorithmByOrganizationId(@NonNull Long organizationId) {
        List<MaskingAlgorithmEntity> algorithmEntities = algorithmRepository.findByOrganizationId(organizationId);
        List<Long> ids = algorithmEntities.stream().map(MaskingAlgorithmEntity::getId).collect(Collectors.toList());
        List<MaskingSegmentEntity> segmentEntities = segmentRepository.findByMaskingAlgorithmIdInOrderByOrdinalAsc(ids);
        Map<Long, List<MaskingSegment>> algorithmId2Segments = new HashMap<>();
        for (MaskingSegmentEntity entity : segmentEntities) {
            MaskingSegment segment = segmentMapper.entityToModel(entity);
            List<MaskingSegment> segments =
                    algorithmId2Segments.computeIfAbsent(entity.getMaskingAlgorithmId(), e -> new ArrayList<>());
            segments.add(segment);
        }
        return algorithmEntities.stream().map(entity -> {
            MaskingAlgorithm algorithm = algorithmMapper.entityToModel(entity);
            if (algorithm.getSegmentsType() == MaskingSegmentsType.CUSTOM) {
                algorithm.setSegments(algorithmId2Segments.get(algorithm.getId()));
            }
            return algorithm;
        }).collect(Collectors.toList());
    }

}
