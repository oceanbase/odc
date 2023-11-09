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

import org.antlr.v4.runtime.ParserRuleContext;

import com.oceanbase.tools.sqlparser.statement.Expression;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

/**
 * {@link ParamWithAssign}
 *
 * @author yh263208
 * @date 2022-11-25 19:49
 * @since ODC_release_4.1.0
 * @see FunctionParam
 */
@Getter
@EqualsAndHashCode(callSuper = true)
public class ParamWithAssign extends FunctionParam {

    private final String name;
    private final Expression assignValue;

    public ParamWithAssign(@NonNull ParserRuleContext context,
            @NonNull String name, @NonNull Expression assignValue) {
        super(context);
        this.name = name;
        this.assignValue = assignValue;
    }

    public ParamWithAssign(@NonNull String name, @NonNull Expression assignValue) {
        this.name = name;
        this.assignValue = assignValue;
    }

    @Override
    public String toString() {
        return this.name + "=>" + this.assignValue.toString();
    }

}
