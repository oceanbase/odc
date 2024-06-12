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
package com.oceanbase.odc.service.connection.logicaldatabase.parser;

import org.apache.commons.lang.StringUtils;
import org.springframework.http.HttpStatus;

import com.oceanbase.odc.core.shared.constant.ErrorCode;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.exception.HttpException;
import com.oceanbase.tools.sqlparser.SyntaxErrorException;

/**
 * @Author: Lebie
 * @Date: 2024/4/23 14:03
 * @Description: []
 */
class BadExpressionException extends HttpException {

    public BadExpressionException(ErrorCode errorCode, Object[] args, String message) {
        super(errorCode, args, message);
    }

    public BadExpressionException(SyntaxErrorException ex) {
        super(ErrorCodes.LogicalTableBadExpressionSyntax,
                new Object[] {buildErrorMessage(ex.getText(), ex.getStart(), ex.getStop())},
                ErrorCodes.LogicalTableBadExpressionSyntax.getEnglishMessage(
                        new Object[] {buildErrorMessage(ex.getText(), ex.getStart(), ex.getStop())}));
    }

    @Override
    public HttpStatus httpStatus() {
        return HttpStatus.BAD_REQUEST;
    }

    private static String buildErrorMessage(String original, int start, int stop) {
        start = Math.max(start - 15, 0);
        return escapeWhitespace(StringUtils.substring(original, start, stop + 1));
    }

    private static String escapeWhitespace(String s) {
        StringBuilder buf = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (c == '\t') {
                buf.append("\\t");
            } else if (c == '\n') {
                buf.append("\\n");
            } else if (c == '\r') {
                buf.append("\\r");
            } else {
                buf.append(c);
            }
        }
        return buf.toString();
    }
}
