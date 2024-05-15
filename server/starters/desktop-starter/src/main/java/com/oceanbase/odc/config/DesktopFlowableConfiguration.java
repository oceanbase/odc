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
package com.oceanbase.odc.config;

import javax.sql.DataSource;

import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * {@code Flowable} Configuration for client mode
 *
 * @author yh263208
 * @date 2022-01-27 11:19
 * @since ODC_release_3.3.0
 * @see BaseFlowableConfiguration
 */
@Configuration
@Profile("clientMode")
public class DesktopFlowableConfiguration extends BaseFlowableConfiguration {

    @Override
    public SpringProcessEngineConfiguration springProcessEngineConfiguration(
            @Autowired @Qualifier("metadbTransactionManager") PlatformTransactionManager platformTransactionManager,
            DataSource dataSource) {
        SpringProcessEngineConfiguration processEngineCfg =
                super.springProcessEngineConfiguration(platformTransactionManager, dataSource);
        processEngineCfg.setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);
        return processEngineCfg;
    }

}
