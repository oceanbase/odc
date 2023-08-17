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
package com.oceanbase.tools.sqlparser.statement.common;

import org.antlr.v4.runtime.ParserRuleContext;

import com.oceanbase.tools.sqlparser.statement.BaseStatement;
import com.oceanbase.tools.sqlparser.statement.Expression;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * {@link WindowOffset}
 *
 * @author yh263208
 * @date 2022-12-11 17:06
 * @since ODC_release_4.1.0
 * @see BaseStatement
 */
@Setter
@Getter
@EqualsAndHashCode(callSuper = false)
public class WindowOffset extends BaseStatement {

    private Expression interval;
    private final WindowOffsetType type;

    public WindowOffset(@NonNull ParserRuleContext context,
            @NonNull WindowOffsetType type) {
        super(context);
        this.type = type;
    }

    public WindowOffset(@NonNull WindowOffsetType type) {
        this.type = type;
    }

    @Override
    public String toString() {
        if (this.type == WindowOffsetType.CURRENT_ROW) {
            return "CURRENT ROW";
        }
        if (this.interval == null) {
            return "";
        }
        return this.interval.toString() + " " + this.type.name();
    }

}
