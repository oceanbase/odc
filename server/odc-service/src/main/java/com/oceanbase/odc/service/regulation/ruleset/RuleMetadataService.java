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
package com.oceanbase.odc.service.regulation.ruleset;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.metadb.regulation.ruleset.MetadataEntity;
import com.oceanbase.odc.metadb.regulation.ruleset.RuleMetadataRepository;
import com.oceanbase.odc.metadb.regulation.ruleset.RuleMetadataSpecs;
import com.oceanbase.odc.service.regulation.ruleset.model.QueryRuleMetadataParams;
import com.oceanbase.odc.service.regulation.ruleset.model.RuleMetadata;

import lombok.NonNull;

/**
 * @Author: Lebie
 * @Date: 2023/5/22 17:07
 * @Description: []
 */
@Service
public class RuleMetadataService {
    @Autowired
    private RuleMetadataRepository metadataRepository;

    @Transactional(rollbackFor = Exception.class)
    @SkipAuthorize("internal authenticated")
    public List<RuleMetadata> list(QueryRuleMetadataParams params) {
        Specification<MetadataEntity> specs = RuleMetadataSpecs.typeIn(params.getRuleTypes())
                .and(RuleMetadataSpecs.labels(params.getLabels()));
        return metadataRepository.findAll(specs).stream().map(RuleMetadataMapper::fromEntity)
                .collect(Collectors.toList());
    }

    @SkipAuthorize("internal authenticated")
    public RuleMetadata detail(@NonNull Long id) {
        return RuleMetadataMapper.fromEntity(metadataRepository.findById(id).orElseThrow(() -> new NotFoundException(
                ErrorCodes.NotFound, null, "ruleMetadata not found, id=" + id)));
    }
}
