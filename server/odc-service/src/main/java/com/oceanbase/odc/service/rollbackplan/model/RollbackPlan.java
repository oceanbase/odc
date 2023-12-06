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
package com.oceanbase.odc.service.rollbackplan.model;

import java.util.List;
import java.util.stream.Collectors;

import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.sql.split.OffsetString;
import com.oceanbase.odc.core.sql.split.SqlCommentProcessor;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * {@link RollbackPlan}
 *
 * @author jingtian
 * @date 2023/4/25
 * @since ODC_release_4.2.0
 */
@Getter
@Setter
@EqualsAndHashCode
public class RollbackPlan {
    private final String sql;
    private final DialectType dialectType;
    private List<String> querySqls;
    private List<String> rollbackSqls;
    private String errorMessage;
    private int changeLineCount;

    public RollbackPlan(@NonNull String sql, @NonNull DialectType dialectType) {
        this.sql = sql;
        this.dialectType = dialectType;
    }

    public RollbackPlan(String sql, DialectType dialectType, List<String> querySqls, List<String> rollbackSqls,
            String errorMessage,
            int changeLineCount) {
        this.sql = sql;
        this.dialectType = dialectType;
        this.querySqls = querySqls;
        this.rollbackSqls = rollbackSqls;
        this.errorMessage = errorMessage;
        this.changeLineCount = changeLineCount;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("/* \n")
                .append("[SQL]: \n")
                .append(removeCommentsForSingleSql(sql) + "\n");
        builder.append("\n");
        if (this.querySqls != null) {
            builder.append("[QUERY SQL]: \n");
            for (String querySql : this.querySqls) {
                builder.append(querySql + "\n");
            }
            builder.append("\n");
        }
        if (this.rollbackSqls != null) {
            builder.append("*/ \n");
            for (String rollbackSql : this.rollbackSqls) {
                builder.append(rollbackSql + "\n");
            }
        } else if (this.errorMessage != null) {
            builder.append("[ERROR MESSAGE]: \n")
                    .append(this.errorMessage + "\n")
                    .append("*/ \n");
        }
        builder.append("\n");
        return builder.toString();
    }

    private String removeCommentsForSingleSql(String singleSql) {
        return SqlCommentProcessor.removeSqlComments(singleSql, ";", dialectType, false).stream()
                .map(OffsetString::getStr).collect(Collectors.toList())
                .get(0);
    }
}
