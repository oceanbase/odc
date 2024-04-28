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
 * @author wenniu.ly
 * @date 2021/3/15
 */
public enum TaskType implements Translatable {
    /**
     * Database change
     */
    ASYNC,
    /**
     * Import
     */
    IMPORT,
    /**
     * Export
     */
    EXPORT,
    /**
     * Mock data
     */
    MOCKDATA,
    /**
     * Rollback
     */
    ROLLBACK,
    /**
     * Apply permission
     */
    @Deprecated
    PERMISSION_APPLY,
    /**
     * Shadow table sync
     */
    SHADOWTABLE_SYNC,
    /**
     * Database partition plan
     */
    PARTITION_PLAN,
    /**
     * SQL check
     */
    SQL_CHECK,
    /**
     * Database scheduled change
     */
    ALTER_SCHEDULE,
    /**
     * Auto-generate rollback SQL
     */
    GENERATE_ROLLBACK,
    /**
     * Online schema change
     */
    ONLINE_SCHEMA_CHANGE,
    /**
     * Export result set
     */
    EXPORT_RESULT_SET,
    /**
     * Pre-check
     */
    PRE_CHECK,
    /**
     * Apply project permission
     */
    APPLY_PROJECT_PERMISSION,
    /**
     * Apply database permission
     */
    APPLY_DATABASE_PERMISSION,
    /**
     * Apply table permission
     */
    APPLY_TABLE_PERMISSION,
    /**
     * Structure comparison
     */
    STRUCTURE_COMPARISON;

    @Override
    public String code() {
        return name();
    }

    public String getLocalizedMessage() {
        Locale locale = LocaleContextHolder.getLocale();
        return translate(null, locale);
    }

    public boolean needsPreCheck() {
        return this == ASYNC || this == ONLINE_SCHEMA_CHANGE || this == ALTER_SCHEDULE || this == EXPORT_RESULT_SET;
    }

    public boolean needForExecutionStrategy() {
        return !(this == PRE_CHECK || this == SQL_CHECK || this == GENERATE_ROLLBACK);
    }

}
