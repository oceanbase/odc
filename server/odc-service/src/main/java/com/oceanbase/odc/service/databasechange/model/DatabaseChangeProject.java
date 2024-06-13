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
package com.oceanbase.odc.service.databasechange.model;

import java.io.Serializable;

import com.oceanbase.odc.common.i18n.Internationalizable;
import com.oceanbase.odc.service.collaboration.project.model.Project;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class DatabaseChangeProject implements Serializable {

    private static final long serialVersionUID = -5013749085190365604L;
    private Long id;
    @Internationalizable
    private String name;

    public DatabaseChangeProject(Project project) {
        if (project != null) {
            this.id = project.getId();
            this.name = project.getName();
        }
    }

}
