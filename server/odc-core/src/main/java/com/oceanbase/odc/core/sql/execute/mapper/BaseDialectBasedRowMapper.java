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
package com.oceanbase.odc.core.sql.execute.mapper;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.tools.dbbrowser.model.datatype.DataType;
import com.oceanbase.tools.dbbrowser.model.datatype.DataTypeFactory;
import com.oceanbase.tools.dbbrowser.model.datatype.JdbcDataTypeFactory;

import lombok.Getter;
import lombok.NonNull;

/**
 * {@link BaseDialectBasedRowMapper} based on {@link DialectType}
 *
 * @author yh263208
 * @date 2022-06-13 15:46
 * @since ODC_release_3.4.0
 */
public abstract class BaseDialectBasedRowMapper implements JdbcRowMapper {
    @Getter
    private final DialectType dialectType;

    public BaseDialectBasedRowMapper(@NonNull DialectType dialectType) {
        this.dialectType = dialectType;
    }

    abstract protected Collection<JdbcColumnMapper> getColumnDataMappers(@NonNull DialectType dialectType);

    @Override
    public List<Object> mapRow(@NonNull ResultSet resultSet) throws SQLException, IOException {
        Collection<JdbcColumnMapper> mappers = getColumnDataMappers(dialectType);
        if (mappers == null) {
            throw new NullPointerException("Mappers is null by " + dialectType);
        }
        ResultSetMetaData metaData = resultSet.getMetaData();
        int columnCount = metaData.getColumnCount();
        List<Object> line = new ArrayList<>(columnCount);
        for (int i = 0; i < columnCount; i++) {
            DataTypeFactory factory = new JdbcDataTypeFactory(metaData, i);
            DataType dataType = factory.generate();
            JdbcColumnMapper mapper = getColumnMapper(dataType, mappers);
            line.add(mapper.mapCell(new CellData(resultSet, i, dataType)));
        }
        return line;
    }

    private JdbcColumnMapper getColumnMapper(DataType dataType, Collection<JdbcColumnMapper> candidateMappers) {
        for (JdbcColumnMapper mapper : candidateMappers) {
            if (mapper.supports(dataType)) {
                return mapper;
            }
        }
        return new EmptyJdbcColumnMapper();
    }

    private static class EmptyJdbcColumnMapper implements JdbcColumnMapper {

        @Override
        public Object mapCell(@NonNull CellData cellData) throws SQLException {
            return cellData.getString();
        }

        @Override
        public boolean supports(@NonNull DataType dataType) {
            return false;
        }
    }

}
