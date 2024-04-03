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
package com.oceanbase.odc.plugin.task.obmysql.partitionplan.mapper;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.oceanbase.odc.core.sql.execute.mapper.CellData;
import com.oceanbase.tools.dbbrowser.model.datatype.DataType;

import lombok.NonNull;

/**
 * {@link CellDataProcessors}
 *
 * @author yh263208
 * @date 2024-01-24 21:21
 * @since ODC_release_4.2.4
 */
public class CellDataProcessors {

    private final static List<CellDataProcessor> PROCESSORS = new ArrayList<>();

    static {
        PROCESSORS.add(new OBMySQLTimeDataTypeProcessor());
    }

    public static CellDataProcessor getByDataType(@NonNull DataType dataType) {
        return PROCESSORS.stream().filter(p -> p.supports(dataType)).findFirst().orElse(new EmptyCellDataProcessor());
    }

    static public class EmptyCellDataProcessor implements CellDataProcessor {

        @Override
        public Object mapCell(@NonNull CellData cellData) throws SQLException {
            return cellData.getObject();
        }

        @Override
        public String convertToSqlLiteral(Object target, @NonNull DataType dataType) {
            if (target == null) {
                return "NULL";
            }
            return target.toString();
        }

        @Override
        public boolean supports(@NonNull DataType dataType) {
            return true;
        }
    }

}
