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
package com.oceanbase.odc.service.regulation.risklevel;

import java.util.Objects;

import org.springframework.expression.spel.standard.SpelExpressionParser;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2023/6/19 19:47
 * @Description: []
 */
@Slf4j
public class RiskDetectUtils {
    private static final SpelExpressionParser PARSER = new SpelExpressionParser();

    public static boolean validate(Object root, String operation, Object value) {
        if (Objects.isNull(root)) {
            return false;
        }
        Object result;
        String assertStr = "'" + root + "'." + operation + "('" + value.toString() + "')";
        try {
            result = PARSER.parseExpression(assertStr).getValue();
        } catch (Exception ex) {
            log.warn("risk detect rule parse failed, ex");
            return false;
        }
        if (Objects.isNull(result)) {
            log.warn("risk detect rule parse failed, ex");
            return false;
        }
        return (boolean) result;
    }
}
