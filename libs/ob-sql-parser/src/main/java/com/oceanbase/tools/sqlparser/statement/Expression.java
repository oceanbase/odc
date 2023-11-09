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

import lombok.NonNull;

/**
 * {@link Expression}
 *
 * @author yh263208
 * @date 2022-11-24 20:56
 * @since ODC_release_4.1.0
 * @see Statement
 */
public interface Expression extends Statement {

    Expression getParentReference();

    Expression getReference();

    ReferenceOperator getReferenceOperator();

    ReferenceOperator getParentReferenceOperator();

    Expression reference(@NonNull Expression nextExpr, @NonNull Expression.ReferenceOperator operator);

    Expression parentReference(@NonNull Expression parentExpr, @NonNull Expression.ReferenceOperator operator);

    enum ReferenceOperator {
        // .
        DOT {
            @Override
            public String wrap(@NonNull String expr) {
                return "." + expr;
            }
        },
        // ()
        PAREN {
            @Override
            public String wrap(@NonNull String expr) {
                return "(" + expr + ")";
            }
        },
        // []
        BRACKET {
            @Override
            public String wrap(@NonNull String expr) {
                return "[" + expr + "]";
            }
        },
        // {}
        BRACE {
            @Override
            public String wrap(@NonNull String expr) {
                return "{" + expr + "}";
            }
        };

        public abstract String wrap(@NonNull String expr);
    }

}
