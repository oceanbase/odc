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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.integration.jdbc.lock.JdbcLockRegistry;
import org.springframework.jdbc.core.JdbcTemplate;

import com.oceanbase.odc.common.lang.Holder;
import com.oceanbase.odc.core.migrate.DefaultSchemaHistoryRepository;
import com.oceanbase.odc.core.migrate.MigrateConfiguration;
import com.oceanbase.odc.core.migrate.Migrates;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author yizhou.xw
 * @version : AbstractMetadbMigrate.java, v 0.1 2021-04-02 14:50
 */
@Slf4j
@DependsOn({"lockConfiguration"})
abstract public class AbstractMetaDBMigrate {

    private static final String LOCK_KEY = "ODC_METADB_MIGRATE";
    private static final long TRY_LOCK_TIMEOUT_SECONDS = 60L;

    @Autowired
    protected DataSource dataSource;

    @Autowired
    private JdbcLockRegistry jdbcLockRegistry;

    abstract public MigrateConfiguration migrateConfiguration();

    @PostConstruct
    public void migrate() throws InterruptedException {
        log.info("try lock...");
        Lock lock = jdbcLockRegistry.obtain(LOCK_KEY);
        if (lock.tryLock(TRY_LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            try {
                log.info("get lock success");
                MigrateConfiguration configuration = migrateConfiguration();
                log.info("init configuration success, migrate starting, initVersion={}",
                        configuration.getInitVersion());

                new Migrates(configuration, new DefaultSchemaHistoryRepository(
                        configuration.getDataSource())).migrate();
                log.info("migrate success");
            } finally {
                lock.unlock();
            }
        } else {
            log.warn("failed to start migrate due try lock timeout, TRY_LOCK_TIMEOUT_SECONDS={}",
                    TRY_LOCK_TIMEOUT_SECONDS);
            throw new RuntimeException("failed to start migrate due try lock timeout");
        }
    }

    protected String getInitVersion() {
        try {
            return innerGetInitVersion();
        } catch (SQLException ex) {
            throw new RuntimeException("getInitVersion failed", ex);
        }
    }

    private String innerGetInitVersion() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            String version;
            List<String> tableList = new ArrayList<>();
            try (Statement stmt = connection.createStatement();
                    ResultSet resultSet = stmt.executeQuery("show tables;")) {
                while (resultSet.next()) {
                    String tableName = resultSet.getString(1);
                    tableList.add(tableName.toLowerCase());
                }
            }

            if (tableList.contains("odc_version_info")) {
                // 跟进版本信息表查询当前版本号
                try (Statement stmt = connection.createStatement();
                        ResultSet resultSet = stmt.executeQuery("select version_num from odc_version_info;")) {
                    if (resultSet.next()) {
                        version = resultSet.getString(1);
                        return version;
                    }
                }
            }
            // 判断版本号
            if (tableList.contains("odc_session_extended")) {
                version = OdcVersionEnum.V_220.getNumber();
            } else if (tableList.contains("odc_user_token")) {
                version = OdcVersionEnum.V_210.getNumber();
            } else if (tableList.contains("odc_user_info")) {
                version = OdcVersionEnum.V_200.getNumber();
            } else {
                // 未安装过ODC
                version = OdcVersionEnum.V_100.getNumber();
            }
            return version;
        }
    }

    protected Map<Long, Long> getOrganizationId2CreatorId() {
        if (!ifIamUserExists()) {
            return new HashMap<>();
        }
        String sql = "SELECT `organization_id`, `id` FROM `iam_user`";
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        Map<Long, Long> organizationId2CreatorId = new HashMap<>();
        jdbcTemplate.query(sql, resultSet -> {
            Object organizationId = resultSet.getObject("organization_id");
            Object creatorId = resultSet.getObject("id");
            if (organizationId == null || creatorId == null) {
                throw new IllegalStateException("OrganizationId or creatorId is null");
            }
            organizationId2CreatorId.putIfAbsent((Long) organizationId, (Long) creatorId);
        });
        return organizationId2CreatorId;
    }

    private boolean ifIamUserExists() {
        String sql = "SHOW FULL TABLES WHERE Table_type='BASE TABLE'";
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        Holder<Boolean> result = new Holder<>(false);
        jdbcTemplate.query(sql, resultSet -> {
            if ("iam_user".equalsIgnoreCase(resultSet.getString(1))) {
                result.setValue(true);
            }
        });
        return result.getValue();
    }

    /**
     * Created by mogao.zj
     */
    @SuppressWarnings("all")
    enum OdcVersionEnum {

        V_UNKNOWN("UNKNOWN"),
        V_100("1.0.0"),
        V_200("2.0.0"),
        V_210("2.1.0"),
        V_220("2.2.0"),
        V_221("2.2.1"),
        V_230("2.3.0"),
        V_231("2.3.1"),
        V_240("2.4.0"),
        V_241("2.4.1");

        @Getter
        private final String number;

        OdcVersionEnum(String number) {
            this.number = number;
        }
    }

}
