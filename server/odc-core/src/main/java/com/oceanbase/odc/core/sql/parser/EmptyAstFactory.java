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

package com.oceanbase.odc.core.sql.parser;

import com.oceanbase.tools.sqlparser.SyntaxErrorException;

import lombok.NonNull;

/**
 * {@link EmptyAstFactory}
 *
 * @author yh263208
 * @date 2023-11-17 17:37
 * @since ODC_release_4.2.3
 */
public class EmptyAstFactory implements AbstractSyntaxTreeFactory {

    private final AbstractSyntaxTree ast;

    public EmptyAstFactory(@NonNull AbstractSyntaxTree ast) {
        this.ast = ast;
    }

    @Override
    public AbstractSyntaxTree buildAst(@NonNull String statement) throws SyntaxErrorException {
        return ast;
    }

}
