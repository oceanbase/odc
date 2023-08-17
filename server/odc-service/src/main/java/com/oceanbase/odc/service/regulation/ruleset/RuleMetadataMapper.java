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
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.metadb.regulation.ruleset.MetadataEntity;
import com.oceanbase.odc.metadb.regulation.ruleset.MetadataLabelEntity;
import com.oceanbase.odc.service.regulation.ruleset.model.MetadataLabel;
import com.oceanbase.odc.service.regulation.ruleset.model.RuleMetadata;

/**
 * @Author: Lebie
 * @Date: 2023/5/22 19:44
 * @Description: []
 */
public class RuleMetadataMapper {
    public static RuleMetadata fromEntity(MetadataEntity entity) {
        RuleMetadata model = new RuleMetadata();
        model.setId(entity.getId());
        model.setName(entity.getName());
        model.setDescription(entity.getDescription());
        model.setType(entity.getType());
        model.setBuiltIn(entity.getBuiltIn());
        if (Objects.nonNull(entity.getLabels())) {
            Map<MetadataLabel, List<String>> label2Values = entity.getLabels().stream().collect(Collectors.groupingBy(
                    MetadataLabelEntity::getLabel,
                    Collectors.mapping(MetadataLabelEntity::getValue, Collectors.toList())));
            if (label2Values.containsKey(MetadataLabel.SUB_TYPE)) {
                model.setSubTypes(label2Values.get(MetadataLabel.SUB_TYPE));
            }
            if (label2Values.containsKey(MetadataLabel.SUPPORTED_DIALECT_TYPE)) {
                model.setSupportedDialectTypes(label2Values.get(MetadataLabel.SUPPORTED_DIALECT_TYPE).stream().map(
                        DialectType::fromValue).collect(Collectors.toList()));
            }
        }
        if (Objects.nonNull(entity.getPropertyMetadatas())) {
            model.setPropertyMetadatas(
                    entity.getPropertyMetadatas().stream().map(PropertyMetadataMapper::fromEntity).collect(
                            Collectors.toList()));
        }
        return model;
    }
}
