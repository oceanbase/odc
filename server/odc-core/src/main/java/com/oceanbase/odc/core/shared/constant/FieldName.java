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

public enum FieldName implements Translatable {

    /**
     * Batch Import DataSource
     */
    DATASOURCE_NAME,
    DATASOURCE_TYPE,
    DATASOURCE_HOST,
    DATASOURCE_PORT,
    DATASOURCE_CLUSTERNAME,
    DATASOURCE_TENANTNAME,
    DATASOURCE_USERNAME,
    DATASOURCE_PASSWORD,
    DATASOURCE_ENVIRONMENT,
    DATASOURCE_SYSTENANTUSERNAME,
    DATASOURCE_SYSTENANTPASSWORD,
    DATASOURCE_ENVIRONMENT_DEFAULT,
    DATASOURCE_ENVIRONMENT_DEV,
    DATASOURCE_ENVIRONMENT_PROD,
    DATASOURCE_ENVIRONMENT_SIT,

    /**
     * Batch Import User
     */
    USER_ACCOUNTNAME,
    USER_NAME,
    USER_PASSWORD,
    USER_ENABLED,
    USER_ROLEIDS,
    USER_DESCRIPTION;

    @Override
    public String code() {
        return name();
    }

    public String getLocalizedMessage() {
        Locale locale = LocaleContextHolder.getLocale();
        return translate(null, locale);
    }
}
