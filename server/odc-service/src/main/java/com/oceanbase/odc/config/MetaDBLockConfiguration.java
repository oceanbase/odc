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

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.integration.jdbc.lock.JdbcLockRegistry;
import org.springframework.integration.jdbc.lock.LockRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yizhou.xw
 * @version : LockConfiguration.java, v 0.1 2021-04-02 15:36
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@Configuration("metadbLockConfiguration")
public class MetaDBLockConfiguration extends BaseLockConfiguration {

    @Primary
    @Bean("metadbJdbcLockRepository")
    public JdbcLockRegistry metadbJdbcLockRepository(
            @Qualifier("metadbLockRepository") LockRepository metadbLockRepository) {
        return new JdbcLockRegistry(metadbLockRepository);
    }

    @Primary
    @Bean("metadbLockRepository")
    public LockRepository metadbLockRepository(
            DataSource dataSource, @Value("${server.port:8989}") String listenPort) {
        return buildLockRepository(dataSource, listenPort);
    }

}
