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
package com.oceanbase.odc.service.i18n;

import java.util.Locale;

import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;

/**
 * @author yizhou.xw
 */
@Service
@SkipAuthorize("odc internal usage")
public class I18nService {

    @Autowired
    private MessageSource messageSource;

    public <E extends Enum<E>> String i18n(E enumValue, Object... args) {
        return i18n(LocaleContextHolder.getLocale(), enumValue, args);
    }

    /**
     * 枚举值的 i18n key 模板为 `${enumClassSimpleName}.${enumValueName}`
     */
    public <E extends Enum<E>> String i18n(Locale locale, E enumValue, Object... args) {
        Validate.notNull(enumValue, "enumValue require not null");
        return i18n(locale, enumValue.getClass().getSimpleName() + "." + enumValue.name(), args);
    }

    /**
     * 根据 key 返回 i18n value, locale 从当前用户会话获取 <br>
     * 如果在 i18n 资源中未找到，则返回 key 本身作为 默认的 message
     * 
     * @param locale locale
     * @param key i18n key
     * @param args i18n args
     * @return i18ned message
     */
    public String i18n(Locale locale, String key, Object... args) {
        Validate.notNull(locale, "locale require not null");
        Validate.notEmpty(key, "key require not empty");
        return messageSource.getMessage(key, args, key, locale);
    }

}
