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
package com.oceanbase.tools.sqlparser.statement.truncate;

import org.antlr.v4.runtime.ParserRuleContext;

import com.oceanbase.tools.sqlparser.statement.BaseStatement;
import com.oceanbase.tools.sqlparser.statement.common.RelationFactor;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * {@link TruncateTable}
 *
 * @author yh263208
 * @date 2024-03-05 22:22
 * @since ODC_release_4.2.4
 * @see BaseStatement
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class TruncateTable extends BaseStatement {

    private final RelationFactor relationFactor;

    public TruncateTable(@NonNull RelationFactor relationFactor) {
        this.relationFactor = relationFactor;
    }

    public TruncateTable(@NonNull ParserRuleContext context, @NonNull RelationFactor relationFactor) {
        super(context);
        this.relationFactor = relationFactor;
    }

    @Override
    public String toString() {
        return "TRUNCATE TABLE " + this.relationFactor.toString();
    }

}
