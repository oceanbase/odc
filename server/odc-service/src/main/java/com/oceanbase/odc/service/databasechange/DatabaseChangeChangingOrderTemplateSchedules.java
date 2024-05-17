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
package com.oceanbase.odc.service.databasechange;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.metadb.collaboration.ProjectEntity;
import com.oceanbase.odc.metadb.collaboration.ProjectRepository;
import com.oceanbase.odc.metadb.databasechange.DatabaseChangeChangingOrderTemplateEntity;
import com.oceanbase.odc.metadb.databasechange.DatabaseChangeChangingOrderTemplateRepository;
import com.oceanbase.odc.metadb.databasechange.DatabaseChangeChangingOrderTemplateSpecs;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.database.model.Database;

import lombok.extern.slf4j.Slf4j;

/**
 * @author: zijia.cj
 * @date: 2024/5/16
 */
@Slf4j
@Component
public class DatabaseChangeChangingOrderTemplateSchedules {
    private static final int PAGE_SIZE = 100;
    @Autowired
    private DatabaseChangeChangingOrderTemplateService templateService;
    @Autowired
    private DatabaseChangeChangingOrderTemplateRepository templateRepository;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private DatabaseService databaseService;


    @Scheduled(fixedDelayString = "${odc.task.databasechange.update-enable-interval-millis:180000}")
    public void syncTemplates() {
        int page = 0;
        Pageable pageable;
        Page<DatabaseChangeChangingOrderTemplateEntity> pageResult;

        do {
            pageable = PageRequest.of(page, PAGE_SIZE);
            Specification<DatabaseChangeChangingOrderTemplateEntity> specification =
                    Specification.where(DatabaseChangeChangingOrderTemplateSpecs.enabledEquals(true));
            pageResult = templateRepository.findAll(specification, pageable);
            Map<Long, List<DatabaseChangeChangingOrderTemplateEntity>> projectId2TemplateEntityList = pageResult
                    .getContent()
                    .stream().collect(Collectors.groupingBy(DatabaseChangeChangingOrderTemplateEntity::getProjectId));
            List<ProjectEntity> projectEntities = projectRepository.findByIdIn(projectId2TemplateEntityList.keySet());
            List<Long> archivedProjectIds = projectEntities.stream()
                    .filter(p -> Boolean.TRUE.equals(p.getArchived()))
                    .map(ProjectEntity::getId).collect(Collectors.toList());
            List<Long> disabledTemplateIds = projectId2TemplateEntityList.entrySet().stream()
                    .filter(entry -> archivedProjectIds.contains(entry.getKey()))
                    .flatMap(entry -> entry.getValue().stream()
                            .map(DatabaseChangeChangingOrderTemplateEntity::getId))
                    .collect(Collectors.toList());

            List<Long> nonArchivedProjectIds = projectEntities.stream()
                    .filter(p -> Boolean.FALSE.equals(p.getArchived()))
                    .map(ProjectEntity::getId).collect(Collectors.toList());
            Map<Long, List<Database>> projectId2Databases = this.databaseService
                    .listDatabasesByProjectIds(nonArchivedProjectIds);
            disabledTemplateIds.addAll(projectId2TemplateEntityList.entrySet().stream()
                    // 留下未归档的projectId2TemplateEntityList
                    .filter(entry -> nonArchivedProjectIds.contains(entry.getKey()))
                    .flatMap(entry -> {
                        List<Database> databases = projectId2Databases.get(entry.getKey());
                        if (CollectionUtils.isEmpty(databases)) {
                            return entry.getValue().stream().map(DatabaseChangeChangingOrderTemplateEntity::getId);
                        }
                        Set<Long> dbIds = databases.stream().map(Database::getId).collect(Collectors.toSet());
                        return entry.getValue().stream().filter(en -> {
                            Set<Long> templateDbIds = en.getDatabaseSequences().stream()
                                    .flatMap(Collection::stream).collect(Collectors.toSet());
                            return !CollectionUtils.containsAll(dbIds, templateDbIds);
                        }).map(DatabaseChangeChangingOrderTemplateEntity::getId);
                    }).collect(Collectors.toList()));
            templateRepository.updateEnabledByIds(disabledTemplateIds);
            page++;
        } while (pageResult.hasNext());
    }

}
