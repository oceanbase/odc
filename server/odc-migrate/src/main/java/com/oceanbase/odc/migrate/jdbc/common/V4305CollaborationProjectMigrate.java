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
package com.oceanbase.odc.migrate.jdbc.common;

import java.util.UUID;

import javax.sql.DataSource;

import org.springframework.transaction.annotation.Transactional;

import com.oceanbase.odc.core.migrate.JdbcMigratable;
import com.oceanbase.odc.core.migrate.Migratable;
import com.oceanbase.odc.metadb.collaboration.ProjectRepository;
import com.oceanbase.odc.service.common.util.SpringContextUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * @author jingtian
 * @date 2024/5/13
 */
@Slf4j
@Migratable(version = "4.3.0.5", description = "migrate historical collaboration_project data")
public class V4305CollaborationProjectMigrate implements JdbcMigratable {
    private ProjectRepository projectRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void migrate(DataSource dataSource) {
        this.projectRepository = SpringContextUtil.getBean(ProjectRepository.class);
        projectRepository.findAll().forEach(project -> {
            log.info("migrate project, project id: {}", project.getId());
            project.setUniqueIdentifier("ODC_" + UUID.randomUUID());
            projectRepository.save(project);
        });
        log.info("Migrate the unique_identifier of collaboration_project succeed");
    }
}
