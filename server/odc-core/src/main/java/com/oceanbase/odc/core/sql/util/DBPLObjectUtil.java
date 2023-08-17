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
package com.oceanbase.odc.core.sql.util;

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;

import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.exception.BadArgumentException;
import com.oceanbase.odc.core.shared.exception.UnsupportedException;
import com.oceanbase.tools.dbbrowser.model.DBBasicPLObject;
import com.oceanbase.tools.dbbrowser.model.DBPLParam;
import com.oceanbase.tools.dbbrowser.model.DBPLParamMode;

import lombok.NonNull;

/**
 * {@link DBPLObjectUtil}
 *
 * @author yh263208
 * @date 2023-03-02 14:57
 * @since ODC_release_4.1.2
 */
public class DBPLObjectUtil {

    public static void checkParams(@NonNull DBBasicPLObject plObject) {
        if (CollectionUtils.isEmpty(plObject.getParams())) {
            return;
        }
        List<DBPLParam> params = plObject.getParams();
        for (DBPLParam p : params) {
            if (JdbcDataTypeUtil.validateInParameter(p)) {
                continue;
            }
            String errMsg = String.format("Param value={%s} and param type={%s} is not matched",
                    p.getDefaultValue(), p.getDataType());
            throw new BadArgumentException(ErrorCodes.IllegalArgument, new Object[] {p.getParamName(), errMsg}, errMsg);
        }
    }

    public static String getMySQLParamString(DBPLParamMode type) {
        if (DBPLParamMode.INOUT == type) {
            return "inout";
        } else if (DBPLParamMode.IN == type) {
            return "in";
        } else if (DBPLParamMode.OUT == type) {
            return "out";
        }
        throw new UnsupportedException(String.format("Unsupported param type '%s'", type));
    }

    public static String getOracleParamString(DBPLParamMode type) {
        if (DBPLParamMode.INOUT == type) {
            return "in out";
        } else if (DBPLParamMode.IN == type) {
            return "in";
        } else if (DBPLParamMode.OUT == type) {
            return "out";
        }
        throw new UnsupportedException(String.format("Unsupported param type '%s'", type));
    }

}
