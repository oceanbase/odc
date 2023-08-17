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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.Range;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import lombok.NonNull;

/**
 * {@link NumericRangeChecker}
 *
 * @author yh263208
 * @date 2022-06-23 20:48
 * @since ODC_release_3.3.2
 * @see ExpressionChecker
 */
public class NumericRangeChecker implements ExpressionChecker {
    /**
     * accept range string, eg.
     *
     * <pre>
     *     [1-3]
     *     [3]
     * </pre>
     */
    private final static String RANGE_PATTERN_STR = "^\\[([0-9]+)(\\-([0-9]+))?\\]$";
    private final static Pattern RANGE_PATTERN = Pattern.compile(RANGE_PATTERN_STR);

    @Override
    public boolean supports(@NonNull String expression) {
        Matcher matcher = RANGE_PATTERN.matcher(expression);
        return matcher.matches();
    }

    @Override
    public boolean contains(@NonNull String expression, @NonNull Object value) {
        String intValue = value.toString();
        if (!StringUtils.isNumeric(intValue)) {
            return false;
        }
        Matcher matcher = RANGE_PATTERN.matcher(expression);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Range string is illegal, " + expression);
        }
        int minimum = Integer.parseInt(matcher.group(1));
        String upperBound = matcher.group(3);
        int maximum = minimum;
        if (upperBound != null) {
            maximum = Integer.parseInt(upperBound);
        }
        Validate.isTrue(minimum <= maximum, "Maximum is not bigger than Minimum, " + expression);
        return Range.between(minimum, maximum).contains(Integer.parseInt(intValue));
    }

}
