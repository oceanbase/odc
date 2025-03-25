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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;

import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.metadb.regulation.ruleset.PropertyMetadataEntity;
import com.oceanbase.odc.service.regulation.ruleset.model.PropertyMetadata;
import com.oceanbase.odc.service.regulation.ruleset.model.PropertyType;

import lombok.NonNull;

/**
 * @Author: Lebie
 * @Date: 2023/5/22 19:44
 * @Description: []
 */
public class PropertyMetadataMapper {
    public static PropertyMetadata fromEntity(@NonNull PropertyMetadataEntity entity) {
        PropertyMetadata model = new PropertyMetadata();
        model.setName(entity.getName());
        model.setDisplayName(entity.getName());
        model.setDescription(entity.getDescription());
        model.setComponentType(entity.getComponentType());
        PropertyType type = entity.getType();
        model.setType(type);
        List<String> candidates = entity.getCandidates();
        if (type == PropertyType.BOOLEAN) {
            if (CollectionUtils.isEmpty(entity.getDefaultValues())) {
                model.setDefaultValue(Collections.emptyList());
            } else {
                Verify.equals(1, entity.getDefaultValues().size(), "defaultValues.size");
                model.setDefaultValue(Boolean.parseBoolean(entity.getDefaultValues().get(0)));
            }
            if (CollectionUtils.isNotEmpty(candidates)) {
                model.setCandidates(candidates.stream().map(Boolean::parseBoolean).collect(Collectors.toList()));
            }
        } else if (type == PropertyType.INTEGER) {
            if (CollectionUtils.isEmpty(entity.getDefaultValues())) {
                model.setDefaultValue(Collections.emptyList());
            } else {
                Verify.equals(1, entity.getDefaultValues().size(), "defaultValues.size");
                model.setDefaultValue(Integer.parseInt(entity.getDefaultValues().get(0)));
            }
            if (CollectionUtils.isNotEmpty(candidates)) {
                model.setCandidates(candidates.stream().map(Integer::parseInt).collect(Collectors.toList()));
            }
        } else if (type == PropertyType.STRING) {
            if (CollectionUtils.isEmpty(entity.getDefaultValues())) {
                model.setDefaultValue(Collections.emptyList());
            } else {
                Verify.equals(1, entity.getDefaultValues().size(), "defaultValues.size");
                model.setDefaultValue(entity.getDefaultValues().get(0));
            }
            if (CollectionUtils.isNotEmpty(candidates)) {
                model.setCandidates(new ArrayList<>(candidates));
            }
        } else if (type == PropertyType.INTEGER_LIST) {
            if (CollectionUtils.isEmpty(entity.getDefaultValues())) {
                model.setDefaultValue(Collections.emptyList());
            } else {
                model.setDefaultValue(entity.getDefaultValues().stream()
                        .map(Integer::parseInt).collect(Collectors.toList()));
            }
            if (CollectionUtils.isNotEmpty(candidates)) {
                model.setCandidates(candidates.stream().map(Integer::parseInt).collect(Collectors.toList()));
            }
        } else if (type == PropertyType.STRING_LIST) {
            if (CollectionUtils.isEmpty(entity.getDefaultValues())) {
                model.setDefaultValue(Collections.emptyList());
            } else {
                model.setDefaultValue(entity.getDefaultValues());
            }
            if (CollectionUtils.isNotEmpty(candidates)) {
                model.setCandidates(new ArrayList<>(candidates));
            }
        }
        return model;
    }
}
