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
package com.oceanbase.tools.sqlparser.statement.createtable;

import org.antlr.v4.runtime.ParserRuleContext;

import com.oceanbase.tools.sqlparser.statement.BaseStatement;
import com.oceanbase.tools.sqlparser.statement.common.DataType;
import com.oceanbase.tools.sqlparser.statement.expression.ColumnReference;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * {@link ColumnDefinition}
 *
 * @author yh263208
 * @date 2022-12-26 14:49
 * @since ODC_release_4.1.0
 * @see BaseStatement
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class ColumnDefinition extends BaseStatement implements TableElement {

    private ColumnAttributes columnAttributes;
    private Boolean visible;
    private Location location;
    /**
     * if this field is true, means `BIGINT UNSIGNED NOT NULL AUTO_INCREMENT UNIQUE`
     */
    private boolean serial;
    private GenerateOption generateOption;
    private ForeignReference foreignReference;
    private final DataType dataType;
    private final ColumnReference columnReference;

    public ColumnDefinition(@NonNull ParserRuleContext context,
            @NonNull ColumnReference columnReference, DataType dataType) {
        super(context);
        this.dataType = dataType;
        this.columnReference = columnReference;
    }

    public ColumnDefinition(@NonNull ColumnReference columnReference, DataType dataType) {
        this.dataType = dataType;
        this.columnReference = columnReference;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(this.columnReference.toString());
        if (this.dataType != null) {
            builder.append(" ").append(this.dataType.toString());
        } else if (this.serial) {
            builder.append(" SERIAL");
        }
        if (this.visible != null) {
            if (this.visible) {
                builder.append(" ").append("VISIBLE");
            } else {
                builder.append(" ").append("INVISIBLE");
            }
        }
        if (this.generateOption != null) {
            builder.append(" ").append(this.generateOption.toString());
        }
        if (this.columnAttributes != null) {
            builder.append(" ").append(this.columnAttributes);
        }
        if (this.foreignReference != null) {
            builder.append(" ").append(this.foreignReference);
        }
        if (this.location != null) {
            builder.append(" ").append(this.location);
        }
        return builder.toString();
    }

    @Getter
    @EqualsAndHashCode
    @AllArgsConstructor
    public static class Location {
        private final String type;
        private final ColumnReference column;

        @Override
        public String toString() {
            if (this.column != null) {
                return this.type + " " + this.column;
            }
            return this.type;
        }
    }

}
