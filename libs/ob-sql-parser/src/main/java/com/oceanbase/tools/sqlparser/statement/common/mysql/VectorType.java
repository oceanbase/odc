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
package com.oceanbase.tools.sqlparser.statement.common.mysql;

import java.util.Collections;
import java.util.List;

import org.antlr.v4.runtime.ParserRuleContext;

import com.oceanbase.tools.sqlparser.statement.BaseStatement;
import com.oceanbase.tools.sqlparser.statement.common.DataType;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * @Author: Lebie
 * @Date: 2024/10/14 17:14
 * @Description: []
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class VectorType extends BaseStatement implements DataType {
    private final String typeName;
    private final Integer dimension;

    public VectorType(@NonNull ParserRuleContext context,
            @NonNull String typeName, @NonNull Integer dimension) {
        super(context);
        this.typeName = typeName;
        this.dimension = dimension;
    }

    public VectorType(@NonNull String typeName, @NonNull Integer dimension) {
        this.typeName = typeName;
        this.dimension = dimension;
    }

    @Override
    public String getName() {
        return this.typeName;
    }

    @Override
    public List<String> getArguments() {
        return Collections.singletonList(String.valueOf(this.dimension));
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(getName());
        builder.append("(").append(dimension).append(")");
        return builder.toString();
    }
}
