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

package com.oceanbase.odc.service.onlineschemachange.rename;

import java.util.Set;
import java.util.function.Supplier;

import com.google.common.collect.Sets;
import com.oceanbase.odc.common.util.VersionUtils;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.shared.exception.UnsupportedException;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;

/**
 * @author yaobin
 * @date 2023-10-16
 * @since 4.2.3
 */
public class OscDBUserUtil {

    public static boolean isLockUserRequired(DialectType dialectType, Supplier<String> obVersion,
            Supplier<LockTableSupportDecider> lockTableSupportDeciderSupplier) {
        String version = obVersion.get();
        if (dialectType.isOBMysql()) {
            // version is null, or version less than 4.2.5
            return !(version != null && lockTableSupportDeciderSupplier.get().supportLockTable(version));
        } else if (dialectType == DialectType.OB_ORACLE) {
            return version != null && !VersionUtils.isGreaterThanOrEqualsTo(version, "4.0.0");
        } else {
            throw new UnsupportedException(String.format("Dialect '%s' not supported", dialectType));
        }
    }

    public static Set<String> getLockUserWhiteList(ConnectionConfig config) {
        Set<String> users = Sets.newHashSet(config.getUsername(), "__oceanbase_inner_drc_user");
        if (config.getDialectType().isMysql()) {
            users.add("root");
        } else {
            users.add("SYS");
            users.add("SYSTEM");
            users.add("PUBLIC");
            users.add("LBACSYS");
            users.add("ORAAUDITOR");
            users.add("ROOT");
            users.add(config.getUsername().toUpperCase());
        }
        return users;
    }

}
