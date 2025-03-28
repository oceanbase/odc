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
package com.oceanbase.tools.sqlparser.statement.createmview;

import org.antlr.v4.runtime.ParserRuleContext;

import com.oceanbase.tools.sqlparser.statement.BaseStatement;

import lombok.*;

/**
 * @description:
 * @author: zijia.cj
 * @date: 2025/3/17 20:59
 * @since: 4.3.4
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class MaterializedViewOptions extends BaseStatement {

    private Boolean enableQueryWrite;
    // This property is set to false if there is no relevant content in the sql
    private Boolean enableQueryComputation;
    private MaterializedViewRefreshOption refreshOption;

    public MaterializedViewOptions(@NonNull ParserRuleContext context,
            @NonNull MaterializedViewRefreshOption refreshOption) {
        super(context);
        this.refreshOption = refreshOption;
    }

    public MaterializedViewOptions(@NonNull MaterializedViewRefreshOption refreshOption) {
        this.refreshOption = refreshOption;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(this.refreshOption.toString());
        if (Boolean.TRUE.equals(this.enableQueryWrite)) {
            builder.append(" ENABLE QUERY REWRITE");
        } else if (Boolean.FALSE.equals(this.enableQueryWrite)) {
            builder.append(" DISABLE QUERY REWRITE");
        }
        if (Boolean.TRUE.equals(this.enableQueryComputation)) {
            builder.append(" ENABLE ON QUERY COMPUTATION");
        } else if (Boolean.FALSE.equals(this.enableQueryComputation)) {
            builder.append(" DISABLE ON QUERY COMPUTATION");
        }
        return builder.toString();
    }

}
