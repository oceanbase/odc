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
package com.oceanbase.odc.service.db;

import org.springframework.stereotype.Service;

import com.oceanbase.odc.core.shared.exception.NotImplementedException;
import com.oceanbase.odc.service.db.model.QueryDBObjectParams;
import com.oceanbase.odc.service.db.model.QueryDBObjectResp;
import com.oceanbase.odc.service.db.model.SyncDBObjectReq;

import lombok.NonNull;

/**
 * @author gaoda.xy
 * @date 2024/3/28 13:41
 */
@Service
public class DBObjectIndexService {

    public QueryDBObjectResp listDatabaseObjects(@NonNull QueryDBObjectParams params) {
        throw new NotImplementedException();
    }

    public Boolean syncDatabaseObjects(@NonNull SyncDBObjectReq req) {
        throw new NotImplementedException();
    }

}
