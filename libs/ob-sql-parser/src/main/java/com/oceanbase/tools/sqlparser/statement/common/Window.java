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

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

/**
 * {@link Window}
 *
 * @author yh263208
 * @date 2022-12-12 15:39
 * @since ODC_release_4.1.0
 */
@Getter
@EqualsAndHashCode(callSuper = false)
public class Window extends BaseStatement {

    private final String name;
    private final WindowSpec spec;

    public Window(@NonNull ParserRuleContext context, @NonNull String name, @NonNull WindowSpec spec) {
        super(context);
        this.name = name;
        this.spec = spec;
    }

    public Window(@NonNull String name, @NonNull WindowSpec spec) {
        this.name = name;
        this.spec = spec;
    }

    @Override
    public String toString() {
        return this.name + " AS (" + this.spec.toString() + ")";
    }

}
