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
package com.oceanbase.tools.dbbrowser.model.datatype.parser;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link DefaultDataTypeTokenVisitor}
 *
 * @author yh263208
 * @date 2022-06-27 16:22
 * @since ODC_release_3.4.0
 * @see DataTypeTokenVisitor
 */
@Slf4j
public class DefaultDataTypeTokenVisitor implements DataTypeTokenVisitor {

    @Override
    public void visitName(@NonNull DataTypeToken token) {
        if (log.isDebugEnabled()) {
            log.debug("Visit name token, token={}", token);
        }
    }

    @Override
    public void visitBrackets(@NonNull DataTypeToken token) {
        if (log.isDebugEnabled()) {
            log.debug("Visit brackets token, token={}", token);
        }
    }

    @Override
    public void visitNumber(@NonNull DataTypeToken token) {
        if (log.isDebugEnabled()) {
            log.debug("Visit number token, token={}", token);
        }
    }

    @Override
    public void visitUnknown(@NonNull DataTypeToken token) {
        if (log.isDebugEnabled()) {
            log.debug("Visit unknown token, token={}", token);
        }
    }
}
