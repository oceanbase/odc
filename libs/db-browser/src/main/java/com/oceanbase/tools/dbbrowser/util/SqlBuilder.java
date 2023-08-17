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
package com.oceanbase.tools.dbbrowser.util;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

import javax.validation.constraints.NotEmpty;

import lombok.NonNull;

/**
 * @author jingtian
 */
public abstract class SqlBuilder implements Appendable, CharSequence {
    private static final String LIST_DELIMITER = ",";
    private static final String WHITE_SPACE = " ";
    private static final String LINE_BREAKER = "\n";
    private static final String NAME_SEPARATOR = ".";
    private final StringBuilder sb;

    public SqlBuilder() {
        this.sb = new StringBuilder();
    }

    @Override
    public SqlBuilder append(CharSequence csq) {
        sb.append(csq);
        return this;
    }

    @Override
    public SqlBuilder append(CharSequence csq, int start, int end) {
        sb.append(csq, start, end);
        return this;
    }

    @Override
    public SqlBuilder append(char c) {
        sb.append(c);
        return this;
    }

    public SqlBuilder append(Object obj) {
        return append(String.valueOf(obj));
    }

    /**
     * append with identifier quote if expression=true
     *
     * @param identifier
     * @return SqlBuilder
     */
    public SqlBuilder identifierIf(String identifier, boolean expression) {
        if (expression) {
            return identifier(identifier);
        }
        return append(identifier);
    }

    /**
     * append with full identifier <br>
     * same as {@link #schemaPrefixIfNotBlank(String)} then {@link #identifier(String)}
     */
    public SqlBuilder identifier(String schemaName, String objectName) {
        schemaPrefixIfNotBlank(schemaName);
        return identifier(objectName);
    }

    /**
     * append with schema prefix if given schemaName is not blank <br>
     * will append as identifier first, then append NAME_SEPARATOR after
     *
     * @param schemaName
     * @return SqlBuilder
     */
    public SqlBuilder schemaPrefixIfNotBlank(String schemaName) {
        if (StringUtils.isBlank(schemaName)) {
            return this;
        }
        return this.identifier(schemaName).append(NAME_SEPARATOR);
    }

    /**
     * append with identifier quote
     *
     * @param identifier
     * @return SqlBuilder
     */
    public abstract SqlBuilder identifier(String identifier);

    /**
     * append with value quote
     *
     * @param value
     * @return SqlBuilder
     */
    public abstract SqlBuilder value(String value);

    /**
     * append default value <br>
     * - for mysql mode, will handle current_timestamp expression <br>
     * - for oracle mode, default value same as input value
     *
     * @param value
     * @return SqlBuilder
     */
    public abstract SqlBuilder defaultValue(String value);

    /**
     * append as list with value quote
     *
     * @param values
     * @return SqlBuilder
     */
    public SqlBuilder values(@NonNull List<String> values) {
        return appendList(values, this::value);
    }

    /**
     * append as list with identifier quote if expression==true
     *
     * @param identifiers
     * @param expression
     * @return SqlBuilder
     */
    public SqlBuilder identifiersIf(List<String> identifiers, boolean expression) throws IOException {
        if (expression) {
            return identifiers(identifiers);
        }
        return list(identifiers);
    }

    /**
     * append as list with identifier quote
     *
     * @param identifiers
     * @return SqlBuilder
     */
    public SqlBuilder identifiers(@NotEmpty List<String> identifiers) {
        return appendList(identifiers, this::identifier);
    }

    public SqlBuilder likeValueIfNotBlank(String value) {
        if (StringUtils.isBlank(value)) {
            return this;
        }
        return columnValueContains(null, value);
    }

    public SqlBuilder columnValueContains(String columnName, @NonNull String value) {
        if (StringUtils.isNotBlank(columnName)) {
            identifier(columnName);
        }
        append(" LIKE ").value("%" + StringUtils.escapeLike(value) + "%");
        return this;
    }

    /**
     * append without any quote
     *
     * @param list
     * @return SqlBuilder
     */
    public SqlBuilder list(@NotEmpty List<String> list) {
        return appendList(list, this::append);
    }

    public SqlBuilder space() {
        return append(WHITE_SPACE);
    }

    public SqlBuilder line() {
        return append(LINE_BREAKER);
    }

    private SqlBuilder appendList(List<String> list, Consumer<String> consumer) {
        int size = list.size();
        int pos = 0;
        for (String value : list) {
            pos++;
            consumer.accept(value);
            if (pos < size) {
                append(LIST_DELIMITER);
            }
        }
        return this;
    }

    @Override
    public int length() {
        return sb.length();
    }

    @Override
    public char charAt(int index) {
        return sb.charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return sb.subSequence(start, end);
    }

    public String substring(int start, int end) {
        return sb.substring(start, end);
    }

    @Override
    public String toString() {
        return sb.toString();
    }
}
