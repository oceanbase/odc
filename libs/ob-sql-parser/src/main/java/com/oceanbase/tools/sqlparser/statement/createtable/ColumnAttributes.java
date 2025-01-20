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

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.ParserRuleContext;
import org.apache.commons.collections4.CollectionUtils;

import com.oceanbase.tools.sqlparser.statement.Expression;
import com.oceanbase.tools.sqlparser.statement.common.BaseOptions;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

/**
 * {@link ColumnAttributes}
 *
 * @author yh263208
 * @date 2023-06-05 11:00
 * @since ODC_release_4.2.0
 * @see BaseOptions
 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class ColumnAttributes extends BaseOptions {

    private Expression defaultValue;
    private Expression origDefault;
    private Boolean autoIncrement;
    private Integer id;
    private String comment;
    private Expression onUpdate;
    private String collation;
    private Integer srid;
    private List<InLineConstraint> constraints;
    private List<String> skipIndexTypes;

    public ColumnAttributes(@NonNull ParserRuleContext context) {
        super(context);
    }

    @Override
    @SuppressWarnings("all")
    protected void doMerge(PropertyDescriptor pd, Object otherVal)
            throws InvocationTargetException, IllegalAccessException {
        if (!"constraints".equals(pd.getName()) || this.constraints == null) {
            super.doMerge(pd, otherVal);
        } else {
            this.constraints = new ArrayList<>(this.constraints);
            this.constraints.addAll((List<? extends InLineConstraint>) otherVal);
        }
    }

    public List<InLineCheckConstraint> getCheckConstraints() {
        if (CollectionUtils.isEmpty(this.constraints)) {
            return Collections.emptyList();
        }
        return this.constraints.stream()
                .filter(c -> c instanceof InLineCheckConstraint)
                .map(c -> (InLineCheckConstraint) c).collect(Collectors.toList());
    }

    public List<InLineForeignConstraint> getForeignConstraints() {
        if (CollectionUtils.isEmpty(this.constraints)) {
            return Collections.emptyList();
        }
        return this.constraints.stream()
                .filter(c -> c instanceof InLineForeignConstraint)
                .map(c -> (InLineForeignConstraint) c).collect(Collectors.toList());
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (Boolean.TRUE.equals(this.autoIncrement)) {
            builder.append(" AUTO_INCREMENT");
        }
        if (this.defaultValue != null) {
            builder.append(" DEFAULT ").append(this.defaultValue);
        }
        if (this.origDefault != null) {
            builder.append(" ORIG_DEFAULT ").append(this.origDefault);
        }
        if (this.id != null) {
            builder.append(" ID ").append(this.id);
        }
        if (this.comment != null) {
            builder.append(" COMMENT ").append(this.comment);
        }
        if (this.onUpdate != null) {
            builder.append(" ON UPDATE ").append(this.onUpdate);
        }
        if (CollectionUtils.isNotEmpty(this.constraints)) {
            builder.append(" ").append(this.constraints.stream()
                    .map(InLineConstraint::toString)
                    .collect(Collectors.joining(" ")));
        }
        if (this.srid != null) {
            builder.append(" SRID ").append(this.srid);
        }
        if (this.collation != null) {
            builder.append(" COLLATE ").append(this.collation);
        }
        if (this.skipIndexTypes != null) {
            builder.append(" SKIP_INDEX (")
                    .append(String.join(",", skipIndexTypes))
                    .append(")");
        }
        return builder.length() == 0 ? "" : builder.substring(1);
    }

}
