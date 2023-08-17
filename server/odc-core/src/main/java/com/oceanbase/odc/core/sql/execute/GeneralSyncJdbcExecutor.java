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
package com.oceanbase.odc.core.sql.execute;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.CallableStatementCallback;
import org.springframework.jdbc.core.CallableStatementCreator;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ParameterizedPreparedStatementSetter;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.StatementCallback;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Synchronous {@link SyncJdbcExecutor}, used to execute sql synchronously. The underlying
 * implementation is based on {@link org.springframework.jdbc.core.JdbcTemplate}
 *
 * @author yh263208
 * @date 2021-11-10 15:08
 * @since ODC_release_3.2.2
 */
@SuppressWarnings("all")
@Slf4j
public class GeneralSyncJdbcExecutor implements SyncJdbcExecutor {

    private final JdbcTemplate jdbcTemplate;

    public GeneralSyncJdbcExecutor(@NonNull DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public <T> T execute(ConnectionCallback<T> connectionCallback) throws DataAccessException {
        return this.jdbcTemplate.execute(connectionCallback);
    }

    @Override
    public <T> T execute(StatementCallback<T> statementCallback) throws DataAccessException {
        return this.jdbcTemplate.execute(statementCallback);
    }

    @Override
    public void execute(String s) throws DataAccessException {
        this.jdbcTemplate.execute(s);
    }

    @Override
    public <T> T query(String s, ResultSetExtractor<T> resultSetExtractor) throws DataAccessException {
        return this.jdbcTemplate.query(s, resultSetExtractor);
    }

    @Override
    public void query(String s, RowCallbackHandler rowCallbackHandler) throws DataAccessException {
        this.jdbcTemplate.query(s, rowCallbackHandler);
    }

    @Override
    public <T> List<T> query(String s, RowMapper<T> rowMapper) throws DataAccessException {
        return this.jdbcTemplate.query(s, rowMapper);
    }

    @Override
    public <T> Stream<T> queryForStream(String sql, RowMapper<T> rowMapper) throws DataAccessException {
        return this.jdbcTemplate.queryForStream(sql, rowMapper);
    }

    @Override
    public <T> T queryForObject(String s, RowMapper<T> rowMapper) throws DataAccessException {
        return this.jdbcTemplate.queryForObject(s, rowMapper);
    }

    @Override
    public <T> T queryForObject(String s, Class<T> aClass) throws DataAccessException {
        return this.jdbcTemplate.queryForObject(s, aClass);
    }

    @Override
    public Map<String, Object> queryForMap(String s) throws DataAccessException {
        return this.jdbcTemplate.queryForMap(s);
    }

    @Override
    public <T> List<T> queryForList(String s, Class<T> aClass) throws DataAccessException {
        return this.jdbcTemplate.queryForList(s, aClass);
    }

    @Override
    public List<Map<String, Object>> queryForList(String s) throws DataAccessException {
        return this.jdbcTemplate.queryForList(s);
    }

    @Override
    public SqlRowSet queryForRowSet(String s) throws DataAccessException {
        return this.jdbcTemplate.queryForRowSet(s);
    }

    @Override
    public int update(String s) throws DataAccessException {
        return this.jdbcTemplate.update(s);
    }

    @Override
    public int[] batchUpdate(String... strings) throws DataAccessException {
        return this.jdbcTemplate.batchUpdate(strings);
    }

    @Override
    public <T> T execute(PreparedStatementCreator preparedStatementCreator,
            PreparedStatementCallback<T> preparedStatementCallback) throws DataAccessException {
        return this.jdbcTemplate.execute(preparedStatementCreator, preparedStatementCallback);
    }

    @Override
    public <T> T execute(String s, PreparedStatementCallback<T> preparedStatementCallback) throws DataAccessException {
        return this.jdbcTemplate.execute(s, preparedStatementCallback);
    }

    @Override
    public <T> T query(PreparedStatementCreator preparedStatementCreator, ResultSetExtractor<T> resultSetExtractor)
            throws DataAccessException {
        return this.jdbcTemplate.query(preparedStatementCreator, resultSetExtractor);
    }

    @Override
    public <T> T query(String s, PreparedStatementSetter preparedStatementSetter,
            ResultSetExtractor<T> resultSetExtractor) throws DataAccessException {
        return this.jdbcTemplate.query(s, preparedStatementSetter, resultSetExtractor);
    }

    @Override
    public <T> T query(String s, Object[] objects, int[] ints, ResultSetExtractor<T> resultSetExtractor)
            throws DataAccessException {
        return this.jdbcTemplate.query(s, objects, ints, resultSetExtractor);
    }

    @Override
    public <T> T query(String s, Object[] objects, ResultSetExtractor<T> resultSetExtractor)
            throws DataAccessException {
        return this.jdbcTemplate.query(s, objects, resultSetExtractor);
    }

    @Override
    public <T> T query(String s, ResultSetExtractor<T> resultSetExtractor, Object... objects)
            throws DataAccessException {
        return this.jdbcTemplate.query(s, resultSetExtractor, objects);
    }

    @Override
    public void query(PreparedStatementCreator preparedStatementCreator, RowCallbackHandler rowCallbackHandler)
            throws DataAccessException {
        this.jdbcTemplate.query(preparedStatementCreator, rowCallbackHandler);
    }

    @Override
    public void query(String s, PreparedStatementSetter preparedStatementSetter,
            RowCallbackHandler rowCallbackHandler) throws DataAccessException {
        this.jdbcTemplate.query(s, preparedStatementSetter, rowCallbackHandler);
    }

    @Override
    public void query(String s, Object[] objects, int[] ints, RowCallbackHandler rowCallbackHandler)
            throws DataAccessException {
        this.jdbcTemplate.query(s, objects, ints, rowCallbackHandler);
    }

    @Override
    public void query(String s, Object[] objects, RowCallbackHandler rowCallbackHandler) throws DataAccessException {
        this.jdbcTemplate.query(s, objects, rowCallbackHandler);
    }

    @Override
    public void query(String s, RowCallbackHandler rowCallbackHandler, Object... objects) throws DataAccessException {
        this.jdbcTemplate.query(s, rowCallbackHandler, objects);
    }

    @Override
    public <T> List<T> query(PreparedStatementCreator preparedStatementCreator, RowMapper<T> rowMapper)
            throws DataAccessException {
        return this.jdbcTemplate.query(preparedStatementCreator, rowMapper);
    }

    @Override
    public <T> List<T> query(String s, PreparedStatementSetter preparedStatementSetter, RowMapper<T> rowMapper)
            throws DataAccessException {
        return this.jdbcTemplate.query(s, preparedStatementSetter, rowMapper);
    }

    @Override
    public <T> List<T> query(String s, Object[] objects, int[] ints, RowMapper<T> rowMapper)
            throws DataAccessException {
        return this.jdbcTemplate.query(s, objects, ints, rowMapper);
    }

    @Override
    public <T> List<T> query(String s, Object[] objects, RowMapper<T> rowMapper) throws DataAccessException {
        return this.jdbcTemplate.query(s, objects, rowMapper);
    }

    @Override
    public <T> List<T> query(String s, RowMapper<T> rowMapper, Object... objects) throws DataAccessException {
        return this.jdbcTemplate.query(s, rowMapper, objects);
    }

    @Override
    public <T> Stream<T> queryForStream(PreparedStatementCreator psc, RowMapper<T> rowMapper)
            throws DataAccessException {
        return this.jdbcTemplate.queryForStream(psc, rowMapper);
    }

    @Override
    public <T> Stream<T> queryForStream(String sql, PreparedStatementSetter pss, RowMapper<T> rowMapper)
            throws DataAccessException {
        return this.jdbcTemplate.queryForStream(sql, pss, rowMapper);
    }

    @Override
    public <T> Stream<T> queryForStream(String sql, RowMapper<T> rowMapper, Object... args) throws DataAccessException {
        return this.jdbcTemplate.queryForStream(sql, rowMapper, args);
    }

    @Override
    public <T> T queryForObject(String s, Object[] objects, int[] ints, RowMapper<T> rowMapper)
            throws DataAccessException {
        return this.jdbcTemplate.queryForObject(s, objects, ints, rowMapper);
    }

    @Override
    public <T> T queryForObject(String s, Object[] objects, RowMapper<T> rowMapper) throws DataAccessException {
        return this.jdbcTemplate.queryForObject(s, objects, rowMapper);
    }

    @Override
    public <T> T queryForObject(String s, RowMapper<T> rowMapper, Object... objects) throws DataAccessException {
        return this.jdbcTemplate.queryForObject(s, rowMapper, objects);
    }

    @Override
    public <T> T queryForObject(String s, Object[] objects, int[] ints, Class<T> aClass) throws DataAccessException {
        return this.jdbcTemplate.queryForObject(s, objects, ints, aClass);
    }

    @Override
    public <T> T queryForObject(String s, Object[] objects, Class<T> aClass) throws DataAccessException {
        return this.jdbcTemplate.queryForObject(s, objects, aClass);
    }

    @Override
    public <T> T queryForObject(String s, Class<T> aClass, Object... objects) throws DataAccessException {
        return this.jdbcTemplate.queryForObject(s, aClass, objects);
    }

    @Override
    public Map<String, Object> queryForMap(String s, Object[] objects, int[] ints) throws DataAccessException {
        return this.jdbcTemplate.queryForMap(s, objects, ints);
    }

    @Override
    public Map<String, Object> queryForMap(String s, Object... objects) throws DataAccessException {
        return this.jdbcTemplate.queryForMap(s, objects);
    }

    @Override
    public <T> List<T> queryForList(String s, Object[] objects, int[] ints, Class<T> aClass)
            throws DataAccessException {
        return this.jdbcTemplate.queryForList(s, objects, ints, aClass);
    }

    @Override
    public <T> List<T> queryForList(String s, Object[] objects, Class<T> aClass) throws DataAccessException {
        return this.jdbcTemplate.queryForList(s, objects, aClass);
    }

    @Override
    public <T> List<T> queryForList(String s, Class<T> aClass, Object... objects) throws DataAccessException {
        return this.jdbcTemplate.queryForList(s, aClass, objects);
    }

    @Override
    public List<Map<String, Object>> queryForList(String s, Object[] objects, int[] ints) throws DataAccessException {
        return this.jdbcTemplate.queryForList(s, objects, ints);
    }

    @Override
    public List<Map<String, Object>> queryForList(String s, Object... objects) throws DataAccessException {
        return this.jdbcTemplate.queryForList(s, objects);
    }

    @Override
    public SqlRowSet queryForRowSet(String s, Object[] objects, int[] ints) throws DataAccessException {
        return this.jdbcTemplate.queryForRowSet(s, objects, ints);
    }

    @Override
    public SqlRowSet queryForRowSet(String s, Object... objects) throws DataAccessException {
        return this.jdbcTemplate.queryForRowSet(s, objects);
    }

    @Override
    public int update(PreparedStatementCreator preparedStatementCreator) throws DataAccessException {
        return this.jdbcTemplate.update(preparedStatementCreator);
    }

    @Override
    public int update(PreparedStatementCreator preparedStatementCreator, KeyHolder keyHolder)
            throws DataAccessException {
        return this.jdbcTemplate.update(preparedStatementCreator, keyHolder);
    }

    @Override
    public int update(String s, PreparedStatementSetter preparedStatementSetter) throws DataAccessException {
        return this.jdbcTemplate.update(s, preparedStatementSetter);
    }

    @Override
    public int update(String s, Object[] objects, int[] ints) throws DataAccessException {
        return this.jdbcTemplate.update(s, objects, ints);
    }

    @Override
    public int update(String s, Object... objects) throws DataAccessException {
        return this.jdbcTemplate.update(s, objects);
    }

    @Override
    public int[] batchUpdate(String s, BatchPreparedStatementSetter batchPreparedStatementSetter)
            throws DataAccessException {
        return this.jdbcTemplate.batchUpdate(s, batchPreparedStatementSetter);
    }

    @Override
    public int[] batchUpdate(String s, List<Object[]> list) throws DataAccessException {
        return this.jdbcTemplate.batchUpdate(s, list);
    }

    @Override
    public int[] batchUpdate(String s, List<Object[]> list, int[] ints) throws DataAccessException {
        return this.jdbcTemplate.batchUpdate(s, list, ints);
    }

    @Override
    public <T> int[][] batchUpdate(String s, Collection<T> collection, int i,
            ParameterizedPreparedStatementSetter<T> parameterizedPreparedStatementSetter) throws DataAccessException {
        return this.jdbcTemplate.batchUpdate(s, collection, i, parameterizedPreparedStatementSetter);
    }

    @Override
    public <T> T execute(CallableStatementCreator callableStatementCreator,
            CallableStatementCallback<T> callableStatementCallback) throws DataAccessException {
        return this.jdbcTemplate.execute(callableStatementCreator, callableStatementCallback);
    }

    @Override
    public <T> T execute(String s, CallableStatementCallback<T> callableStatementCallback) throws DataAccessException {
        return this.jdbcTemplate.execute(s, callableStatementCallback);
    }

    @Override
    public Map<String, Object> call(CallableStatementCreator callableStatementCreator,
            List<SqlParameter> list) throws DataAccessException {
        return this.jdbcTemplate.call(callableStatementCreator, list);
    }

}
