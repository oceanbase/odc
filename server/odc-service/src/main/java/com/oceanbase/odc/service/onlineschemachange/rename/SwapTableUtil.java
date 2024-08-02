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

package com.oceanbase.odc.service.onlineschemachange.rename;

import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.service.onlineschemachange.model.FullVerificationResult;
import com.oceanbase.odc.service.onlineschemachange.model.SwapTableType;

/**
 * @author yaobin
 * @date 2023-11-08
 * @since 4.2.3
 */
public class SwapTableUtil {

    public static boolean isSwapTableEnable(SwapTableType swapTableType, TaskStatus taskStatus,
            Double fullTransferProgressPercentage, FullVerificationResult fullVerificationResult) {
        return swapTableType == SwapTableType.MANUAL &&
                taskStatus == TaskStatus.RUNNING &&
                fullTransferProgressPercentage.intValue() == 100 &&
                (fullVerificationResult == FullVerificationResult.CONSISTENT ||
                        fullVerificationResult == FullVerificationResult.UNCHECK);
    }

    public static String escapeMySQLName(String name) {
        return escapeName(name, getQuota(DialectType.MYSQL));
    }

    public static String escapeOracleName(String name) {
        return escapeName(name, getQuota(DialectType.ORACLE));
    }

    public static String escapeName(String name, String quota) {
        return quota + name + quota;
    }

    public static String getQuota(DialectType dialectType) {
        switch (dialectType) {
            case MYSQL:
            case OB_MYSQL:
                return "`";
            default:
                return "\"";
        }
    }
}
