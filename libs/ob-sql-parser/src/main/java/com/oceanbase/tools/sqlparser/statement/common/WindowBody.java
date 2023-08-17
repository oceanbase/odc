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
 * {@link WindowBody}
 *
 * @author yh263208
 * @date 2022-12-11 16:59
 * @since ODC_release_4.1.0
 * @see BaseStatement
 */
@Getter
@EqualsAndHashCode(callSuper = false)
public class WindowBody extends BaseStatement {

    private final WindowType type;
    private final WindowOffset begin;
    private final WindowOffset end;
    private final WindowOffset offset;

    public WindowBody(@NonNull ParserRuleContext context,
            @NonNull WindowType type, @NonNull WindowOffset begin,
            @NonNull WindowOffset end) {
        super(context);
        this.type = type;
        this.begin = begin;
        this.end = end;
        this.offset = null;
    }

    public WindowBody(@NonNull ParserRuleContext context,
            @NonNull WindowType type, @NonNull WindowOffset offset) {
        super(context);
        this.type = type;
        this.offset = offset;
        this.begin = null;
        this.end = null;
    }

    public WindowBody(@NonNull WindowType type,
            @NonNull WindowOffset begin, @NonNull WindowOffset end) {
        this.type = type;
        this.begin = begin;
        this.end = end;
        this.offset = null;
    }

    public WindowBody(@NonNull WindowType type,
            @NonNull WindowOffset offset) {
        this.type = type;
        this.offset = offset;
        this.begin = null;
        this.end = null;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(this.type.name());
        if (this.begin != null && this.end != null) {
            return builder.append(" BETWEEN ")
                    .append(this.begin.toString())
                    .append(" AND ")
                    .append(this.end.toString()).toString();
        }
        if (this.offset != null) {
            return builder.append(" ").append(this.offset.toString()).toString();
        }
        return builder.toString();
    }

}
