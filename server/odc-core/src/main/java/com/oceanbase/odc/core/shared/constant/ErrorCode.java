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

/**
 * @author yizhou.xw
 * @version : ErrorCode.java, v 0.1 2021-02-19 15:43
 */
public interface ErrorCode extends Translatable {

    /**
     * @return error code, e.g. BadArgument
     */
    @Override
    String code();

    /**
     * get localized message
     * 
     * @param args
     * @return localized message by current user locale
     */
    default String getLocalizedMessage(Object[] args) {
        Locale locale = LocaleContextHolder.getLocale();
        return translate(args, locale);
    }

    default String getEnglishMessage(Object[] args) {
        return translate(args, Locale.US);
    }
}
