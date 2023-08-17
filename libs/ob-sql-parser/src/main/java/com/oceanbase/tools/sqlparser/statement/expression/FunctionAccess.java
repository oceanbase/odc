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
import java.util.stream.Collectors;

import org.antlr.v4.runtime.ParserRuleContext;
import org.apache.commons.collections4.CollectionUtils;

import com.oceanbase.tools.sqlparser.statement.BaseStatement;
import com.oceanbase.tools.sqlparser.statement.Expression;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

/**
 * {@link FunctionAccess}
 *
 * @author yh263208
 * @date 2022-11-25 17:38
 * @since ODC_release_4.1.0
 * @see BaseStatement
 */
@Getter
@EqualsAndHashCode(callSuper = true)
public class FunctionAccess extends FunctionCall {

    private final List<Expression> functionAccess;

    public FunctionAccess(@NonNull ParserRuleContext context,
            @NonNull String functionName,
            @NonNull List<FunctionParam> functionParams,
            @NonNull List<Expression> functionAccess) {
        super(context, functionName, functionParams);
        this.functionAccess = functionAccess;
    }

    public FunctionAccess(@NonNull String functionName,
            @NonNull List<FunctionParam> functionParams,
            @NonNull List<Expression> functionAccess) {
        super(functionName, functionParams);
        this.functionAccess = functionAccess;
    }

    @Override
    public String toString() {
        if (CollectionUtils.isEmpty(this.functionAccess)) {
            return super.toString();
        }
        return super.toString()
                + functionAccess.stream().map(e -> "(" + e.toString() + ")").collect(Collectors.joining());
    }

}
