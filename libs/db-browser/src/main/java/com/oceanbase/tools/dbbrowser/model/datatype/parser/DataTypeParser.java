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
package com.oceanbase.tools.dbbrowser.model.datatype.parser;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link DataTypeParser}, accept, eg.
 * 
 * <pre>
 *     number(5,3)
 *     varchar(10 char)
 * </pre>
 *
 * @author yh263208
 * @date 2022-06-27 10:40
 * @since ODC_release_3.4.0
 */
@Slf4j
public class DataTypeParser {

    private final static Pattern NAME_PATTERN = Pattern.compile("[a-zA-Z0-9_]+");
    private final static Pattern NUMBER_PATTERN = Pattern.compile("[0-9]+");
    private final List<DataTypeToken> tokens;

    public DataTypeParser(@NonNull List<DataTypeToken> tokens) {
        this.tokens = tokens;
    }

    public DataTypeParser(@NonNull String dataTypeName) {
        this(DataTypeParser.getTokens(dataTypeName));
    }

    public static List<DataTypeToken> getTokens(String content) {
        if (StringUtils.isBlank(content)) {
            return Collections.emptyList();
        }
        int length = content.length();
        List<DataTypeToken> tokens = new LinkedList<>();
        int start = 0;
        boolean matches = false;
        for (int i = 1; i <= length; i++) {
            String text = content.substring(start, i);
            if (text.trim().length() == 0) {
                start = i;
                continue;
            }
            int type = getType(text);
            if (type == DataTypeToken.INVALID_TYPE && !matches) {
                tokens.add(getToken(content, start, i));
                start = i;
            } else if (type != DataTypeToken.INVALID_TYPE) {
                matches = true;
            } else {
                i--;
                tokens.add(getToken(content, start, i));
                start = i;
                matches = false;
            }
        }
        if (start < length) {
            String text = content.substring(start, length);
            if (text.trim().length() != 0) {
                tokens.add(getToken(content, start, length));
            }
        }
        return tokens;
    }

    public void parse(@NonNull DataTypeTokenVisitor visitor) {
        for (DataTypeToken token : tokens) {
            if (token.getType() == DataTypeToken.INVALID_TYPE) {
                visit(token, visitor::visitUnknown);
            } else if (token.getType() == DataTypeToken.BRACKETS_TYPE) {
                visit(token, visitor::visitBrackets);
            } else if (token.getType() == DataTypeToken.NAME_TYPE) {
                visit(token, visitor::visitName);
            } else if (token.getType() == DataTypeToken.NUMBER_TYPE) {
                visit(token, visitor::visitNumber);
            } else {
                throw new IllegalStateException("Unknown token, " + token);
            }
        }
    }

    private void visit(DataTypeToken token, Consumer<DataTypeToken> consumer) {
        try {
            consumer.accept(token);
        } catch (Exception e) {
            log.warn("Failed to visit", e);
        }
    }

    private static int getType(String text) {
        int type = DataTypeToken.INVALID_TYPE;
        if (NUMBER_PATTERN.matcher(text).matches()) {
            type = DataTypeToken.NUMBER_TYPE;
        } else if (NAME_PATTERN.matcher(text).matches()) {
            type = DataTypeToken.NAME_TYPE;
        } else if ("(".equals(text) || ")".equals(text)) {
            type = DataTypeToken.BRACKETS_TYPE;
        }
        return type;
    }

    private static DataTypeToken getToken(String content, int start, int end) {
        String text = content.substring(start, end);
        return new CommonDataTypeToken(text, start, end, getType(text));
    }

}
