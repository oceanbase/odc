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

import org.springframework.context.i18n.LocaleContextHolder;

import com.oceanbase.odc.common.i18n.Translatable;

/**
 * {@link Symbols}
 *
 * @author yh263208
 * @date 2024-05-09 21:54
 * @since ODC_release_4.2.4
 */
public enum Symbols implements Translatable {

    /**
     * [
     */
    LEFT_BRACKET,
    /**
     * ]
     */
    RIGHT_BRACKET;

    @Override
    public String code() {
        return name();
    }

    public String getLocalizedMessage() {
        return translate(null, LocaleContextHolder.getLocale());
    }

}
