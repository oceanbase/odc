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
package com.oceanbase.odc.service.schedule.export;

import java.util.List;

import com.oceanbase.odc.common.task.RouteLogCallable;
import com.oceanbase.odc.service.iam.model.User;
import com.oceanbase.odc.service.iam.util.SecurityContextUtils;
import com.oceanbase.odc.service.schedule.export.model.ImportTaskResult;
import com.oceanbase.odc.service.schedule.export.model.ScheduleTaskImportRequest;

public class ScheduleTaskImportCallable extends RouteLogCallable<List<ImportTaskResult>> {
    public static final String LOG_PATH_PATTERN = "%s/%s/%s/%s.log";
    public static final String WORK_SPACE = "scheduleTaskImport";
    public static final String LOG_NAME = "import";

    private final ScheduleTaskImporter scheduleTaskImporter;
    private final ScheduleTaskImportRequest request;
    private final User user;

    public ScheduleTaskImportCallable(User currentUser, String taskId,
            ScheduleTaskImporter scheduleTaskImporter, ScheduleTaskImportRequest request) {
        super(WORK_SPACE, taskId, LOG_NAME);
        this.scheduleTaskImporter = scheduleTaskImporter;
        this.request = request;
        this.user = currentUser;
    }

    @Override
    public List<ImportTaskResult> doCall() {
        log.info("Let's start import schedule task, request: {}", request);
        try {
            SecurityContextUtils.setCurrentUser(user);
            return scheduleTaskImporter.importSchedule(request);
        } catch (Exception e) {
            log.info("Import schedule task failed", e);
            throw e;
        }
    }
}
