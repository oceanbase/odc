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
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.integration.jdbc.lock.DefaultLockRepository;
import org.springframework.integration.jdbc.lock.JdbcLockRegistry;
import org.springframework.integration.jdbc.lock.LockRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.oceanbase.odc.common.util.SystemUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yizhou.xw
 * @version : LockConfiguration.java, v 0.1 2021-04-02 15:36
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@Configuration("lockConfiguration")
public class LockConfiguration {
    private static final int LOCK_TIME_TO_LIVE_SECONDS = 60;
    private static final int CREATE_LOCK_TABLE_TIMEOUT_SECONDS = 10;
    private static final String CREATE_LOCK_TABLE_SQL = "CREATE TABLE IF NOT EXISTS DISTRIBUTED_LOCK (\n"
            + "  LOCK_KEY CHAR(36) NOT NULL,\n"
            + "  REGION VARCHAR(100) NOT NULL,\n"
            + "  CLIENT_ID CHAR(36),\n"
            + "  CREATED_DATE DATETIME(6) NOT NULL,\n"
            + "  constraint DISTRIBUTED_LOCK_PK primary key (LOCK_KEY, REGION)\n"
            + ")";

    @Bean("odcLockRepository")
    public LockRepository odcLockRepository(DataSource dataSource, @Value("${server.port:8989}") String listenPort) {
        String localIpAddress = SystemUtils.getLocalIpAddress();
        log.info("create odc lock repository..., localIpAddress={}, listenPort={}", localIpAddress, listenPort);
        initLockTable(dataSource);
        String lockId = String.format("%s:%s", localIpAddress, listenPort);
        DefaultLockRepository defaultLockRepository = new OdcLockRepository(dataSource, lockId);
        defaultLockRepository.setPrefix("DISTRIBUTED_");
        defaultLockRepository.setTimeToLive(LOCK_TIME_TO_LIVE_SECONDS * 1000);
        log.info("odc lock repository created.");
        return defaultLockRepository;
    }

    @Bean
    public JdbcLockRegistry jdbcLockRegistry(@Qualifier("odcLockRepository") LockRepository odcLockRepository) {
        return new JdbcLockRegistry(odcLockRepository);
    }

    private void initLockTable(DataSource dataSource) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.setQueryTimeout(CREATE_LOCK_TABLE_TIMEOUT_SECONDS);
        jdbcTemplate.execute(CREATE_LOCK_TABLE_SQL);
    }

    /**
     * Spring DefaultLockRepository use SERIALIZABLE isolation level, which is not supported by
     * oceanbase, so we define an OdcLockRepository use default isolation level
     *
     * @author yizhou.xw
     * @version : OdcLockRepository.java, v 0.1 2021-04-02 17:05
     */
    @Slf4j
    public static class OdcLockRepository extends DefaultLockRepository {
        public OdcLockRepository(DataSource dataSource, String id) {
            super(dataSource, id);
        }

        /**
         * change isolation level, due SERIALIZABLE not supported by oceanbase
         */
        @Transactional(timeout = 3)
        @Override
        public boolean acquire(String lock) {
            return super.acquire(lock);
        }
    }

}
