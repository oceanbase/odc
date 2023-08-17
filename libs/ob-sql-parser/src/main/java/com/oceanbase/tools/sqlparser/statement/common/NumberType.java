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
import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.ParserRuleContext;

import com.oceanbase.tools.sqlparser.statement.BaseStatement;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * {@link NumberType}
 *
 * @author yh263208
 * @date 2022-12-09 19:19
 * @since ODC_release_4.1.0
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class NumberType extends BaseStatement implements DataType {

    private final String typeName;
    private final BigDecimal precision;
    private final BigDecimal scale;
    private boolean starPresicion;
    private Boolean signed;
    private boolean zeroFill;

    public NumberType(@NonNull ParserRuleContext context, @NonNull String typeName,
            BigDecimal precision, BigDecimal scale) {
        super(context);
        this.typeName = typeName;
        this.precision = precision;
        this.scale = scale;
    }

    public NumberType(@NonNull String typeName, BigDecimal precision, BigDecimal scale) {
        this.typeName = typeName;
        this.precision = precision;
        this.scale = scale;
    }

    @Override
    public String getName() {
        return this.typeName;
    }

    @Override
    public List<String> getArguments() {
        List<String> args = new ArrayList<>();
        if (this.precision != null) {
            args.add(this.precision.toString());
        } else if (this.starPresicion) {
            args.add("*");
        }
        if (this.scale != null) {
            args.add(this.scale.toString());
        }
        return args;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(this.typeName.toUpperCase());
        if (!getArguments().isEmpty()) {
            builder.append("(").append(String.join(",", getArguments())).append(")");
        }
        if (this.signed != null) {
            builder.append(" ").append(this.signed ? "SIGNED" : "UNSIGNED");
        }
        if (this.zeroFill) {
            builder.append(" ").append("ZEROFILL");
        }
        return builder.toString();
    }

}
