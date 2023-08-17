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
package com.oceanbase.odc.core.migrate.resource.checker;

import java.util.Arrays;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import lombok.NonNull;

/**
 * {@link ListChecker}
 *
 * @author yh263208
 * @date 2022-06-23 21:02
 * @since ODC_release_3.3.2
 * @see ExpressionChecker
 */
public class ListChecker implements ExpressionChecker {
    /**
     * accept expression string, eg.
     *
     * <pre>
     *     [1,2]
     *     [abc,rotf]
     *     [00ooi]
     * </pre>
     */
    private final static String PATTERN_STR = "^\\[\\w+(,\\w+)*\\]$";
    private final static Pattern PATTERN = Pattern.compile(PATTERN_STR);

    @Override
    public boolean supports(@NonNull String expression) {
        Matcher matcher = PATTERN.matcher(expression);
        return matcher.matches();
    }

    @Override
    public boolean contains(@NonNull String expression, @NonNull Object value) {
        Matcher matcher = PATTERN.matcher(expression);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Input string is illegal, " + expression);
        }
        String[] values = expression.substring(1, expression.length() - 1).split(",");
        Set<String> valueSet = Arrays.stream(values).map(String::trim).collect(Collectors.toSet());
        return valueSet.contains(value.toString());
    }
}
