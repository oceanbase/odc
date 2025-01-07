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
    /**
     * 数据库方言类型
     */
    private final DialectType dialectType;

    public BaseDialectBasedRowMapper(@NonNull DialectType dialectType) {
        this.dialectType = dialectType;
    }

    abstract protected Collection<JdbcColumnMapper> getColumnDataMappers(@NonNull DialectType dialectType);

    @Override
    public List<Object> mapRow(@NonNull ResultSet resultSet) throws SQLException, IOException {
        // 获取列数据映射器集合
        Collection<JdbcColumnMapper> mappers = getColumnDataMappers(dialectType);
        if (mappers == null) {
            throw new NullPointerException("Mappers is null by " + dialectType);
        }
        // 获取结果集元数据
        ResultSetMetaData metaData = resultSet.getMetaData();
        // 获取列数
        int columnCount = metaData.getColumnCount();
        // 创建List<Object>对象
        List<Object> line = new ArrayList<>(columnCount);
        // 遍历每一列
        for (int i = 0; i < columnCount; i++) {
            // 创建数据类型工厂对象
            DataTypeFactory factory = new JdbcDataTypeFactory(metaData, i);
            // 生成数据类型
            DataType dataType = factory.generate();
            // 获取列数据映射器
            JdbcColumnMapper mapper = getColumnMapper(dataType, mappers);
            // 将列数据映射为Object并添加到line中
            line.add(mapper.mapCell(new CellData(resultSet, i, dataType)));
        }
        return line;
    }

    /**
     * 获取列数据映射器
     *
     * @param dataType         数据类型
     * @param candidateMappers 候选的列数据映射器集合
     * @return 列数据映射器
     */
    private JdbcColumnMapper getColumnMapper(DataType dataType, Collection<JdbcColumnMapper> candidateMappers) {
        for (JdbcColumnMapper mapper : candidateMappers) {
            if (mapper.supports(dataType)) {
                return mapper;
            }
        }
        return new EmptyJdbcColumnMapper();
    }

    /**
     * 空的列数据映射器
     */
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
