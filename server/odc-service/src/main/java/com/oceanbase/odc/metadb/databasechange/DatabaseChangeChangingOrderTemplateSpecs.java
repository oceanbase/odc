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
    private static final String DATABASE_CHANGE_CHANGING_ORDER_TEMPLATE_ID_NAME = "id";
    private static final String DATABASE_CHANGE_CHANGING_ORDER_TEMPLATE_NAME_NAME = "name";
    private static final String DATABASE_CHANGE_CHANGING_ORDER_TEMPLATE_CREATOR_ID_NAME = "creatorId";
    private static final String DATABASE_CHANGE_CHANGING_ORDER_TEMPLATE_PROJECT_ID_NAME = "projectId";

    public static Specification<DatabaseChangeChangingOrderTemplateEntity> idEquals(Long id) {
        return SpecificationUtil.columnEqual(DATABASE_CHANGE_CHANGING_ORDER_TEMPLATE_ID_NAME, id);
    }

    public static Specification<DatabaseChangeChangingOrderTemplateEntity> nameLikes(String name) {
        return SpecificationUtil.columnLike(DATABASE_CHANGE_CHANGING_ORDER_TEMPLATE_NAME_NAME, name);
    }

    public static Specification<DatabaseChangeChangingOrderTemplateEntity> creatorIdIn(Collection<Long> userIds) {
        return SpecificationUtil.columnIn(DATABASE_CHANGE_CHANGING_ORDER_TEMPLATE_CREATOR_ID_NAME, userIds);
    }

    public static Specification<DatabaseChangeChangingOrderTemplateEntity> projectIdEquals(Long projectId) {
        return SpecificationUtil.columnEqual(DATABASE_CHANGE_CHANGING_ORDER_TEMPLATE_PROJECT_ID_NAME, projectId);
    }
}
