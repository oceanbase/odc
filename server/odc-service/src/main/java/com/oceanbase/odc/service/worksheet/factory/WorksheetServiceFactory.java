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
package com.oceanbase.odc.service.worksheet.factory;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import com.oceanbase.odc.service.worksheet.model.WorksheetLocation;
import com.oceanbase.odc.service.worksheet.service.DefaultWorksheetService;
import com.oceanbase.odc.service.worksheet.service.RepoWorksheetService;
import com.oceanbase.odc.service.worksheet.service.WorksheetService;

/**
 * @author keyang
 * @date 2024/08/05
 * @since 4.3.2
 */
@Component
public class WorksheetServiceFactory {
    @Resource
    private DefaultWorksheetService defaultWorksheetService;
    @Resource
    private RepoWorksheetService repoWorksheetService;

    public WorksheetService getProjectFileService(WorksheetLocation location) {
        if (location == null) {
            return defaultWorksheetService;
        }
        if (location == WorksheetLocation.REPOS) {
            return repoWorksheetService;
        }
        return defaultWorksheetService;
    }
}
