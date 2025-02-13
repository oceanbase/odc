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
package com.oceanbase.tools.sqlparser.statement.select;

import java.util.List;

import org.antlr.v4.runtime.ParserRuleContext;
import org.apache.commons.collections4.CollectionUtils;

import com.oceanbase.tools.sqlparser.statement.BaseStatement;
import com.oceanbase.tools.sqlparser.statement.Expression;
import com.oceanbase.tools.sqlparser.statement.select.oracle.Pivot;
import com.oceanbase.tools.sqlparser.statement.select.oracle.UnPivot;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * {@link ExpressionReference}
 *
 * @author yh263208
 * @date 2022-11-25 10:41
 * @since ODC_release_4.1.0
 * @see FromReference
 */
@Getter
@EqualsAndHashCode(callSuper = false)
public class ExpressionReference extends BaseStatement implements FromReference {

    private final String alias;
    private final Expression target;
    @Setter
    private Pivot pivot;
    @Setter
    private UnPivot unPivot;
    @Setter
    private FlashbackUsage flashbackUsage;
    @Setter
    private boolean lateral;
    @Setter
    private List<String> aliasColumns;

    public ExpressionReference(@NonNull ParserRuleContext context,
            @NonNull Expression target, String alias) {
        super(context);
        this.target = target;
        this.alias = alias;
    }

    public ExpressionReference(@NonNull Expression target, String alias) {
        this.target = target;
        this.alias = alias;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (this.lateral) {
            builder.append("LATERAL ");
        }
        builder.append(this.target.toString());
        if (this.flashbackUsage != null) {
            builder.append(" ").append(this.flashbackUsage.toString());
        }
        if (this.pivot != null) {
            builder.append(" ").append(this.pivot.toString());
        }
        if (this.unPivot != null) {
            builder.append(" ").append(this.unPivot.toString());
        }
        if (this.alias != null) {
            builder.append(" ").append(this.alias);
        }
        if (CollectionUtils.isNotEmpty(this.aliasColumns)) {
            builder.append(" (").append(String.join(",", this.aliasColumns)).append(")");
        }
        return builder.toString();
    }

}
