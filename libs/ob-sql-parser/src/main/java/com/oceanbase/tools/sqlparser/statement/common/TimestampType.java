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
import org.apache.commons.lang3.Validate;

import com.oceanbase.tools.sqlparser.statement.BaseStatement;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * {@link TimestampType}
 *
 * @author yh263208
 * @date 2022-12-09 19:19
 * @since ODC_release_4.1.0
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class TimestampType extends BaseStatement implements DataType {

    private final boolean withTimeZone;
    private final boolean withLocalTimeZone;
    private final BigDecimal precision;

    public TimestampType(@NonNull ParserRuleContext context, BigDecimal precision,
            boolean withTimeZone, boolean withLocalTimeZone) {
        super(context);
        Validate.isTrue(!withLocalTimeZone || !withTimeZone);
        this.precision = precision;
        this.withLocalTimeZone = withLocalTimeZone;
        this.withTimeZone = withTimeZone;
    }

    public TimestampType(BigDecimal precision,
            boolean withTimeZone, boolean withLocalTimeZone) {
        Validate.isTrue(!withLocalTimeZone || !withTimeZone);
        this.precision = precision;
        this.withLocalTimeZone = withLocalTimeZone;
        this.withTimeZone = withTimeZone;
    }

    @Override
    public String getName() {
        if (this.withTimeZone) {
            return "TIMESTAMP WITH TIME ZONE";
        } else if (this.withLocalTimeZone) {
            return "TIMESTAMP WITH LOCAL TIME ZONE";
        }
        return "TIMESTAMP";
    }

    @Override
    public List<String> getArguments() {
        if (this.precision == null) {
            return Collections.emptyList();
        }
        return Collections.singletonList(this.precision.toString());
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("TIMESTAMP");
        if (this.precision != null) {
            builder.append("(").append(this.precision.toString()).append(")");
        }
        if (this.withTimeZone) {
            builder.append(" WITH TIME ZONE");
        } else if (this.withLocalTimeZone) {
            builder.append(" WITH LOCAL TIME ZONE");
        }
        return builder.toString();
    }

}
