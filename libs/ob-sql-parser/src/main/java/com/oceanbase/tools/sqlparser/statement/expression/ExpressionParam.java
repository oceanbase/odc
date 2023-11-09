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

import com.oceanbase.tools.sqlparser.statement.Expression;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

/**
 * {@link ExpressionParam}
 *
 * @author yh263208
 * @date 2022-11-25 20:02
 * @since ODC_release_4.1.0
 * @see FunctionParam
 */
@Getter
@EqualsAndHashCode(callSuper = true)
public class ExpressionParam extends FunctionParam {

    private final Expression target;

    public ExpressionParam(@NonNull Expression target) {
        this(target, null);
    }

    public ExpressionParam(@NonNull Expression target, String alias) {
        this.target = target;
        if (alias != null) {
            addOption(new ConstExpression(alias));
        }
    }

    @Override
    public String getText() {
        return this.target.getText();
    }

    @Override
    public int getStart() {
        return this.target.getStart();
    }

    @Override
    public int getStop() {
        return this.target.getStop();
    }

    @Override
    public int getLine() {
        return this.target.getLine();
    }

    @Override
    public int getCharPositionInLine() {
        return this.target.getCharPositionInLine();
    }

    @Override
    public String toString() {
        return this.target.toString();
    }

}
