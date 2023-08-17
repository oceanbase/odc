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
package com.oceanbase.tools.sqlparser.statement.common.oracle;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.ParserRuleContext;

import com.oceanbase.tools.sqlparser.statement.BaseStatement;
import com.oceanbase.tools.sqlparser.statement.common.DataType;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * {@link IntervalType}
 *
 * @author yh263208
 * @date 2022-12-09 19:19
 * @since ODC_release_4.1.0
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class IntervalType extends BaseStatement implements DataType {

    private final boolean yearToMonth;
    private final boolean dayToSecond;
    private final BigDecimal yearPrecision;
    private final BigDecimal dayPrecision;
    private final BigDecimal secondPrecision;

    public IntervalType(@NonNull ParserRuleContext context, BigDecimal yearPrecision) {
        super(context);
        this.yearToMonth = true;
        this.dayToSecond = false;
        this.yearPrecision = yearPrecision;
        this.dayPrecision = null;
        this.secondPrecision = null;
    }

    public IntervalType(@NonNull ParserRuleContext context, BigDecimal dayPrecision, BigDecimal secondPrecision) {
        super(context);
        this.yearToMonth = false;
        this.dayToSecond = true;
        this.yearPrecision = null;
        this.dayPrecision = dayPrecision;
        this.secondPrecision = secondPrecision;
    }

    public IntervalType(BigDecimal yearPrecision) {
        this.yearToMonth = true;
        this.dayToSecond = false;
        this.yearPrecision = yearPrecision;
        this.dayPrecision = null;
        this.secondPrecision = null;
    }

    public IntervalType(BigDecimal dayPrecision, BigDecimal secondPrecision) {
        this.yearToMonth = false;
        this.dayToSecond = true;
        this.yearPrecision = null;
        this.dayPrecision = dayPrecision;
        this.secondPrecision = secondPrecision;
    }

    @Override
    public String getName() {
        if (this.dayToSecond) {
            return "INTERVAL DAY TO SECOND";
        }
        return "INTERVAL YEAR TO MONTH";
    }

    @Override
    public List<String> getArguments() {
        List<String> args = new ArrayList<>();
        if (this.yearPrecision != null) {
            args.add(this.yearPrecision.toString());
        }
        if (this.dayPrecision != null) {
            args.add(this.dayPrecision.toString());
        }
        if (this.secondPrecision != null) {
            args.add(this.secondPrecision.toString());
        }
        return args;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("INTERVAL");
        if (this.yearToMonth) {
            builder.append(" ").append("YEAR");
            if (this.yearPrecision != null) {
                builder.append("(").append(this.yearPrecision.toString()).append(")");
            }
            builder.append(" TO MONTH");
        } else if (this.dayToSecond) {
            builder.append(" ").append("DAY");
            if (this.dayPrecision != null) {
                builder.append("(").append(this.dayPrecision.toString()).append(")");
            }
            builder.append(" TO SECOND");
            if (this.secondPrecision != null) {
                builder.append("(").append(this.secondPrecision.toString()).append(")");
            }
        }
        return builder.toString();
    }

}
