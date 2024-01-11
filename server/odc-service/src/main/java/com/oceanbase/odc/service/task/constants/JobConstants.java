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
 * @author yaobin
 * @date 2023-11-15
 * @since 4.2.4
 */
public class JobConstants {

    public static final String TEMPLATE_KIND_POD = "Pod";

    public static final String TEMPLATE_KIND_JOB = "Job";

    public static final String TEMPLATE_API_VERSION = "v1";

    public static final String TEMPLATE_BATCH_API_VERSION = "batch/v1";

    public static final String TEMPLATE_JOB_NAME_PREFIX = "odc-job-";

    public static final String RESTART_POLICY_NEVER = "Never";

    public static final String IMAGE_PULL_POLICY_NEVER = "Never";

    public static final String IMAGE_PULL_POLICY_ALWAYS = "Always";

    public static final String FIELD_SELECTOR_METADATA_NAME = "metadata.name";

    public static final String ODC_BOOT_MODE_EXECUTOR = "TASK_EXECUTOR";

    public static final String QUARTZ_DATA_MAP_JOB_CONTEXT = JobEnvConstants.TASK_ALL_PARAMETERS;

    public static final int REPORT_TASK_INFO_INTERVAL_SECONDS = 5;

}
