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
package com.oceanbase.odc.metadb.datasecurity;

import java.util.Collection;

import org.springframework.data.jpa.domain.Specification;

import com.oceanbase.odc.common.jpa.SpecificationUtil;

/**
 * @author gaoda.xy
 * @date 2023/5/22 11:28
 */
public class SensitiveColumnSpecs {

    private static final String ENABLED = "enabled";
    private static final String DATABASE_ID = "databaseId";
    private static final String TABLE_NAME = "tableName";
    private static final String COLUMN_NAME = "columnName";
    private static final String MASKING_ALGORITHM_ID = "maskingAlgorithmId";
    private static final String ORGANIZATION_ID = "organizationId";

    public static Specification<SensitiveColumnEntity> enabledEqual(Boolean enabled) {
        return SpecificationUtil.columnEqual(ENABLED, enabled);
    }

    public static Specification<SensitiveColumnEntity> databaseIdIn(Collection<Long> databaseIds) {
        return SpecificationUtil.columnIn(DATABASE_ID, databaseIds);
    }

    public static Specification<SensitiveColumnEntity> tableNameLike(String tableName) {
        return SpecificationUtil.columnLike(TABLE_NAME, tableName);
    }

    public static Specification<SensitiveColumnEntity> tableNameIn(Collection<String> tableNames) {
        return SpecificationUtil.columnIn(TABLE_NAME, tableNames);
    }

    public static Specification<SensitiveColumnEntity> columnNameLike(String columnName) {
        return SpecificationUtil.columnLike(COLUMN_NAME, columnName);
    }

    public static Specification<SensitiveColumnEntity> columnNameIn(Collection<String> columnNames) {
        return SpecificationUtil.columnIn(COLUMN_NAME, columnNames);
    }

    public static Specification<SensitiveColumnEntity> maskingAlgorithmIdIn(Collection<Long> maskingAlgorithmIds) {
        return SpecificationUtil.columnIn(MASKING_ALGORITHM_ID, maskingAlgorithmIds);
    }

    public static Specification<SensitiveColumnEntity> organizationIdEqual(Long organizationId) {
        return SpecificationUtil.columnEqual(ORGANIZATION_ID, organizationId);
    }

}
