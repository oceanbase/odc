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

package com.oceanbase.tools.sqlparser.statement.expression;

import java.util.List;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.ParserRuleContext;
import org.apache.commons.collections4.CollectionUtils;

import com.oceanbase.tools.sqlparser.statement.BaseStatement;
import com.oceanbase.tools.sqlparser.statement.Expression;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

/**
 * {@link JsonOnOption}
 *
 * @author yh263208
 * @date 2023-09-26 15:50
 * @since ODC_release_4.2.2
 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class JsonOnOption extends BaseStatement {

    private Expression onError;
    private Expression onEmpty;
    private List<OnMismatch> onMismatches;
    private Expression onNull;

    public JsonOnOption(@NonNull ParserRuleContext context) {
        super(context);
    }

    public JsonOnOption(@NonNull ParserRuleContext begin, @NonNull ParserRuleContext end) {
        super(begin, end);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (this.onError != null) {
            builder.append(" ").append(this.onError).append(" ON ERROR_P");
        }
        if (this.onEmpty != null) {
            builder.append(" ").append(this.onEmpty).append(" ON EMPTY");
        }
        if (CollectionUtils.isNotEmpty(this.onMismatches)) {
            builder.append(" ")
                    .append(this.onMismatches.stream()
                            .map(OnMismatch::toString).collect(Collectors.joining(" ")));
        }
        if (this.onNull != null) {
            builder.append(" ").append(this.onNull).append(" ON NULL");
        }
        return builder.length() == 0 ? "" : builder.substring(1);
    }

    @Getter
    @EqualsAndHashCode(callSuper = false)
    public static class OnMismatch extends BaseStatement {
        private final Expression opt;
        private final List<String> mismatchTypes;

        public OnMismatch(@NonNull ParserRuleContext context, @NonNull Expression opt, List<String> mismatchTypes) {
            super(context);
            this.opt = opt;
            this.mismatchTypes = mismatchTypes;
        }

        public OnMismatch(@NonNull Expression opt, List<String> mismatchTypes) {
            this.opt = opt;
            this.mismatchTypes = mismatchTypes;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append(this.opt).append(" ON MISMATCH");
            if (CollectionUtils.isNotEmpty(this.mismatchTypes)) {
                builder.append(" (")
                        .append(String.join(",", this.mismatchTypes))
                        .append(")");
            }
            return builder.toString();
        }
    }

}
