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

package com.oceanbase.odc.service.db.model;

import org.springframework.context.i18n.LocaleContextHolder;

import com.oceanbase.odc.common.i18n.I18n;
import com.oceanbase.odc.common.i18n.Translatable;

import lombok.NonNull;

/**
 * @author jingtian
 * @date 2023/12/12
 * @since ODC_release_4.2.3
 */
public enum UpdateTableDdlCheck implements Translatable {
    DROP_INDEX("drop-index"),
    CREATE_INDEX("create-index"),
    DROP_AND_CREATE_INDEX("drop-and-create-index");

    private final String name;
    private static final String MESSAGE_CODE = "message";

    UpdateTableDdlCheck(@NonNull String name) {
        this.name = name;
    }

    @Override
    public String code() {
        return this.name;
    }

    public String getLocalizedMessage() {
        return translate(null, MESSAGE_CODE);
    }

    private String translate(Object[] args, String subtype) {
        String key = prefix() + "." + subtype;
        return I18n.translate(key, args, key, LocaleContextHolder.getLocale());
    }

    private String prefix() {
        return I18N_KEY_PREFIX + "generate-update-table-ddl-check." + code();
    }
}
