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
package com.oceanbase.odc.service.ai.knowledgebase.dbschema;

import java.time.Duration;
import java.util.concurrent.Executors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.service.common.util.ConditionalOnProperty;

import lombok.extern.slf4j.Slf4j;

/**
 * @author: liuyizhuo.lyz
 * @date: 2025/7/21
 */
@Slf4j
@Component
@ConditionalOnProperty(value = "odc.ai.kb.enable-build-kb", havingValues = "true")
public class SchemaKBScheduler implements SchedulingConfigurer {

    @Autowired
    private SchemaKBProperties schemaKBProperties;
    @Autowired
    private SchemaKBBuildManager schemaKBBuildManager;

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        if (!schemaKBProperties.isEnableBuildKB()) {
            log.info("SchemaKBScheduler is disabled");
            return;
        }
        taskRegistrar.setScheduler(Executors.newSingleThreadScheduledExecutor());
        taskRegistrar.addFixedDelayTask(() -> {
            try {
                schemaKBBuildManager.submitBuildSchemaKBTaskForAllOrganizations();
            } catch (Exception e) {
                log.warn("Failed to build schema kb", e);
            }
        }, Duration.ofSeconds(schemaKBProperties.getBuildKBFixedDelaySeconds()));
    }

}
