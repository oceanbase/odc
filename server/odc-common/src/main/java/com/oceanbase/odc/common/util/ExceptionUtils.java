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
package com.oceanbase.odc.common.util;

import org.apache.commons.lang3.ArrayUtils;

public class ExceptionUtils extends org.apache.commons.lang3.exception.ExceptionUtils {
    private static final int ROOT_CAUSE_DEPTH = 5;

    public static String getRootCauseReason(final Throwable throwable) {
        String[] rootCauseStackTrace = getRootCauseStackTrace(throwable);
        StringBuilder rootReason = new StringBuilder();
        if (ArrayUtils.isNotEmpty(rootCauseStackTrace)) {
            int depth = ROOT_CAUSE_DEPTH;
            while (depth-- > 0) {
                if (rootCauseStackTrace.length > depth) {
                    rootReason.append(" ").append(rootCauseStackTrace[depth]);
                }
            }
        }
        return rootReason.toString();
    }

}
