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
 * resource type
 * 
 * @author yizhou.xw
 * @version : ResourceType.java, v 0.1 2021-03-19 18:59
 */
public enum ResourceType implements Translatable {

    /**
     * ODC Resources, with 'ODC_' prefix
     */
    ODC_PRIVILEGE,
    ODC_ORGANIZATION,
    ODC_ROLE,
    ODC_RESOURCE_ROLE,
    ODC_USER,
    ODC_CONNECTION,
    ODC_PRIVATE_CONNECTION,
    ODC_CONNECT_LABEL,
    ODC_SESSION,
    ODC_TASK,
    ODC_SCRIPT,
    ODC_SNIPPET,
    ODC_SOURCE_FILE,
    ODC_FILE,
    ODC_RESOURCE_GROUP,
    ODC_SYSTEM_CONFIG,
    ODC_ASYNC_SQL_RESULT,
    ODC_FLOW_INSTANCE,
    ODC_FLOW_APPROVAL_INSTANCE,
    ODC_FLOW_TASK_INSTANCE,
    ODC_FLOW_GATEWAY_INSTANCE,
    ODC_FLOW_CONFIG,
    ODC_AUDIT_EVENT,
    ODC_BUCKET,
    ODC_STORAGE_OBJECT_METADATA,
    ODC_EXTERNAL_APPROVAL,
    ODC_TUTORIAL,
    ODC_DATA_MASKING_RULE,
    ODC_DATA_MASKING_POLICY,
    ODC_SHADOWTABLE_COMPARING_TASK,
    ODC_STRUCTURE_COMPARISON_TASK,
    ODC_SCHEDULE,

    ODC_SCHEDULE_TRIGGER,
    ODC_SCHEDULE_TASK,

    ODC_DLM_LIMITER_CONFIG,

    ODC_PL_DEBUG_SESSION,
    ODC_AUTOMATION_RULE,
    ODC_EXTERNAL_SQL_INTERCEPTOR,
    ODC_INTEGRATION,
    ODC_PROJECT,
    ODC_ENVIRONMENT,
    ODC_DATABASE,
    ODC_RULESET,
    ODC_RULE,
    ODC_SENSITIVE_COLUMN,
    ODC_SENSITIVE_RULE,
    ODC_MASKING_ALGORITHM,
    ODC_SENSITIVE_COLUMN_SCANNING_TASK,
    ODC_APPROVAL_FLOW_CONFIG,
    ODC_RISK_LEVEL,
    ODC_RISK_DETECT_RULE,
    ODC_INDIVIDUAL_ORGANIZATION,
    ODC_TEAM_ORGANIZATION,

    ODC_NOTIFICATION_CHANNEL,
    ODC_NOTIFICATION_POLICY,
    ODC_NOTIFICATION_MESSAGE,


    /**
     * OB Resources, with 'OB_' prefix
     */
    OB_CLUSTER,
    OB_TENANT,
    OB_USER,
    OB_DATABASE,
    OB_TABLE,
    OB_INDEX,
    OB_COLUMN,
    OB_CONSTRAINT,
    OB_VIEW,
    OB_SEQUENCE,
    OB_TRIGGER,
    OB_FUNCTION,
    OB_PROCEDURE,
    OB_PACKAGE,
    OB_TYPE,
    OB_SYNONYM,
    OB_SESSION,

    /**
     * Public Aliyun Resources, with 'ALIYUN_' prefix
     */
    ALIYUN_ACCOUNT,
    ALIYUN_SUB_ACCOUNT,

    ODC_JOB,
    ODC_TABLE;

    @Override
    public String code() {
        return name();
    }

    public String getLocalizedMessage() {
        Locale locale = LocaleContextHolder.getLocale();
        return translate(null, locale);
    }
}
