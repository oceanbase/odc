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

import java.util.HashMap;
import java.util.Map;

import com.oceanbase.odc.plugin.task.api.partitionplan.datatype.TimeDataType;
import com.oceanbase.tools.dbbrowser.model.datatype.CommonDataTypeFactory;
import com.oceanbase.tools.dbbrowser.model.datatype.DataType;

import lombok.NonNull;

/**
 * {@link OBOracleCommonDataTypeFactory}
 *
 * @author yh263208
 * @date 2024-01-26 13:51
 * @since ODC_release_4.2.4
 * @see CommonDataTypeFactory
 */
public class OBOracleCommonDataTypeFactory extends CommonDataTypeFactory {

    private static final Map<String, Integer> TYPE_NAME_2_PREC = new HashMap<>();

    static {
        /**
         * {@code timestamp with time zone} and {@code timestamp with local time zone} is not supported.
         * ref. https://www.oceanbase.com/docs/common-oceanbase-database-cn-1000000000220162
         */
        TYPE_NAME_2_PREC.put("date", TimeDataType.SECOND);
        TYPE_NAME_2_PREC.put("timestamp", TimeDataType.SECOND);
    }

    public OBOracleCommonDataTypeFactory(@NonNull String dataType) {
        super(dataType);
    }

    @Override
    public DataType generate() {
        DataType dataType = super.generate();
        Integer prec = TYPE_NAME_2_PREC.get(dataType.getDataTypeName().toLowerCase());
        return prec == null ? dataType : new TimeDataType(dataType.getDataTypeName(), prec);
    }

}
