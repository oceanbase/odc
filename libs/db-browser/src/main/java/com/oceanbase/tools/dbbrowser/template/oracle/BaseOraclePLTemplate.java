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
package com.oceanbase.tools.dbbrowser.template.oracle;

import com.oceanbase.tools.dbbrowser.model.DBObject;
import com.oceanbase.tools.dbbrowser.model.DBPLParamMode;
import com.oceanbase.tools.dbbrowser.model.PLConstants;
import com.oceanbase.tools.dbbrowser.template.BasePLTemplate;

/**
 * {@link BaseOraclePLTemplate}
 *
 * @author yh263208
 * @date 2023-02-22 15:54
 * @since db-browser_1.0.0-SNAPSHOT
 */
public abstract class BaseOraclePLTemplate<T extends DBObject> extends BasePLTemplate<T> {

    @Override
    protected String generateInOutString(DBPLParamMode type) {
        if (DBPLParamMode.INOUT == type) {
            return PLConstants.OB_ORACLE_PL_INOUT_PARAM;
        } else if (DBPLParamMode.IN == type) {
            return PLConstants.PL_IN_PARAM;
        } else if (DBPLParamMode.OUT == type) {
            return PLConstants.PL_OUT_PARAM;
        }
        throw new IllegalArgumentException("Illegal parameter type, " + type);
    }

}
