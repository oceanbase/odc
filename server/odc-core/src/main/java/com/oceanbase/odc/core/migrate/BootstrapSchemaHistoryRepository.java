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

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import javax.sql.DataSource;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.util.CollectionUtils;

import com.oceanbase.odc.common.util.ResourceUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * schema history persistent, the table schema define in migrate_schema_history_table_template.sql
 * 
 * @author yizhou.xw
 * @version : SchemaHistoryRepository.java, v 0.1 2021-03-26 14:26
 */
@Slf4j
public class BootstrapSchemaHistoryRepository implements SchemaHistoryRepository {

    private static final String DEFAULT_TABLE = "migrate_schema_history";
    private final String TABLE_TEMPLATE_FILE_NAME = "migrate_schema_history_table_template.sql";
    private final String ALL_COLUMNS = "install_rank, version, description, type, script, checksum,"
            + " installed_by, installed_on, execution_millis, success";

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedJdbcTemplate;

    private final SimpleJdbcInsert simpleJdbcInsert;
    private final String table;

    public BootstrapSchemaHistoryRepository(String table, DataSource dataSource) {
        Validate.notBlank(table, "parameter table may not be blank");
        Validate.notNull(dataSource, "parameter dataSource may not be null");

        this.dataSource = dataSource;
        this.table = table;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.namedJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
        this.simpleJdbcInsert = new SimpleJdbcInsert(jdbcTemplate).withTableName(table)
                .usingGeneratedKeyColumns("install_rank");

        try {
            initialize();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException("Initialize schema history repository failed", e);
        }
    }

    public BootstrapSchemaHistoryRepository(DataSource dataSource) {
        this(DEFAULT_TABLE, dataSource);
    }

    @Override
    public List<SchemaHistory> listAll() {
        String sql = new StringBuilder()
                .append("select ")
                .append(ALL_COLUMNS)
                .append(" from ")
                .append("`").append(table).append("`")
                .toString();
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(SchemaHistory.class));
    }

    @Override
    public List<SchemaHistory> listSuccess() {
        String selectLatestInstallRank = new StringBuilder()
                .append("select ")
                .append(" max(install_rank) as install_rank")
                .append(" from ")
                .append("`").append(table).append("`")
                .append(" where `success`=1")
                .append(" group by version, script")
                .toString();
        String selectInstallRankIn = new StringBuilder()
                .append("select ")
                .append(ALL_COLUMNS)
                .append(" from ")
                .append("`").append(table).append("`")
                .append(" where install_rank in (:installRanks)")
                .toString();
        List<Long> installRanks = jdbcTemplate.queryForList(selectLatestInstallRank, Long.class);
        if (CollectionUtils.isEmpty(installRanks)) {
            return Collections.emptyList();
        }
        MapSqlParameterSource parameterSource = new MapSqlParameterSource();
        parameterSource.addValue("installRanks", installRanks);
        return namedJdbcTemplate.query(selectInstallRankIn, parameterSource,
                new BeanPropertyRowMapper<>(SchemaHistory.class));
    }

    @Override
    public SchemaHistory create(SchemaHistory schemaHistory) {
        if (StringUtils.isEmpty(schemaHistory.getInstalledBy())) {
            schemaHistory.setInstalledBy(currentUser());
        }
        SqlParameterSource beanPropertySqlParameterSource = new BeanPropertySqlParameterSource(schemaHistory);
        Number installRank = simpleJdbcInsert.executeAndReturnKey(beanPropertySqlParameterSource);
        schemaHistory.setInstallRank(installRank.longValue());
        log.info("schema history created, history={}", schemaHistory);
        return schemaHistory;
    }

    private void initialize() throws IOException, ClassNotFoundException {
        String content;
        try (InputStream inputStream = ResourceUtils.getFileAsStream(TABLE_TEMPLATE_FILE_NAME)) {
            content = IOUtils.toString(inputStream, Charsets.UTF_8);
        }
        String replaced = StringUtils.replace(content, "${schema_history}", table);
        ByteArrayResource resource = new ByteArrayResource(replaced.getBytes(Charsets.UTF_8));

        ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator(resource);
        databasePopulator.setSqlScriptEncoding("UTF-8");
        databasePopulator.execute(dataSource);
        log.info("Schema history repository initialized.");
    }

    String currentUser() {
        String sql = "select CURRENT_USER() as user_name FROM DUAL";
        return jdbcTemplate.queryForObject(sql, String.class);
    }

}
