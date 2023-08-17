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
package com.oceanbase.odc.metadb.task;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.jpa.domain.Specification;

import com.oceanbase.odc.common.jpa.SpecificationUtil;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.core.shared.constant.TaskType;

import lombok.NonNull;

/**
 * @author wenniu.ly
 * @date 2022/2/11
 */
public class TaskSpecs {
    // equal
    public static Specification<TaskEntity> idEquals(Long id) {
        return columnEqualSpec("id", id);
    }

    public static Specification<TaskEntity> idIn(Collection<Long> ids) {
        return SpecificationUtil.columnIn("id", ids);
    }

    public static Specification<TaskEntity> organizationIdEquals(Long organizationId) {
        return columnEqualSpec("organizationId", organizationId);
    }

    public static Specification<TaskEntity> taskTypeEquals(TaskType taskType) {
        return columnEqualSpec("taskType", taskType);
    }

    public static Specification<TaskEntity> creatorIdEquals(Long creatorId) {
        return columnEqualSpec("creatorId", creatorId);
    }

    // like
    public static Specification<TaskEntity> databaseNameLike(String databaseNameLike) {
        return (root, query, builder) -> {
            if (StringUtils.isBlank(databaseNameLike)) {
                return builder.conjunction();
            }
            return builder.like(root.get("databaseName"), "%" + StringUtils.escapeLike(databaseNameLike) + "%");
        };
    }

    // in
    public static Specification<TaskEntity> creatorIdIn(List<Long> creatorIds) {
        return columnIn("creatorId", creatorIds);
    }

    public static Specification<TaskEntity> taskTypeIn(List<TaskType> taskTypes) {
        return columnIn("taskType", taskTypes);
    }

    public static Specification<TaskEntity> connectionIdIn(List<Long> connectionIds) {
        return columnIn("connectionId", connectionIds);
    }

    public static Specification<TaskEntity> databaseNameIn(List<String> databaseName) {
        return columnIn("databaseName", databaseName);
    }

    public static Specification<TaskEntity> statusIn(List<TaskStatus> taskStatuses) {
        return columnIn("status", taskStatuses);
    }

    private static Specification<TaskEntity> columnEqualSpec(@NonNull String columnName, Object columnValue) {
        return (root, query, builder) -> {
            if (Objects.isNull(columnValue)) {
                return builder.conjunction();
            }
            return builder.equal(root.get(columnName), columnValue);
        };
    }

    private static Specification<TaskEntity> columnIn(String column, Collection<?> values) {
        return (root, query, builder) -> {
            if (CollectionUtils.isEmpty(values)) {
                return builder.conjunction();
            } else {
                return root.get(column).in(values);
            }
        };
    }

    public static Specification<TaskEntity> createTimeLaterThan(Date columnValue) {
        return (root, query, builder) -> {
            if (Objects.isNull(columnValue)) {
                return builder.conjunction();
            } else {
                return builder.greaterThanOrEqualTo(root.get("createTime"), columnValue);
            }
        };
    }

    public static Specification<TaskEntity> createTimeEarlierThan(Date columnValue) {
        return (root, query, builder) -> {
            if (Objects.isNull(columnValue)) {
                return builder.conjunction();
            } else {
                return builder.lessThanOrEqualTo(root.get("createTime"), columnValue);
            }
        };
    }
}
