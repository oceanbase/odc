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
package com.oceanbase.tools.sqlparser.statement.select.oracle;

import java.util.List;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.ParserRuleContext;

import com.oceanbase.tools.sqlparser.statement.expression.BaseExpression;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * {@link XmlNamespaces}
 *
 * @author yh263208
 * @date 2024-02-27 21:17
 * @since ODC_release_4.2.4
 */
public class XmlNamespaces extends BaseExpression {

    private final List<XmlNamespace> namespaces;

    public XmlNamespaces(@NonNull ParserRuleContext context, @NonNull List<XmlNamespace> namespaces) {
        super(context);
        this.namespaces = namespaces;
    }

    public XmlNamespaces(@NonNull List<XmlNamespace> namespaces) {
        this.namespaces = namespaces;
    }

    @Override
    protected String doToString() {
        return "XMLNAMESPACES("
                + this.namespaces.stream().map(BaseExpression::toString).collect(Collectors.joining(",")) + ")";
    }

    /**
     * {@link XmlNamespace}
     *
     * @author yh263208
     * @date 2024-02-27 21:19
     * @since ODC_release_4.2.4
     */
    @Getter
    @Setter
    public static class XmlNamespace extends BaseExpression {

        private boolean defaultValue;
        private String alias;
        private final String value;

        public XmlNamespace(@NonNull ParserRuleContext context, @NonNull String value) {
            super(context);
            this.value = value;
        }

        public XmlNamespace(@NonNull String value) {
            this.value = value;
        }

        @Override
        protected String doToString() {
            StringBuilder stringBuilder = new StringBuilder();
            if (this.defaultValue) {
                stringBuilder.append("DEFAULT ");
            }
            stringBuilder.append(this.value);
            if (this.alias != null) {
                stringBuilder.append(" AS ").append(this.alias);
            }
            return stringBuilder.toString();
        }
    }

}
