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
package com.oceanbase.odc.service.regulation.ruleset.model;

import static com.oceanbase.odc.common.i18n.Translatable.I18N_KEY_PREFIX;

import org.springframework.context.i18n.LocaleContextHolder;

import com.oceanbase.odc.common.i18n.I18n;
import com.oceanbase.odc.common.i18n.Translatable;

import lombok.NonNull;

/**
 * @Author: Lebie
 * @Date: 2023/7/17 15:58
 * @Description: []
 */
public enum SqlConsoleRules implements Translatable {
    ALLOW_SQL_TYPES("allow-sql-types"),
    NOT_ALLOWED_DEBUG_PL("not-allowed-debug-pl"),
    NOT_ALLOWED_CREATE_PL("not-allowed-create-pl"),
    NOT_ALLOWED_EDIT_RESULTSET("not-allowed-edit-resultset"),
    NOT_ALLOWED_EXPORT_RESULTSET("not-allowed-export-resultset"),
    MAX_EXECUTE_SQLS("max-execute-sqls"),
    MAX_RETURN_ROWS("max-return-rows"),
    EXTERNAL_SQL_INTERCEPTOR("external-sql-interceptor");

    private String name;

    SqlConsoleRules(@NonNull String name) {
        this.name = name;
    }


    public String getRuleName() {
        return "${com.oceanbase.odc.builtin-resource.regulation.rule.sql-console." + this.name + ".name}";
    }

    public String getPropertyName() {
        return "${com.oceanbase.odc.builtin-resource.regulation.rule.sql-console." + this.name + ".metadata.name}";
    }

    public boolean isBooleanRule() {
        return this == NOT_ALLOWED_DEBUG_PL || this == NOT_ALLOWED_CREATE_PL || this == NOT_ALLOWED_EDIT_RESULTSET
                || this == NOT_ALLOWED_EXPORT_RESULTSET;
    }

    public String getLocalizedMessage(Object[] args) {
        return translate(args, "message");
    }


    @Override
    public String code() {
        return this.name;
    }

    private String prefix() {
        return I18N_KEY_PREFIX + "builtin-resource.regulation.rule.sql-console." + code();
    }

    private String translate(Object[] args, String subtype) {
        String key = prefix() + "." + subtype;
        return I18n.translate(key, args, key, LocaleContextHolder.getLocale());
    }
}
