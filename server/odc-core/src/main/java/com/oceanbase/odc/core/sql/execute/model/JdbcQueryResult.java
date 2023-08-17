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
package com.oceanbase.odc.core.sql.execute.model;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import com.oceanbase.odc.core.sql.execute.mapper.JdbcRowMapper;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;

/**
 * Query SQL result data encapsulation
 *
 * @author yh263208
 * @date 2021-11-14 15:30
 * @since ODC_release_3.2.2
 */
@Getter
public class JdbcQueryResult {
    private final JdbcResultSetMetaData metaData;
    @Getter(AccessLevel.NONE)
    private final JdbcRowMapper mapper;
    private final List<List<Object>> rows = new LinkedList<>();

    public JdbcQueryResult(@NonNull ResultSetMetaData metaData, @NonNull JdbcRowMapper mapper) throws SQLException {
        this.mapper = mapper;
        this.metaData = new JdbcResultSetMetaData(metaData);
    }

    public void addLine(@NonNull ResultSet resultSet) throws SQLException, IOException {
        rows.add(mapper.mapRow(resultSet));
    }

    public Object get(int rowIndex, int colIndex) {
        if (rowIndex >= rows.size()) {
            throw new ArrayIndexOutOfBoundsException("RowIndex " + rowIndex);
        }
        List<Object> columns = this.rows.get(rowIndex);
        if (colIndex >= columns.size()) {
            throw new ArrayIndexOutOfBoundsException("ColIndex " + rowIndex);
        }
        return columns.get(colIndex);
    }

}

