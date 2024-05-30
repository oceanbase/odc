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

public enum AuditEventType implements Translatable {
    PERSONAL_CONFIGURATION,

    PASSWORD_MANAGEMENT,

    CONNECTION_MANAGEMENT,

    SCRIPT_MANAGEMENT,

    DATABASE_OPERATION,

    ORGANIZATION_CONFIGURATION,

    RESOURCE_GROUP_MANAGEMENT,

    MEMBER_MANAGEMENT,

    AUDIT_EVENT,

    FLOW_CONFIG,

    ASYNC,

    MOCKDATA,

    IMPORT,

    EXPORT,

    EXPORT_RESULT_SET,

    SHADOWTABLE_SYNC,

    PARTITION_PLAN,

    ALTER_SCHEDULE,

    ONLINE_SCHEMA_CHANGE,

    APPLY_PROJECT_PERMISSION,

    APPLY_DATABASE_PERMISSION,

    APPLY_TABLE_PERMISSION,

    STRUCTURE_COMPARISON,

    DATA_MASKING_RULE,

    DATA_MASKING_POLICY,

    PERMISSION_APPLY,

    UNKNOWN_TASK_TYPE,

    DATABASE_MANAGEMENT,

    DATASOURCE_MANAGEMENT,

    PROJECT_MANAGEMENT,

    SQL_SECURITY_RULE_MANAGEMENT,

    DATABASE_PERMISSION_MANAGEMENT,

    TABLE_PERMISSION_MANAGEMENT,

    ENVIRONMENT_MANAGEMENT,

    AUTOMATION_RULE_MANAGEMENT,

    NOTIFICATION_MANAGEMENT,

    SENSITIVE_COLUMN_MANAGEMENT,

    ;


    @Override
    public String code() {
        return name();
    }

    public String getLocalizedMessage() {
        Locale locale = LocaleContextHolder.getLocale();
        return translate(null, locale);
    }
}
