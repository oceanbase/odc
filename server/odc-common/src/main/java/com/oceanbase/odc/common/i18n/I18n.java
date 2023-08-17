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
package com.oceanbase.odc.common.i18n;

import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.Validate;
import org.springframework.context.MessageSource;

/**
 * @author yizhou.xw
 * @version : I18n.java, v 0.1 2021-02-20 13:27
 */
public class I18n {
    private static MessageSource MESSAGE_SOURCE;
    static {
        I18n.MESSAGE_SOURCE = messageSource();
    }

    /**
     * if key not found, use default message
     */
    public static String translate(String key, Object[] args, String defaultMessage, Locale locale) {
        Validate.notNull(locale, "locale may not be null");
        Validate.notEmpty(key, "key may not be null or empty");
        return MESSAGE_SOURCE.getMessage(key, args, defaultMessage, locale);
    }

    /**
     * if key not found, throw NoSuchMessageException
     */
    public static String translate(String key, Object[] args, Locale locale) {
        Validate.notNull(locale, "locale may not be null");
        Validate.notEmpty(key, "key may not be null or empty");
        return MESSAGE_SOURCE.getMessage(key, args, locale);
    }

    /**
     * get all message in target locale,
     *
     * @param locale
     * @return
     */
    public static Map<String, String> getAllMessages(Locale locale) {
        return ((ExposedResourceMessageBundleSource) MESSAGE_SOURCE).getAllMessages(locale);
    }

    public static MessageSource getMessageSource() {
        return MESSAGE_SOURCE;
    }

    private static MessageSource messageSource() {
        ExposedResourceMessageBundleSource messageSource = new ExposedResourceMessageBundleSource();
        messageSource.addBasenames("classpath:i18n/ErrorMessages");
        messageSource.addBasenames("classpath:i18n/BusinessMessages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setDefaultLocale(Locale.US);
        // if not set, the behavior may unexpected, e.g. en_US may mapping to zh_CN
        messageSource.setFallbackToSystemLocale(false);
        return messageSource;
    }
}
