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
package com.oceanbase.odc.common.jpa;

import java.util.Collection;
import java.util.Date;
import java.util.Objects;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.CollectionUtils;

import com.oceanbase.odc.common.util.StringUtils;

import lombok.NonNull;

/**
 * Util for {@link Specification}
 *
 * @author yh263208
 * @date 2022-03-03 20:57
 * @since ODC_release_3.3.0
 */
public class SpecificationUtil {

    public static <T> Specification<T> columnIn(String column, Collection<?> values) {
        return (root, query, builder) -> {
            if (CollectionUtils.isEmpty(values)) {
                return builder.conjunction();
            } else {
                return root.get(column).in(values);
            }
        };
    }

    public static <T> Specification<T> columnEqual(@NonNull String columnName, Object columnValue) {
        return (root, query, builder) -> {
            if (Objects.isNull(columnValue)) {
                return builder.conjunction();
            }
            return builder.equal(root.get(columnName), columnValue);
        };
    }


    public static <T> Specification<T> columnBefore(@NonNull String columnName, Date time) {
        return (root, query, builder) -> {
            if (Objects.isNull(time)) {
                return builder.conjunction();
            }
            return builder.lessThan(root.get(columnName), time);
        };
    }

    public static <T> Specification<T> columnLate(@NonNull String columnName, Date time) {
        return (root, query, builder) -> {
            if (Objects.isNull(time)) {
                return builder.conjunction();
            }
            return builder.greaterThan(root.get(columnName), time);
        };
    }

    public static <T, K extends Comparable<K>> Specification<T> columnLessThanOrEqualTo(@NonNull String columnName,
            K number) {
        return (root, query, builder) -> {
            if (Objects.isNull(number)) {
                return builder.conjunction();
            }
            return builder.lessThanOrEqualTo(root.get(columnName), number);
        };
    }

    public static <T> Specification<T> columnLike(@NonNull String columnName, String likeString) {
        return (root, query, builder) -> {
            if (StringUtils.isBlank(likeString)) {
                return builder.conjunction();
            }
            return builder.like(root.get(columnName), "%" + StringUtils.escapeLike(likeString) + "%");
        };
    }

    public static <T> Specification<T> columnIsNull(@NonNull String columnName) {
        return (root, query, builder) -> builder.isNull(root.get(columnName));

    }

    public static <T> Specification<T> columnIsNotNull(@NonNull String columnName) {
        return (root, query, builder) -> builder.isNotNull(root.get(columnName));

    }

}
