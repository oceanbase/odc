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

package com.oceanbase.odc.service.task.constants;

/**
 * Task framework environment constants. Using 'ODC_' prefix to avoid duplication.
 * 
 * @author yaobin
 * @date 2023-11-21
 * @since 4.2.4
 */
public class JobEnvKeyConstants {

    public static final String ODC_JOB_CONTEXT = "ODC_JOB_CONTEXT";

    public static final String ODC_JOB_CONTEXT_FILE_PATH = "ODC_JOB_CONTEXT_FILE_PATH";

    public static final String ODC_TASK_RUN_MODE = "ODC_TASK_RUN_MODE";

    public static final String ODC_BOOT_MODE = "ODC_BOOT_MODE";

    public static final String ODC_LOG_DIRECTORY = "odc.log.directory";

    public static final String ODC_EXECUTOR_PORT = "ODC_EXECUTOR_PORT";

    public static final String ODC_SUPERVISOR_LISTEN_PORT = "ODC_SUPERVISOR_LISTEN_PORT";


    public static final String ODC_SERVICE_HOST = "ODC_SERVICE_HOST";

    public static final String ODC_SERVICE_PORT = "ODC_SERVICE_PORT";

    public static final String ODC_IMAGE_NAME = "ODC_IMAGE_NAME";

    public static final String OB_ARN_PARTITION = "OB_ARN_PARTITION";

    public static final String ODC_EXECUTOR_USER_ID = "ODC_EXECUTOR_USER_ID";

    public static final String ODC_OBJECT_STORAGE_CONFIGURATION = "ODC_OBJECT_STORAGE_CONFIGURATION";

    public static final String ODC_EXECUTOR_DATABASE_HOST = "ODC_EXECUTOR_DATABASE_HOST";

    public static final String ODC_EXECUTOR_DATABASE_PORT = "ODC_EXECUTOR_DATABASE_PORT";

    public static final String ODC_EXECUTOR_DATABASE_NAME = "ODC_EXECUTOR_DATABASE_NAME";

    public static final String ODC_EXECUTOR_DATABASE_USERNAME = "ODC_EXECUTOR_DATABASE_USERNAME";

    public static final String ODC_EXECUTOR_DATABASE_PASSWORD = "ODC_EXECUTOR_DATABASE_PASSWORD";

    public static final String ENCRYPT_KEY = "ENCRYPT_KEY";

    public static final String ENCRYPT_SALT = "ENCRYPT_SALT";

    public static final String REPORT_ENABLED = "REPORT_ENABLED";

    public static final String ODC_PROPERTY_ENCRYPTION_ALGORITHM = "ODC_PROPERTY_ENCRYPTION_ALGORITHM";

    /**
     * TODO: it is encryption password, not salt, should be renamed to ODC_PROPERTY_ENCRYPTION_PASSWORD
     */
    public static final String ODC_PROPERTY_ENCRYPTION_SALT = "ODC_PROPERTY_ENCRYPTION_SALT";

    public static final String ODC_PROPERTY_ENCRYPTION_PREFIX = "ODC_PROPERTY_ENCRYPTION_PREFIX";

    public static final String ODC_PROPERTY_ENCRYPTION_SUFFIX = "ODC_PROPERTY_ENCRYPTION_SUFFIX";
}
