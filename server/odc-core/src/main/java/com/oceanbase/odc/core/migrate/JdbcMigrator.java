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
package com.oceanbase.odc.core.migrate;

import javax.sql.DataSource;

import org.apache.commons.lang3.Validate;

/**
 * @author yizhou.xw
 * @version : JdbcMigrate.java, v 0.1 2021-03-26 20:09
 */
class JdbcMigrator implements Migrator {

    private final Class<? extends JdbcMigratable> jdbcMigratable;
    private final Migratable migratableInfo;
    private final DataSource dataSource;

    JdbcMigrator(Class<? extends JdbcMigratable> jdbcMigratable, DataSource dataSource) {
        Validate.notNull(jdbcMigratable, "parameter jdbcMigratable may not be null");
        Validate.notNull(dataSource, "parameter dataSource may not be null");
        this.jdbcMigratable = jdbcMigratable;
        Migratable migratable = jdbcMigratable.getAnnotation(Migratable.class);
        Validate.notNull(migratable,
                String.format("the class '%s' miss '@Migratable' annotation", jdbcMigratable.getName()));
        this.migratableInfo = migratable;
        this.dataSource = dataSource;
        Validate.notEmpty(this.migratableInfo.version(),
                String.format("version may not be empty, className=%s", jdbcMigratable.getName()));
    }

    @Override
    public Behavior behavior() {
        return Behavior.fromAnnotation(migratableInfo);
    }

    @Override
    public Type type() {
        return Type.JDBC;
    }

    @Override
    public String version() {
        return migratableInfo.version();
    }

    @Override
    public String description() {
        return migratableInfo.description();
    }

    @Override
    public String script() {
        return jdbcMigratable.getName();
    }

    @Override
    public String checksum() {
        return "no checksum for JDBC";
    }

    @Override
    public boolean ignoreChecksum() {
        return migratableInfo.ignoreChecksum();
    }


    @Override
    public boolean doMigrate() {
        try {
            JdbcMigratable jdbcMigrator = this.jdbcMigratable.newInstance();
            jdbcMigrator.migrate(dataSource);
            return true;
        } catch (InstantiationException | IllegalAccessException e) {
            String message = String.format(
                    "create instance with class '%s' failed, please check if no-arg constructor missed",
                    jdbcMigratable.getName());
            throw new RuntimeException(message, e);
        }
    }
}
