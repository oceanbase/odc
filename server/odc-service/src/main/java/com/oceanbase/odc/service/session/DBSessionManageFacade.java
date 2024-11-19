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
package com.oceanbase.odc.service.session;

import java.util.List;
import java.util.function.Predicate;

import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.shared.model.OdcDBSession;
import com.oceanbase.odc.service.db.session.KillSessionOrQueryReq;
import com.oceanbase.odc.service.db.session.KillSessionResult;

public interface DBSessionManageFacade {

    List<KillSessionResult> killSessionOrQuery(KillSessionOrQueryReq request);

    boolean supportKillConsoleQuery(ConnectionSession session);

    boolean killConsoleQuery(ConnectionSession session);

    void killAllSessions(ConnectionSession connectionSession,
            Predicate<OdcDBSession> filter, Integer lockTableTimeOutSeconds);

}
