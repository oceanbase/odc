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
package com.oceanbase.odc.service.flow.task.model;

/**
 * @author wenniu.ly
 * @date 2022/3/3
 */
public class RuntimeTaskConstants {
    public static final String PRE_CHECK_TASK_ID = "preCheckTaskId";
    public static final String TASK_ID = "taskId";
    public static final String FLOW_INSTANCE_ID = "flowInstanceId";
    public static final String TIMEOUT_MILLI_SECONDS = "timeOutMilliSeconds";
    public static final String CONNECTION_CONFIG = "connectionConfig";
    public static final String SCHEMA_NAME = "schemaName";
    public static final String PARAMETERS = "parameters";
    public static final String TASK_SUBMITTER = "taskSubmitter";
    public static final String TASK_CREATOR = "taskCreator";
    public static final String TASK_ORGANIZATION_ID = "taskOrganizationId";
    public static final String INTERCEPT_TASK_ID = "interceptTaskId";
    public static final String INTERCEPT_SQL_STATUS = "interceptSqlStatus";
    public static final String INTEGRATION_TEMPLATE_VARIABLES = "integrationTemplateVariables";
    public static final Integer DEFAULT_TASK_CHECK_INTERVAL_SECONDS = 5;
    public static final String RISKLEVEL_DESCRIBER = "riskLevelDescriber";
    public static final String RISKLEVEL = "riskLevel";
    public static final String SUCCESS_CREATE_EXT_INS = "successCreateExternalApprovalInstance";
    public static final String CLOUD_MAIN_ACCOUNT_ID = "cloudMainAccountId";
    public static final String CALLBACK_TASK = "_callback_task_";
}
