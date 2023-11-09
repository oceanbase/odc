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

import java.util.Collections;
import java.util.List;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.commons.collections4.CollectionUtils;

import com.oceanbase.tools.sqlparser.statement.BaseStatement;

import lombok.EqualsAndHashCode;
import lombok.NonNull;

/**
 * {@link GeneralDataType}
 *
 * @author yh263208
 * @date 2022-12-09 19:31
 * @since ODC_release_4.1.0
 * @see DataType
 */
@EqualsAndHashCode(callSuper = false)
public class GeneralDataType extends BaseStatement implements DataType {

    private final String name;
    private final List<String> args;

    public GeneralDataType(@NonNull ParserRuleContext context,
            @NonNull String name, List<String> args) {
        super(context);
        this.name = name;
        this.args = args == null ? Collections.emptyList() : args;
    }

    public GeneralDataType(@NonNull TerminalNode terminalNode,
            @NonNull String name, List<String> args) {
        super(terminalNode);
        this.name = name;
        this.args = args == null ? Collections.emptyList() : args;
    }

    public GeneralDataType(@NonNull String name, List<String> args) {
        this.name = name;
        this.args = args == null ? Collections.emptyList() : args;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public List<String> getArguments() {
        return this.args;
    }

    @Override
    public String toString() {
        if (CollectionUtils.isEmpty(this.args)) {
            return this.name;
        }
        return this.name + "(" + String.join(",", this.args) + ")";
    }

}
