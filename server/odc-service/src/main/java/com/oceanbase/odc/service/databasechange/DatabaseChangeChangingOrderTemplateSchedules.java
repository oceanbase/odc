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

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.metadb.databasechange.DatabaseChangeChangingOrderTemplateEntity;
import com.oceanbase.odc.metadb.databasechange.DatabaseChangeChangingOrderTemplateRepository;
import com.oceanbase.odc.metadb.databasechange.DatabaseChangeChangingOrderTemplateSpecs;

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

    @Scheduled(fixedDelayString = "${odc.task.databasechange.update-enable-interval-millis:180000}")
    public void syncTemplates() {
        int page = 0;
        Pageable pageable;
        Page<DatabaseChangeChangingOrderTemplateEntity> pageResult;
        do {
            pageable = PageRequest.of(page, PAGE_SIZE);
            Specification<DatabaseChangeChangingOrderTemplateEntity> specification =
                    Specification.where(DatabaseChangeChangingOrderTemplateSpecs.enabledEquals(true));
            pageResult = this.templateRepository.findAll(specification, pageable);
            Map<Long, Boolean> templateId2Status = this.templateService
                    .getChangingOrderTemplateId2EnableStatus(pageResult.getContent().stream()
                            .map(DatabaseChangeChangingOrderTemplateEntity::getId).collect(Collectors.toSet()));
            List<Long> disabledTemplateIds = templateId2Status.entrySet().stream()
                    .filter(e -> Boolean.FALSE.equals(e.getValue()))
                    .map(Entry::getKey).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(disabledTemplateIds)) {
                templateRepository.updateEnabledByIds(disabledTemplateIds);
            }
            page++;
        } while (pageResult.hasNext());
    }

}
