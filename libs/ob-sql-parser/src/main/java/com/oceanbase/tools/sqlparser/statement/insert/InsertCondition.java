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

package com.oceanbase.tools.sqlparser.statement.insert;

import java.util.List;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.ParserRuleContext;

import com.oceanbase.tools.sqlparser.statement.BaseStatement;
import com.oceanbase.tools.sqlparser.statement.Expression;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

/**
 * {@link InsertCondition}
 *
 * @author yh263208
 * @date 2023-11-08 18:01
 * @since ODC_release_4.2.3
 */
@Getter
@EqualsAndHashCode(callSuper = false)
public class InsertCondition extends BaseStatement {

    private final Expression when;
    private final List<InsertTable> then;

    public InsertCondition(@NonNull ParserRuleContext context,
            @NonNull Expression when, @NonNull List<InsertTable> then) {
        super(context);
        this.when = when;
        this.then = then;
    }

    public InsertCondition(@NonNull Expression when, @NonNull List<InsertTable> then) {
        this.when = when;
        this.then = then;
    }

    @Override
    public String toString() {
        return "WHEH " + this.when + " THEN" + "\n\t" + this.then.stream()
                .map(InsertTable::toString).collect(Collectors.joining("\n\t"));
    }

}
