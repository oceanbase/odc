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
package com.oceanbase.tools.sqlparser.statement.select.oracle;

import org.antlr.v4.runtime.ParserRuleContext;

import com.oceanbase.tools.sqlparser.statement.BaseStatement;
import com.oceanbase.tools.sqlparser.statement.Expression;
import com.oceanbase.tools.sqlparser.statement.select.GroupBy;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

/**
 * {@link GeneralGroupBy}
 *
 * @author yh263208
 * @date 2022-11-25 15:11
 * @since ODC_release_4.1.0
 * @see GroupBy
 */
@Getter
@EqualsAndHashCode(callSuper = false)
public class GeneralGroupBy extends BaseStatement implements GroupBy {

    private final Expression target;

    public GeneralGroupBy(@NonNull ParserRuleContext context, @NonNull Expression target) {
        super(context);
        this.target = target;
    }

    public GeneralGroupBy(@NonNull Expression target) {
        this.target = target;
    }

    @Override
    public String toString() {
        return this.target.toString();
    }

}
