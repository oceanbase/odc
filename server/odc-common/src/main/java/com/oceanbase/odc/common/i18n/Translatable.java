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

/**
 * @author yizhou.xw
 * @version : Translatable.java, v 0.1 2021-03-19 19:39
 */
public interface Translatable {
    String I18N_KEY_PREFIX = "com.oceanbase.odc.";

    /**
     * get i18n key code
     * 
     * @return i18n key code
     */
    String code();

    /**
     * get translated message
     * 
     * @param args args referenced by i18n message template defined in i18n resource files
     * @param locale target i18n locale
     * @return translated message
     */
    default String translate(Object[] args, Locale locale) {
        String key = I18N_KEY_PREFIX + this.getClass().getSimpleName() + "." + code();
        return I18n.translate(key, args, key, locale);
    }
}
