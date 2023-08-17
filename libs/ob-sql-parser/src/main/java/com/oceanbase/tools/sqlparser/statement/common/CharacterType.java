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

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import org.antlr.v4.runtime.ParserRuleContext;

import com.oceanbase.tools.sqlparser.statement.BaseStatement;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * {@link CharacterType}
 *
 * @author yh263208
 * @date 2022-12-09 19:19
 * @since ODC_release_4.1.0
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class CharacterType extends BaseStatement implements DataType {

    private String charset;
    private String collation;
    private boolean binary;
    private final BigDecimal length;
    private String lengthOption;
    private final String typeName;

    public CharacterType(@NonNull ParserRuleContext context, @NonNull String typeName, BigDecimal length) {
        super(context);
        this.length = length;
        this.typeName = typeName;
    }

    public CharacterType(@NonNull ParserRuleContext context, @NonNull CharacterType other) {
        super(context);
        this.charset = other.charset;
        this.collation = other.collation;
        this.binary = other.binary;
        this.length = other.length;
        this.lengthOption = other.lengthOption;
        this.typeName = other.typeName;
    }

    public CharacterType(@NonNull String typeName, BigDecimal length) {
        this.length = length;
        this.typeName = typeName;
    }

    @Override
    public String getName() {
        return this.typeName;
    }

    @Override
    public List<String> getArguments() {
        if (this.length == null) {
            return Collections.emptyList();
        }
        StringBuilder builder = new StringBuilder(this.length.toString());
        if (this.lengthOption != null) {
            builder.append(" ").append(this.lengthOption);
        }
        return Collections.singletonList(builder.toString());
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(getName().toUpperCase());
        if (this.length != null) {
            builder.append("(").append(this.length.toString());
            if (this.lengthOption != null) {
                builder.append(" ").append(this.lengthOption);
            }
            builder.append(")");
        }
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
