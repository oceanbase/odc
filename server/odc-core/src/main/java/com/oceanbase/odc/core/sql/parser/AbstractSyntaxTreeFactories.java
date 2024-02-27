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

package com.oceanbase.odc.core.sql.parser;

import com.oceanbase.odc.core.shared.constant.DialectType;

import lombok.NonNull;

/**
 * {@link AbstractSyntaxTreeFactories}
 *
 * @author yh263208
 * @date 2023-11-17 16:00
 * @since ODC_release_4.2.3
 */
public class AbstractSyntaxTreeFactories {

    public static AbstractSyntaxTreeFactory getAstFactory(@NonNull DialectType dialectType, long timeoutMillis) {
        if (dialectType.isOracle()) {
            return new OBOracleAstFactory(timeoutMillis);
        } else if (dialectType.isMysql()) {
            return new OBMySQLAstFactory(timeoutMillis);
        } else if (dialectType.isDoris()) {
            return new OBMySQLAstFactory(timeoutMillis);
        }
        return null;
    }

}
