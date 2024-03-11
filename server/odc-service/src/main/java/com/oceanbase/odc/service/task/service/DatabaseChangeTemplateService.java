/*
 * Copyright (c) 2024 OceanBase.
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

package com.oceanbase.odc.service.task.service;

import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.metadb.task.DatabaseChangeTemplateRepository;
import com.oceanbase.odc.service.task.runtime.DatabaseChangeTemplateReq;
import com.oceanbase.odc.service.task.runtime.DatabaseChangeTemplateResp;

public class DatabaseChangeTemplateService {
    @Autowired
    private DatabaseChangeTemplateRepository databaseChangeTemplateRepository;

    public DatabaseChangeTemplateResp createDatabaseTemplate(DatabaseChangeTemplateReq req){

        return new DatabaseChangeTemplateResp();
    }

    public boolean deleteDatabseTemplateById(Long id){
        databaseChangeTemplateRepository.deleteById(id);
        return true;
    }


}
