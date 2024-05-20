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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.context.i18n.LocaleContextHolder;

import com.oceanbase.odc.common.i18n.Translatable;
import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.tools.dbbrowser.parser.constant.SqlType;

import lombok.Getter;
import lombok.NonNull;

/**
 * @author gaoda.xy
 * @date 2024/1/3 14:04
 */
@Getter
public enum DatabasePermissionType implements Translatable {

    QUERY("query"),
    CHANGE("change"),
    EXPORT("export"),
    // The user has no database permission but has permissions to access the inner tables
    ACCESS("access");

    private final String action;

    DatabasePermissionType(String action) {
        this.action = action;
    }

    public static List<DatabasePermissionType> all() {
        return Arrays.asList(DatabasePermissionType.values());
    }

    public static DatabasePermissionType from(String action) {
        if (QUERY.action.equalsIgnoreCase(action)) {
            return DatabasePermissionType.QUERY;
        }
        if (CHANGE.action.equalsIgnoreCase(action)) {
            return DatabasePermissionType.CHANGE;
        }
        if (EXPORT.action.equalsIgnoreCase(action)) {
            return DatabasePermissionType.EXPORT;
        }
        if (ACCESS.action.equalsIgnoreCase(action)) {
            return DatabasePermissionType.ACCESS;
        }
        return null;
    }

    public static DatabasePermissionType from(@NonNull SqlType sqlType) {
        switch (sqlType) {
            case SELECT:
            case SHOW:
            case EXPLAIN:
            case DESC:
            case USE_DB:
                return QUERY;
            case UPDATE:
            case DELETE:
            case INSERT:
            case CREATE:
            case DROP:
            case ALTER:
            case REPLACE:
            case TRUNCATE:
            case OTHERS:
            case UNKNOWN:
                return CHANGE;
            default:
                return null;
        }
    }

    public static Set<DatabasePermissionType> from(@NonNull TaskType taskType) {
        Set<DatabasePermissionType> types = new HashSet<>();
        switch (taskType) {
            case EXPORT:
                types.add(EXPORT);
                break;
            case EXPORT_RESULT_SET:
                types.add(QUERY);
                types.add(EXPORT);
                break;
            case IMPORT:
            case MOCKDATA:
            case ASYNC:
            case SHADOWTABLE_SYNC:
            case PARTITION_PLAN:
            case ONLINE_SCHEMA_CHANGE:
            case ALTER_SCHEDULE:
            case STRUCTURE_COMPARISON:
                types.add(CHANGE);
                break;
            default:
                break;
        }
        return types;
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
