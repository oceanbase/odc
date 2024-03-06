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
package com.oceanbase.odc.core.sql.parser;

import java.util.Collections;
import java.util.List;

import org.antlr.v4.runtime.ParserRuleContext;

import com.oceanbase.tools.sqlparser.statement.BaseStatement;
import com.oceanbase.tools.sqlparser.statement.common.RelationFactor;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class CreateStatement extends BaseStatement {

    private final List<RelationFactor> relationFactors;

    public CreateStatement(@NonNull ParserRuleContext ruleNode,
            @NonNull List<RelationFactor> relationFactors) {
        super(ruleNode);
        this.relationFactors = relationFactors;
    }

    public CreateStatement(@NonNull ParserRuleContext ruleNode, @NonNull RelationFactor relationFactor) {
        super(ruleNode);
        this.relationFactors = Collections.singletonList(relationFactor);
    }

    public CreateStatement(@NonNull RelationFactor relationFactor) {
        this.relationFactors = Collections.singletonList(relationFactor);
    }

    public CreateStatement(@NonNull List<RelationFactor> relationFactors) {
        this.relationFactors = relationFactors;
    }

}
