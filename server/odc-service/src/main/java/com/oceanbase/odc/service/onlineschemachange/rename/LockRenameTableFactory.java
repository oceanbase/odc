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

import java.util.function.Supplier;

import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.service.session.DBSessionManageFacade;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2023-08-02
 * @since 4.2.0
 */
@Slf4j
public class LockRenameTableFactory {

    public RenameTableInterceptor generate(ConnectionSession connectionSession,
            DBSessionManageFacade dbSessionManageFacade,
            Supplier<LockTableSupportDecider> lockTableSupportDeciderSupplier) {
        PreConditions.notNull(connectionSession, "connectionSession");

        DialectType dialectType = connectionSession.getDialectType();
        PreConditions.notNull(dialectType, "dialectType");
        String obVersion = ConnectionSessionUtil.getVersion(connectionSession);
        PreConditions.notNull(obVersion, "obVersion");
        return OscDBUserUtil.isLockUserRequired(dialectType, () -> obVersion, lockTableSupportDeciderSupplier)
                ? new LockUserInterceptor(connectionSession, dbSessionManageFacade)
                : new LockTableInterceptor(connectionSession);
    }

}

