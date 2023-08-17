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

import com.oceanbase.odc.core.shared.constant.ConnectType;

/**
 * @author yaobin
 * @date 2023-06-01
 * @since 4.2.0
 */
public enum OmsOceanBaseType {
    /**
     * 专有云,公有云集群实例
     */
    OB_MYSQL,
    /**
     * 专有云 ,公有云集群实例
     */
    OB_ORACLE,
    /**
     * 公有云 MySQL 租户实例
     */
    OB_MYSQL_TENANT,
    /**
     * 公有云 ORACLE 租户实例
     */
    OB_ORACLE_TENANT,
    /**
     * 公有云 VPC MySQL 租户实例
     */
    OB_MYSQL_VPC,
    /**
     * 公有云 VPC ORACLE 租户实例
     */
    OB_ORACLE_VPC;

    public static OmsOceanBaseType from(ConnectType connectType) {

        switch (connectType) {
            case OB_MYSQL:
                return OmsOceanBaseType.OB_MYSQL;
            case OB_ORACLE:
                return OmsOceanBaseType.OB_ORACLE;
            case CLOUD_OB_MYSQL:
                return OmsOceanBaseType.OB_MYSQL_TENANT;
            case CLOUD_OB_ORACLE:
                return OmsOceanBaseType.OB_ORACLE_TENANT;
            default:
                throw new UnsupportedOperationException(
                        String.format("Unsupported type %s to OmsOceanBaseType", connectType));
        }
    }

}
