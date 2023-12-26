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

package com.oceanbase.odc.service.onlineschemachange.ddl;

import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.exception.UnsupportedException;

/**
 * @author yaobin
 * @date 2023-08-31
 * @since 4.2.0
 */
public class OscFactoryWrapperGenerator {

    public static OscFactoryWrapper generate(DialectType dialectType) {

        OscFactoryWrapper oscFactoryWrapper = new OscFactoryWrapper();
        if (dialectType.isMysql()) {
            oscFactoryWrapper.setTableNameDescriptorFactory(new OBMySqlTableNameDescriptorFactory());
        } else if (dialectType.isOracle()) {
            oscFactoryWrapper.setTableNameDescriptorFactory(new OBOracleTableNameDescriptorFactory());
        } else {
            throw new UnsupportedException(ErrorCodes.Unsupported, new Object[] {dialectType.name()},
                    "unsupported dialect type");
        }
        oscFactoryWrapper.setOscDBAccessorFactory(new OscDBAccessorFactory());
        return oscFactoryWrapper;
    }
}
