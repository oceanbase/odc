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
package com.oceanbase.tools.sqlparser.statement;

/**
 * {@link JoinType}
 *
 * @author yh263208
 * @date 2022-11-25 11:00
 * @since ODC_release_4.1.0
 */
public enum JoinType {
    // full outer join
    FULL_OUTER_JOIN,
    // full join
    FULL_JOIN,
    // left outer join
    LEFT_OUTER_JOIN,
    // left join
    LEFT_JOIN,
    // right outer join
    RIGHT_OUTER_JOIN,
    // right join
    RIGHT_JOIN,
    // inner join
    INNER_JOIN,
    // join
    JOIN,
    // straight_join
    STRAIGHT_JOIN,
    // cross join
    CROSS_JOIN,
    // natural inner join
    NATURAL_INNER_JOIN,
    // natural full outer join
    NATURAL_FULL_OUTER_JOIN,
    // natural full join
    NATURAL_FULL_JOIN,
    // natual left outer join
    NATURAL_LEFT_OUTER_JOIN,
    // natural left join
    NATURAL_LEFT_JOIN,
    // natual right outer join
    NATURAL_RIGHT_OUTER_JOIN,
    // natural right join
    NATURAL_RIGHT_JOIN,
    // natural join
    NATURAL_JOIN
}
