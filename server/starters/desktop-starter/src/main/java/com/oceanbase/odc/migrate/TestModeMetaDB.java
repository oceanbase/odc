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
package com.oceanbase.odc.migrate;

import java.util.Arrays;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2023/7/25 18:05
 * @Description: []
 */
@ConditionalOnExpression("#{environment.acceptsProfiles('test')}")
@Slf4j
@Configuration
@Component("metadbMigrate")
@DependsOn({"localObjectStorageFacade", "springContextUtil"})
public class TestModeMetaDB extends DesktopModeMetaDB {

    @Override
    protected List<String> getBasePackages() {
        return Arrays.asList("com.oceanbase.odc.migrate.jdbc.common", "com.oceanbase.odc.migrate.jdbc.web");
    }

}
