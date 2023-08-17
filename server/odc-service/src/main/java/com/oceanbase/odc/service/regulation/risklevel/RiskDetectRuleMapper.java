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
package com.oceanbase.odc.service.regulation.risklevel;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.metadb.regulation.risklevel.RiskDetectRuleEntity;
import com.oceanbase.odc.service.regulation.risklevel.model.BaseTreeNode;
import com.oceanbase.odc.service.regulation.risklevel.model.RiskDetectRule;

@Mapper
public interface RiskDetectRuleMapper {

    RiskDetectRuleMapper INSTANCE = Mappers.getMapper(RiskDetectRuleMapper.class);

    @Mapping(target = "rootNode", expression = "java(fromJson(entity.getValueJson()))")
    RiskDetectRule entityToModel(RiskDetectRuleEntity entity);

    @Mapping(target = "valueJson", expression = "java(toJson(model.getRootNode()))")
    RiskDetectRuleEntity modelToEntity(RiskDetectRule model);

    default BaseTreeNode fromJson(String json) {
        return JsonUtils.fromJson(json, BaseTreeNode.class);
    }

    default String toJson(BaseTreeNode root) {
        return JsonUtils.toJson(root);
    }
}
