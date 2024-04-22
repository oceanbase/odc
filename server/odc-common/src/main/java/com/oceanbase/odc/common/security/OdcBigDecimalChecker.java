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
package com.oceanbase.odc.common.security;

import java.util.regex.Pattern;

import com.oceanbase.odc.common.util.StringUtils;

/**
 * @author gaoda.xy
 * @date 2024/4/22 15:31
 */
public class OdcBigDecimalChecker {

    private static final Pattern DIGITS_PATTERN = Pattern.compile("[+-]?[0-9.]+");

    private static final int MAX_EXPONENT_LENGTH = 4;

    /**
     * Check if the BigDecimal value passed in has a denial of service risk. Scientific notation is
     * allowed by default and the maximum length of the exponent is 4 (meaning 1e9999 is allowed but
     * 1e10000 is not allowed)
     * 
     * @param bigDecimal BigDecimal value passed in
     * @return true if the BigDecimal value is safe
     */
    public static boolean checkBigDecimalDoS(String bigDecimal) {
        return checkBigDecimalDoS(bigDecimal, DIGITS_PATTERN, true, MAX_EXPONENT_LENGTH);
    }


    /**
     * Check if the BigDecimal value passed in has a denial of service (DoS) risk
     * 
     * @param bigDecimal BigDecimal value passed in
     * @param regex Custom regular expression
     * @param allowScientificNotation Whether scientific notation is allowed
     * @param exponent Maximum length of the exponent in scientific notation
     * @return true if the BigDecimal value is safe
     */
    public static boolean checkBigDecimalDoS(String bigDecimal, Pattern regex, boolean allowScientificNotation,
            int exponent) {
        if (StringUtils.isBlank(bigDecimal)) {
            return true;
        }
        if (regex.matcher(bigDecimal).matches()) {
            return true;
        }
        if (allowScientificNotation) {
            return parseExp(bigDecimal, exponent);
        }
        return false;
    }

    /**
     * Parse the length of the exponent in scientific notation
     * 
     * @param bigDecimal BigDecimal value passed in
     * @param exponent Maximum length of the exponent in scientific notation
     * @return true if the length of the exponent is less than or equal to the specified value
     */
    private static boolean parseExp(String bigDecimal, int exponent) {
        String scientificNotation = bigDecimal.replaceAll("[+-]", "");
        char[] in = scientificNotation.toCharArray();
        int len = in.length;
        char c;
        for (int offset = 0; len > 0; offset++, len--) {
            c = in[offset];
            if ((c == 'e') || (c == 'E')) {
                if (len - 1 <= exponent) {
                    return true;
                }
            }
        }
        return false;
    }

}
