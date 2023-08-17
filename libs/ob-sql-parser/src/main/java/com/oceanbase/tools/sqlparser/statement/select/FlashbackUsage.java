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
package com.oceanbase.tools.sqlparser.statement.select;

import org.antlr.v4.runtime.ParserRuleContext;

import com.oceanbase.tools.sqlparser.statement.BaseStatement;
import com.oceanbase.tools.sqlparser.statement.Expression;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

/**
 * {@link FlashbackUsage}
 *
 * @author yh263208
 * @date 2022-12-05 20:42
 * @since ODC_release_4.1.0
 * @see BaseStatement
 */
@Getter
@EqualsAndHashCode(callSuper = false)
public class FlashbackUsage extends BaseStatement {

    private final FlashBackType type;
    private final Expression expression;

    public FlashbackUsage(@NonNull ParserRuleContext context, @NonNull FlashBackType type,
            @NonNull Expression expression) {
        super(context);
        this.type = type;
        this.expression = expression;
    }

    public FlashbackUsage(@NonNull FlashBackType type, @NonNull Expression expression) {
        this.type = type;
        this.expression = expression;
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder("AS OF ");
        if (type == FlashBackType.AS_OF_TIMESTAMP) {
            buffer.append("TIMESTAMP ");
        } else if (type == FlashBackType.AS_OF_SCN) {
            buffer.append("SCN ");
        } else {
            buffer.append("SNAPSHOT ");
        }
        return buffer.append(expression.toString()).toString();
    }

}
