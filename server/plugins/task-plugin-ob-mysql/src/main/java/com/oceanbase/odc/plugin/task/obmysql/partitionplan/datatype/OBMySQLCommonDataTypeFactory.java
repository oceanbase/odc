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
package com.oceanbase.odc.plugin.task.obmysql.partitionplan.datatype;

import java.util.HashMap;
import java.util.Map;

import com.oceanbase.odc.plugin.task.api.partitionplan.datatype.TimeDataType;
import com.oceanbase.tools.dbbrowser.model.datatype.CommonDataTypeFactory;
import com.oceanbase.tools.dbbrowser.model.datatype.DataType;

import lombok.NonNull;

/**
 * {@link OBMySQLCommonDataTypeFactory}
 *
 * @author yh263208
 * @date 2024-01-24 20:43
 * @since ODC_release_4.2.4
 */
public class OBMySQLCommonDataTypeFactory extends CommonDataTypeFactory {

    private static final Map<String, Integer> TYPE_NAME_2_PREC = new HashMap<>();

    static {
        TYPE_NAME_2_PREC.put("date", TimeDataType.DAY);
        TYPE_NAME_2_PREC.put("datetime", TimeDataType.SECOND);
        TYPE_NAME_2_PREC.put("timestamp", TimeDataType.SECOND);
    }

    public OBMySQLCommonDataTypeFactory(@NonNull String dataType) {
        super(dataType);
    }

    @Override
    public DataType generate() {
        DataType dataType = super.generate();
        Integer prec = TYPE_NAME_2_PREC.get(dataType.getDataTypeName().toLowerCase());
        return prec == null ? dataType : new TimeDataType(prec);
    }

}
