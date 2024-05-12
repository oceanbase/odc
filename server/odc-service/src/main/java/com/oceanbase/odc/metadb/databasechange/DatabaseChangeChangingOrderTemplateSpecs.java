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
package com.oceanbase.odc.metadb.databasechange;

import java.util.Collection;

import org.springframework.data.jpa.domain.Specification;

import com.oceanbase.odc.common.jpa.SpecificationUtil;

/**
 * @author: zijia.cj
 * @date: 2024/4/22
 */
public class DatabaseChangeChangingOrderTemplateSpecs {

    public static Specification<DatabaseChangeChangingOrderTemplateEntity> idEquals(Long id) {
        return SpecificationUtil.columnEqual(DatabaseChangeChangingOrderTemplateEntity_.ID, id);
    }

    public static Specification<DatabaseChangeChangingOrderTemplateEntity> nameLikes(String name) {
        return SpecificationUtil.columnLike(DatabaseChangeChangingOrderTemplateEntity_.NAME, name);
    }

    public static Specification<DatabaseChangeChangingOrderTemplateEntity> creatorIdIn(Collection<Long> userIds) {
        return SpecificationUtil.columnIn(DatabaseChangeChangingOrderTemplateEntity_.CREATOR_ID, userIds);
    }

    public static Specification<DatabaseChangeChangingOrderTemplateEntity> projectIdEquals(Long projectId) {
        return SpecificationUtil.columnEqual(DatabaseChangeChangingOrderTemplateEntity_.PROJECT_ID, projectId);
    }
}
