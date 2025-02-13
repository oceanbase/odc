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
package com.oceanbase.tools.sqlparser.statement.expression;

import java.util.List;

import org.antlr.v4.runtime.ParserRuleContext;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * {@link FullTextSearch}
 *
 * @author yh263208
 * @date 2022-12-08 17:53
 * @since ODC_release_4.1.0
 */
@Getter
@EqualsAndHashCode(callSuper = true)
public class FullTextSearch extends FunctionCall {

    private final String against;
    @Setter
    private TextSearchMode searchMode;
    @Setter
    private boolean withQueryExpansion;

    public FullTextSearch(@NonNull ParserRuleContext context,
            @NonNull List<FunctionParam> params, @NonNull String against) {
        super(context, "MATCH", params);
        this.against = against;
    }

    public FullTextSearch(@NonNull List<FunctionParam> params,
            @NonNull String against) {
        super("MATCH", params);
        this.against = against;
    }

    @Override
    public String doToString() {
        StringBuilder builder = new StringBuilder();
        builder.append(super.doToString());
        builder.append(" AGAINST(").append(this.against);
        if (this.searchMode != null) {
            builder.append(" ").append(this.searchMode.getValue());
        }
        if (this.withQueryExpansion) {
            builder.append(" WITH QUERY EXPANSION");
        }
        return builder.append(")").toString();
    }

}
