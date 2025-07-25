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
package com.oceanbase.odc.service.llm.migration;

import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.metadb.llm.LlmModelRepository;
import com.oceanbase.odc.service.llm.model.ProviderType;
import com.oceanbase.odc.service.llm.provider.LlmProviderFacade;
import com.oceanbase.odc.service.llm.provider.LlmProviderFacades;
import com.oceanbase.odc.service.llm.provider.ModelCredential;
import com.oceanbase.odc.service.llm.provider.ProviderCredential;

import lombok.extern.slf4j.Slf4j;

/**
 * @author: liuyizhuo.lyz
 * @date: 2025/7/16
 */
@Component
@Slf4j
public class LlmModelMigration {

    @Autowired
    private LlmProviderFacades llmProviderFacades;
    @Autowired
    private LlmModelRepository llmModelRepository;

    @Async
    @PostConstruct
    public void asyncInit() {
        migrateAllDeprecated();
    }

    public void migrateAllDeprecated() {
        Map<String, LlmProviderFacade> facades =
                llmProviderFacades.getProviderFacades();
        for (LlmProviderFacade<ModelCredential, ProviderCredential> facade : facades.values()) {
            try {
                ProviderType provider = facade.getProviderType();
                List<String> models = facade.generateModelsByProviderCredential(null)
                        .stream().filter(ModelCredential::isDeprecated)
                        .map(ModelCredential::getModelName).toList();
                if (CollectionUtils.isEmpty(models)) {
                    continue;
                }
                updateDeprecatedStatus(provider, models);
            } catch (Exception e) {
                log.warn("Failed to generate models for provider {}", facade, e);
            }
        }
        log.info("Migrated all deprecated models");
    }

    private void updateDeprecatedStatus(ProviderType provider, List<String> models) {
        int rows = llmModelRepository.updateAllDeprecatedByProviderNameAndModelNames(provider.name(), models, true);
        log.info("Updated model deprecated status for provider {} and models {}, rows affected: {}",
                provider, models, rows);
    }

}
