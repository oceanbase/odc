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
package com.oceanbase.odc.service.permission.database.model;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.springframework.context.i18n.LocaleContextHolder;

import com.oceanbase.odc.common.i18n.Translatable;
import com.oceanbase.tools.dbbrowser.parser.constant.SqlType;

import lombok.Getter;

/**
 * @author gaoda.xy
 * @date 2024/1/3 14:04
 */
@Getter
public enum DatabasePermissionType implements Translatable {

    QUERY("query"),
    CHANGE("change"),
    EXPORT("export");

    private final String action;

    DatabasePermissionType(String action) {
        this.action = action;
    }

    public static List<DatabasePermissionType> all() {
        return Arrays.asList(DatabasePermissionType.values());
    }

    public static DatabasePermissionType from(String action) {
        if ("query".equalsIgnoreCase(action)) {
            return DatabasePermissionType.QUERY;
        }
        if ("change".equalsIgnoreCase(action)) {
            return DatabasePermissionType.CHANGE;
        }
        if ("export".equalsIgnoreCase(action)) {
            return DatabasePermissionType.EXPORT;
        }
        throw new IllegalArgumentException("unknown action: " + action);
    }

    public static DatabasePermissionType from(SqlType type) {
        switch (type) {
            case SELECT:
                return QUERY;
            case UPDATE:
            case DELETE:
            case INSERT:
            case CREATE:
            case DROP:
            case ALTER:
            case REPLACE:
            case TRUNCATE:
                return CHANGE;
            default:
                return null;
        }
    }

    @Override
    public String code() {
        return name();
    }

    public String getLocalizedMessage() {
        Locale locale = LocaleContextHolder.getLocale();
        return translate(null, locale);
    }

}
