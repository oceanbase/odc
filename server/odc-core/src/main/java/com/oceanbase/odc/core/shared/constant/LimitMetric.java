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
package com.oceanbase.odc.core.shared.constant;

import java.util.Locale;

import org.springframework.context.i18n.LocaleContextHolder;

import com.oceanbase.odc.common.i18n.Translatable;
import com.oceanbase.odc.core.shared.exception.OverLimitException;

/**
 * limit metric, refer {@link OverLimitException} for details
 * 
 * @author yizhou.xw
 * @version : LimitMetric.java, v 0.1 2021-03-22 16:54
 */
public enum LimitMetric implements Translatable {

    TRANSFER_TASK_COUNT,
    MOCK_TASK_COUNT,
    OBCLIENT_INSTANCE_COUNT,
    FAILED_LOGIN_ATTEMPT_COUNT,
    SQL_LENGTH,
    SQL_SIZE,
    SQL_STATEMENT_COUNT,
    FILE_SIZE,
    FILE_COUNT,
    TRANSACTION_QUERY_LIMIT,
    SESSION_COUNT,
    USER_COUNT,
    EXPORT_OBJECT_COUNT,
    TABLE_NAME_LENGTH,
    WORKSHEET_CHANGE_COUNT,
    WORKSHEET_SAME_LEVEL_COUNT,
    WORKSHEET_COUNT_IN_PROJECT,
    WORKSPACE_COUNT_IN_PROJECT,
    DLM_ROW_LIMIT,
    DLM_DATA_SIZE_LIMIT;

    @Override
    public String code() {
        return name();
    }

    public String getLocalizedMessage() {
        Locale locale = LocaleContextHolder.getLocale();
        return translate(null, locale);
    }
}
