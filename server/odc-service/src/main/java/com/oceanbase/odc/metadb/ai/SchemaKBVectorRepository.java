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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.text.StringSubstitutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.oceanbase.odc.common.jdbc.JdbcTemplateUtils;
import com.oceanbase.odc.service.ai.Constants;

import lombok.NonNull;

/**
 * @author: liuyizhuo.lyz
 * @date: 2025/7/24
 */
@Repository
public class SchemaKBVectorRepository {

    @Autowired(required = false)
    @Qualifier("vectordbDataSource")
    private DataSource vectordbDataSource;

    public static String convertEmbeddingToString(List<Float> embedding) {
        if (embedding == null) {
            return null;
        }
        return embedding.toString();
    }

    public void createTableIfNotExists(@NonNull Long databaseId) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(this.vectordbDataSource);
        Map<String, String> variables = new HashMap<>();
        variables.put(Constants.DATABASE_ID_KEY, databaseId + "");
        StringSubstitutor substitutor = new StringSubstitutor(variables);
        jdbcTemplate.execute(substitutor.replace(Constants.CREATE_VECTOR_DDL_SQL_TEMPLATE));
    }

    public int saveAll(List<SchemaKBVectorEntity> entities) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(this.vectordbDataSource);
        String sql = "INSERT INTO copilot_vector_" + entities.get(0).getDatabaseId()
                + " (embedding, content, content_sha1, database_id, table_name, tag, organization_id) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";

        int[] rowsAffected = jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                SchemaKBVectorEntity entity = entities.get(i);
                ps.setString(1, convertEmbeddingToString(entity.getEmbedding()));
                ps.setString(2, entity.getContent());
                ps.setString(3, entity.getContentSha1());
                ps.setLong(4, entity.getDatabaseId());
                ps.setString(5, entity.getTableName());
                ps.setString(6, entity.getTag());
                ps.setLong(7, entity.getOrganizationId());
            }

            @Override
            public int getBatchSize() {
                return entities.size();
            }
        });
        return JdbcTemplateUtils.batchInsertAffectRows(rowsAffected);
    }

    public int deleteByCreateTimeLater(@NonNull Date expireTime, @NonNull Long databaseId) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(this.vectordbDataSource);
        String sql = "delete from copilot_vector_" + databaseId + " where create_time <= :expireTime";
        MapSqlParameterSource paramSource = new MapSqlParameterSource()
                .addValue("expireTime", expireTime);
        NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
        return namedParameterJdbcTemplate.update(sql, paramSource);
    }

    public List<SchemaKBVectorEntity> annSearchTopKByVectorAndDatabaseIdAndTag(QueryKBVectorParams params) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(this.vectordbDataSource);
        String sql = "SELECT *, l2_distance(embedding, :embedding) as distance " +
                "FROM copilot_vector_" + params.getDatabaseId() +
                " WHERE tag = :tag" +
                " ORDER BY l2_distance(embedding, :embedding) APPROXIMATE LIMIT :topK";

        MapSqlParameterSource paramSource = new MapSqlParameterSource()
                .addValue("embedding", convertEmbeddingToString(params.getEmbedding()))
                .addValue("tag", params.getTag())
                .addValue("topK", params.getTopK());

        NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
        return namedParameterJdbcTemplate.query(sql, paramSource,
                new BeanPropertyRowMapper<>(SchemaKBVectorEntity.class));
    }

    public int deleteByTableNameAndDatabaseId(QueryKBVectorParams params) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(this.vectordbDataSource);
        String sql = "DELETE FROM copilot_vector_"
                + params.getDatabaseId() + " WHERE table_name in (:tableNames)";
        MapSqlParameterSource paramSource = new MapSqlParameterSource()
                .addValue("tableNames", params.getTableNames());

        NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
        return namedParameterJdbcTemplate.update(sql, paramSource);
    }

}
