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
package com.oceanbase.odc.metadb.ai;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.oceanbase.odc.common.jdbc.JdbcTemplateUtils;
import com.oceanbase.odc.common.util.StringUtils;

import lombok.NonNull;

@Repository
public class SchemaKBKeyValueRepository {

    @Autowired(required = false)
    @Qualifier("vectordbDataSource")
    private DataSource vectordbDataSource;

    public int saveAll(List<SchemaKBKeyValueEntity> entities) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(this.vectordbDataSource);
        String sql =
                "INSERT INTO knowledge_base_schema_key_value (key_content, value_content, database_id, tag, organization_id) "
                        +
                        "VALUES (?, ?, ?, ?, ?)";

        int[] rowsAffected = jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                SchemaKBKeyValueEntity entity = entities.get(i);
                ps.setString(1, entity.getKeyContent());
                ps.setString(2, entity.getValueContent());
                ps.setLong(3, entity.getDatabaseId());
                ps.setString(4, entity.getTag());
                ps.setLong(5, entity.getOrganizationId());
            }

            @Override
            public int getBatchSize() {
                return entities.size();
            }
        });
        return JdbcTemplateUtils.batchInsertAffectRows(rowsAffected);
    }

    public List<Long> findAllDatabaseIds() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(this.vectordbDataSource);
        return jdbcTemplate.query("select distinct database_id from knowledge_base_schema_key_value",
                (rs, rowNum) -> rs.getLong(1));
    }

    public int deleteByCreateTimeLater(@NonNull Date expireTime) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(this.vectordbDataSource);
        String sql = "delete from knowledge_base_schema_key_value where create_time <= :expireTime";
        MapSqlParameterSource paramSource = new MapSqlParameterSource()
                .addValue("expireTime", expireTime);
        NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
        return namedParameterJdbcTemplate.update(sql, paramSource);
    }

    public List<SchemaKBKeyValueEntity> findByKeyContentAndDatabaseIdAndTag(QueryKBKeyValueParams params) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(this.vectordbDataSource);
        StringBuilder sqlBuilder = new StringBuilder("SELECT * FROM knowledge_base_schema_key_value k WHERE");
        String likePredicate = "1=1";
        if (CollectionUtils.isNotEmpty(params.getKeys())) {
            likePredicate = params.getKeys().stream()
                    .map(s -> "k.key_content LIKE '%" + StringUtils.escapeLike(s) + "%'")
                    .collect(Collectors.joining(" or "));
        }
        String sql = sqlBuilder
                .append(" k.database_id = :databaseId AND k.tag = :tag")
                .append(" AND (").append(likePredicate).append(")").toString();
        MapSqlParameterSource paramSource = new MapSqlParameterSource()
                .addValue("databaseId", params.getDatabaseId()).addValue("tag", params.getTag());

        NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
        return namedParameterJdbcTemplate.query(sql, paramSource,
                new BeanPropertyRowMapper<>(SchemaKBKeyValueEntity.class));
    }

    public List<SchemaKBKeyValueEntity> findByKeySetAndDatabaseIdAndTag(QueryKBKeyValueParams params) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(this.vectordbDataSource);
        String sql =
                "SELECT * FROM knowledge_base_schema_key_value k WHERE k.key_content IN (:keyContents) AND k.database_id = :databaseId AND k.tag = :tag";
        MapSqlParameterSource paramSource = new MapSqlParameterSource()
                .addValue("keyContents", params.getKeys())
                .addValue("databaseId", params.getDatabaseId())
                .addValue("tag", params.getTag());

        NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);

        return namedParameterJdbcTemplate.query(sql, paramSource,
                new BeanPropertyRowMapper<>(SchemaKBKeyValueEntity.class));
    }

    public int deleteByValueContentAndDatabaseId(QueryKBKeyValueParams params) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(this.vectordbDataSource);
        String sql =
                "DELETE FROM knowledge_base_schema_key_value WHERE value_content = :valueContent AND database_id = :databaseId";
        MapSqlParameterSource paramSource = new MapSqlParameterSource()
                .addValue("valueContent", params.getValue())
                .addValue("databaseId", params.getDatabaseId());

        NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
        return namedParameterJdbcTemplate.update(sql, paramSource);
    }

    public int deleteByKeyContentAndDatabaseId(QueryKBKeyValueParams params) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(this.vectordbDataSource);
        StringBuilder sqlBuilder = new StringBuilder("DELETE FROM knowledge_base_schema_key_value WHERE ");
        String keyPredicate = params.getKeys().stream()
                .map(s -> "key_content = " + StringUtils.quoteMysqlValue(s))
                .collect(Collectors.joining(" or "));
        String sql = sqlBuilder
                .append(" (").append(keyPredicate).append(")")
                .append(" AND database_id = :databaseId AND tag = :tag").toString();
        MapSqlParameterSource paramSource = new MapSqlParameterSource()
                .addValue("databaseId", params.getDatabaseId()).addValue("tag", params.getTag());

        NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
        return namedParameterJdbcTemplate.update(sql, paramSource);
    }

}
