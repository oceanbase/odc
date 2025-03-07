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
package com.oceanbase.odc.service.config;

public class OrganizationConfigKeys {
    public static final String DEFAULT_QUERY_LIMIT = "odc.sqlexecute.default.queryLimit";

    public static final String DEFAULT_QUERY_COUNT = "odc.sqlexecute.default.queryCount";

    public static final String DEFAULT_ROLLBACK_PLAN_ENABLED = "odc.task.default.rollbackPlanEnabled";

    public static final String DEFAULT_TASK_DESCRIPTION_PROMPT = "odc.task.default.taskDescriptionPrompt";

    public static final String DEFAULT_IMPORT_TASK_STRUCTURE_REPLACEMENT_ENABLED =
            "odc.task.default.importTaskStructureReplacementEnabled";
}
