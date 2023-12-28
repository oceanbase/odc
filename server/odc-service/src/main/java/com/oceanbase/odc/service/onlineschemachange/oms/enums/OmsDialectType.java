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
package com.oceanbase.odc.service.onlineschemachange.oms.enums;

import com.oceanbase.odc.core.shared.constant.DialectType;

/**
 * @author yaobin
 * @date 2023-12-27
 * @since 4.2.3
 */
public enum OmsDialectType {
    /**
     * 专有云,公有云集群实例
     */
    OB_MYSQL,
    /**
     * 专有云 ,公有云集群实例
     */
    OB_ORACLE;


    public static OmsDialectType from(DialectType dialectType) {
        switch (dialectType) {
            case OB_MYSQL:
                return OmsDialectType.OB_MYSQL;
            case OB_ORACLE:
                return OmsDialectType.OB_ORACLE;
            default:
                throw new UnsupportedOperationException(
                        String.format("Unsupported type %s to OmsDialectType", dialectType));
        }
    }

}
