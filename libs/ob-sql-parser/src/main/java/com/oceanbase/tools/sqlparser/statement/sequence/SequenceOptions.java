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
package com.oceanbase.tools.sqlparser.statement.sequence;

import java.math.BigDecimal;

import org.antlr.v4.runtime.ParserRuleContext;

import com.oceanbase.tools.sqlparser.statement.common.BaseOptions;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

/**
 * {@link SequenceOptions}
 * 
 * @author yh263208
 * @date 2023-06-01 19:24
 * @since ODC_release_4.2.0
 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class SequenceOptions extends BaseOptions {

    private BigDecimal maxValue;
    private BigDecimal minValue;
    private BigDecimal startWith;
    private Boolean noMaxValue;
    private Boolean noMinValue;
    private Boolean cycle;
    private Boolean noCycle;
    private BigDecimal cache;
    private Boolean noCache;
    private Boolean order;
    private Boolean noOrder;
    private BigDecimal incrementBy;

    public SequenceOptions(@NonNull ParserRuleContext context) {
        super(context);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (this.incrementBy != null) {
            builder.append(" INCREMENT BY ").append(this.incrementBy);
        }
        if (this.maxValue != null) {
            builder.append(" MAXVALUE ").append(this.maxValue);
        }
        if (this.minValue != null) {
            builder.append(" MINVALUE ").append(this.minValue);
        }
        if (this.startWith != null) {
            builder.append(" START WITH ").append(this.startWith);
        }
        if (Boolean.TRUE.equals(this.noMaxValue)) {
            builder.append(" NOMAXVALUE");
        }
        if (Boolean.TRUE.equals(this.noMinValue)) {
            builder.append(" NOMINVALUE");
        }
        if (Boolean.TRUE.equals(this.cycle)) {
            builder.append(" CYCLE");
        }
        if (Boolean.TRUE.equals(this.noCycle)) {
            builder.append(" NOCYCLE");
        }
        if (this.cache != null) {
            builder.append(" CACHE ").append(this.cache);
        }
        if (Boolean.TRUE.equals(this.noCache)) {
            builder.append(" NOCACHE");
        }
        if (Boolean.TRUE.equals(this.order)) {
            builder.append(" ORDER");
        }
        if (Boolean.TRUE.equals(this.noOrder)) {
            builder.append(" NOORDER");
        }
        return builder.length() == 0 ? "" : builder.substring(1);
    }

}
