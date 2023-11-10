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

public enum AuditEventAction implements Translatable {
    /**
     * 个人配置
     */
    UPDATE_PERSONAL_CONFIGURATION,

    /**
     * 密码管理
     */
    SET_PASSWORD,

    CHANGE_PASSWORD,

    RESET_PASSWORD,

    /**
     * 连接管理
     */
    CREATE_CONNECTION,

    DELETE_CONNECTION,

    UPDATE_CONNECTION,

    CREATE_SESSION,

    CLOSE_SESSION,

    ENABLE_CONNECTION,

    DISABLE_CONNECTION,

    /**
     * 脚本管理
     */
    DOWNLOAD_SCRIPT,

    UPDATE_SCRIPT,

    DELETE_SCRIPT,

    UPLOAD_SCRIPT,

    /**
     * 组织配置
     */
    UPDATE_ORGANIZATION_CONFIGURATION,

    /**
     * 成员管理
     */
    ADD_USER,

    UPDATE_USER,

    DELETE_USER,

    ENABLE_USER,

    DISABLE_USER,

    ADD_ROLE,

    UPDATE_ROLE,

    DELETE_ROLE,

    ENABLE_ROLE,

    DISABLE_ROLE,

    /**
     * 资源组管理
     */
    ADD_RESOURCE_GROUP,

    UPDATE_RESOURCE_GROUP,

    DELETE_RESOURCE_GROUP,

    ENABLE_RESOURCE_GROUP,

    DISABLE_RESOURCE_GROUP,

    /**
     * 数据库操作
     */
    SELECT,

    DELETE,

    INSERT,

    REPLACE,

    UPDATE,

    SET,

    DROP,

    ALTER,

    TRUNCATE,

    CREATE,

    OTHERS,

    /**
     * 操作记录
     */
    EXPORT_AUDIT_EVENT,

    /**
     * 流程配置
     */
    CREATE_FLOW_CONFIG,

    UPDATE_FLOW_CONFIG,

    ENABLE_FLOW_CONFIG,

    DISABLE_FLOW_CONFIG,

    DELETE_FLOW_CONFIG,

    BATCH_DELETE_FLOW_CONFIG,

    /**
     * 任务流程
     */
    CREATE_TASK,

    STOP_TASK,

    ROLLBACK_TASK,

    EXECUTE_TASK,

    APPROVE,

    REJECT,

    CREATE_ASYNC_TASK,

    CREATE_MOCKDATA_TASK,

    CREATE_IMPORT_TASK,

    CREATE_EXPORT_TASK,

    CREATE_EXPORT_RESULT_SET_TASK,

    CREATE_PERMISSION_APPLY_TASK,

    CREATE_APPLY_PROJECT_PERMISSION_TASK,

    CREATE_SHADOWTABLE_SYNC_TASK,

    CREATE_PARTITION_PLAN_TASK,

    CREATE_ALTER_SCHEDULE_TASK,

    STOP_ASYNC_TASK,

    CREATE_ONLINE_SCHEMA_CHANGE_TASK,

    STOP_MOCKDATA_TASK,

    STOP_IMPORT_TASK,

    STOP_EXPORT_TASK,

    STOP_EXPORT_RESULT_SET_TASK,

    STOP_SHADOWTABLE_SYNC_TASK,

    STOP_PARTITION_PLAN_TASK,
    STOP_ALTER_SCHEDULE_TASK,

    STOP_ONLINE_SCHEMA_CHANGE_TASK,

    STOP_APPLY_PROJECT_PERMISSION_TASK,

    EXECUTE_ASYNC_TASK,

    EXECUTE_MOCKDATA_TASK,

    EXECUTE_IMPORT_TASK,

    EXECUTE_EXPORT_TASK,

    EXECUTE_EXPORT_RESULT_SET_TASK,

    EXECUTE_SHADOWTABLE_SYNC_TASK,

    EXECUTE_PARTITION_PLAN_TASK,

    EXECUTE_ALTER_SCHEDULE_TASK,

    EXECUTE_ONLINE_SCHEMA_CHANGE_TASK,

    APPROVE_ASYNC_TASK,

    APPROVE_MOCKDATA_TASK,

    APPROVE_IMPORT_TASK,

    APPROVE_EXPORT_TASK,

    APPROVE_EXPORT_RESULT_SET_TASK,

    APPROVE_PERMISSION_APPLY_TASK,

    APPROVE_SHADOWTABLE_SYNC_TASK,

    APPROVE_PARTITION_PLAN_TASK,
    APPROVE_ALTER_SCHEDULE_TASK,

    APPROVE_ONLINE_SCHEMA_CHANGE_TASK,

    APPROVE_APPLY_PROJECT_PERMISSION_TASK,

    REJECT_ASYNC_TASK,

    REJECT_MOCKDATA_TASK,

    REJECT_IMPORT_TASK,

    REJECT_EXPORT_TASK,

    REJECT_EXPORT_RESULT_SET_TASK,

    REJECT_PERMISSION_APPLY_TASK,

    REJECT_SHADOWTABLE_SYNC_TASK,

    REJECT_PARTITION_PLAN_TASK,
    REJECT_ALTER_SCHEDULE_TASK,

    REJECT_ONLINE_SCHEMA_CHANGE_TASK,

    REJECT_APPLY_PROJECT_PERMISSION_TASK,

    /**
     * 数据脱敏规则
     */
    CREATE_DATA_MASKING_RULE,

    UPDATE_DATA_MASKING_RULE,

    ENABLE_DATA_MASKING_RULE,

    DISABLE_DATA_MASKING_RULE,

    DELETE_DATA_MASKING_RULE,

    /**
     * 数据脱敏策略
     */
    CREATE_DATA_MASKING_POLICY,

    UPDATE_DATA_MASKING_POLICY,

    DELETE_DATA_MASKING_POLICY,

    /**
     * 集成
     */
    CREATE_INTEGRATION,

    ENABLE_INTEGRATION,

    DISABLE_INTEGRATION,

    DELETE_INTEGRATION,

    ADD_DATABASE,

    TRANSFER_DATABASE_TO_PROJECT,

    DELETE_DATABASE,

    CREATE_DATASOURCE,

    DELETE_DATASOURCE,

    UPDATE_DATASOURCE,

    CREATE_PROJECT,

    UPDATE_SQL_SECURITY_RULE;

    @Override
    public String code() {
        return name();
    }

    public String getLocalizedMessage() {
        Locale locale = LocaleContextHolder.getLocale();
        return translate(null, locale);
    }
}
