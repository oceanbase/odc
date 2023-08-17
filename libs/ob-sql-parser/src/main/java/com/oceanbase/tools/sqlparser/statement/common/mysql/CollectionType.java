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

import java.util.List;

import org.antlr.v4.runtime.ParserRuleContext;

import com.oceanbase.tools.sqlparser.statement.BaseStatement;
import com.oceanbase.tools.sqlparser.statement.common.DataType;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * {@link CollectionType}
 *
 * @author yh263208
 * @date 2022-12-09 19:19
 * @since ODC_release_4.1.0
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class CollectionType extends BaseStatement implements DataType {

    private String charset;
    private String collation;
    private boolean binary;
    private final String typeName;
    private final List<String> stringList;

    public CollectionType(@NonNull ParserRuleContext context,
            @NonNull String typeName, @NonNull List<String> stringList) {
        super(context);
        this.typeName = typeName;
        this.stringList = stringList;
    }

    public CollectionType(@NonNull String typeName, @NonNull List<String> stringList) {
        this.stringList = stringList;
        this.typeName = typeName;
    }

    @Override
    public String getName() {
        return this.typeName.toUpperCase();
    }

    @Override
    public List<String> getArguments() {
        return this.stringList;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(getName());
        builder.append("(").append(String.join(",", this.stringList)).append(")");
        if (this.binary) {
            builder.append(" ").append("BINARY");
        }
        if (this.charset != null) {
            builder.append(" ").append("CHARSET")
                    .append(" ").append(this.charset);
        }
        if (this.collation != null) {
            builder.append(" ").append("COLLATE")
                    .append(" ").append(this.collation);
        }
        return builder.toString();
    }

}
