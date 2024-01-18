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
package com.oceanbase.odc.service.task.util;

import java.util.Calendar;
import java.util.Date;

/**
 * @author yaobin
 * @date 2024-01-04
 * @since 4.2.4
 */
public class JobDateUtils {

    public static Date getCurrentDate() {
        return new Date();
    }

    public static Date getCurrentDateSubtractDays(int days) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(getCurrentDate());
        cal.add(Calendar.DATE, Math.negateExact(days));
        return cal.getTime();
    }

    public static Date getCurrentDateSubtractSeconds(int seconds) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(getCurrentDate());
        cal.add(Calendar.SECOND, Math.negateExact(seconds));
        return cal.getTime();
    }
}
