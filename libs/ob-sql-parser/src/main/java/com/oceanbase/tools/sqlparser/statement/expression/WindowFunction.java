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

import com.oceanbase.tools.sqlparser.statement.common.WindowSpec;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * {@link WindowFunction}
 *
 * @author yh263208
 * @date 2022-12-11 18:06
 * @since ODC_release_4.1.0
 */
@Setter
@Getter
@EqualsAndHashCode(callSuper = true)
public class WindowFunction extends FunctionCall {

    private WindowSpec window;

    public WindowFunction(@NonNull ParserRuleContext context,
            @NonNull String functionName,
            @NonNull List<FunctionParam> functionParams) {
        super(context, functionName, functionParams);
    }

    public WindowFunction(@NonNull String functionName,
            @NonNull List<FunctionParam> functionParams) {
        super(functionName, functionParams);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(super.toString());
        if (this.window != null) {
            builder.append(" OVER ").append(this.window.toString());
        }
        return builder.toString();
    }

}
