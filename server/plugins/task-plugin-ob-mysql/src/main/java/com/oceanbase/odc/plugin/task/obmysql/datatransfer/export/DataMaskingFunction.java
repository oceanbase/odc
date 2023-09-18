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
package com.oceanbase.odc.plugin.task.obmysql.datatransfer.export;

import com.oceanbase.odc.core.datamasking.masker.AbstractDataMasker;
import com.oceanbase.odc.core.datamasking.masker.ValueMeta;
import com.oceanbase.tools.loaddump.function.AbstractUserDefinedFunction;
import com.oceanbase.tools.loaddump.function.enums.SqlFunctions;

/**
 * @author wenniu.ly
 * @date 2022/8/31
 */

public class DataMaskingFunction extends AbstractUserDefinedFunction {
    private AbstractDataMasker masker;

    public DataMaskingFunction(AbstractDataMasker masker) {
        this.masker = masker;
    }

    @Override
    public String getName() {
        return SqlFunctions.ODC_DATA_MASKING_FUNCTION.name();
    }

    @Override
    public String invoke(String... strings) {
        String columnName = String.valueOf(params.get(AbstractUserDefinedFunction.COLUMN_NAME_KEY));
        String dataType = String.valueOf(params.get(AbstractUserDefinedFunction.DATA_TYPE_KEY));
        ValueMeta valueMeta = new ValueMeta(dataType, columnName);
        if (strings == null || strings[0] == null) {
            return null;
        }
        return masker.mask(strings[0], valueMeta);
    }
}
