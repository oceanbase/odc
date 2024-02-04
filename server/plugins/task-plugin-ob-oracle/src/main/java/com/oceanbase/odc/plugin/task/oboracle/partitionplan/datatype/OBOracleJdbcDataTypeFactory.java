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
package com.oceanbase.odc.plugin.task.oboracle.partitionplan.datatype;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import com.oceanbase.tools.dbbrowser.model.datatype.DataType;
import com.oceanbase.tools.dbbrowser.model.datatype.JdbcDataTypeFactory;

import lombok.NonNull;

/**
 * {@link OBOracleJdbcDataTypeFactory}
 *
 * @author yh263208
 * @date 2024-01-26 14:02
 * @since ODC_release_4.2.4
 */
public class OBOracleJdbcDataTypeFactory extends JdbcDataTypeFactory {

    public OBOracleJdbcDataTypeFactory(@NonNull ResultSetMetaData metaData, int columnIndex) {
        super(metaData, columnIndex);
    }

    @Override
    public DataType generate() throws SQLException {
        DataType dataType = super.generate();
        OBOracleCommonDataTypeFactory factory = new OBOracleCommonDataTypeFactory(dataType.getDataTypeName());
        return factory.generate();
    }

}
