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
package com.oceanbase.odc.service.notification.model;

import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.oceanbase.odc.common.jpa.SpecificationUtil;
import com.oceanbase.odc.metadb.notification.ChannelEntity;

/**
 * @author liuyizhuo.lyz
 * @date 2024/1/10
 */
public class ChannelSpecs {

    public static Specification<ChannelEntity> projectIdEquals(Long projectId) {
        return SpecificationUtil.columnEqual("projectId", projectId);
    }

    public static Specification<ChannelEntity> nameLike(String fuzzyName) {
        return SpecificationUtil.columnLike("name", fuzzyName);

    }

    public static Specification<ChannelEntity> typeIn(List<ChannelType> types) {
        return SpecificationUtil.columnIn("type", types);
    }

}
