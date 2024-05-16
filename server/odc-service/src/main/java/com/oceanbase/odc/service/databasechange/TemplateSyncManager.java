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
import java.util.Objects;
import java.util.concurrent.Future;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.metadb.databasechange.DatabaseChangeChangingOrderTemplateEntity;
import com.oceanbase.odc.metadb.databasechange.DatabaseChangeChangingOrderTemplateRepository;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.database.model.Database;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @author: zijia.cj
 * @date: 2024/5/16
 */
@Service
@Slf4j
public class TemplateSyncManager {
    @Autowired
    private DatabaseService databaseService;
    @Autowired
    @Qualifier("syncTemplateTaskExecutor")
    private ThreadPoolTaskExecutor executor;

    @Autowired
    private DatabaseChangeChangingOrderTemplateRepository templateRepository;

    @SkipAuthorize("internal usage")
    public Future<Boolean> submitSyncTemplateTask(@NonNull DatabaseChangeChangingOrderTemplateEntity templateEntity) {
        return doExecute(() -> executor.submit(() -> syncTemplate(templateEntity)));
    }


    private Boolean syncTemplate(@NonNull DatabaseChangeChangingOrderTemplateEntity templateEntity)
            throws InterruptedException {
        try {
            List<Long> databaseIds = templateEntity.getDatabaseSequences().stream().flatMap(
                    Collection::stream).collect(
                            Collectors.toList());
            List<Database> databases = databaseService.listDatabasesByIds(databaseIds);
            if (!databases.stream()
                    .allMatch(database -> database.getProject() != null && Objects.equals(
                            database.getProject().getId(), templateEntity.getProjectId()))) {
                templateEntity.setEnabled(false);
                templateRepository.save(templateEntity);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Future<Boolean> doExecute(Supplier<Future<Boolean>> supplier) {
        try {
            return supplier.get();
        } catch (Exception ex) {
            throw new BadRequestException("sync template failed");
        }
    }

}
