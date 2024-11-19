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
package com.oceanbase.odc.service.connection;

import java.util.Date;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.metadb.connection.ConnectionSyncHistoryEntity;
import com.oceanbase.odc.metadb.connection.ConnectionSyncHistoryRepository;
import com.oceanbase.odc.service.connection.model.ConnectionSyncErrorReason;
import com.oceanbase.odc.service.connection.model.ConnectionSyncResult;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2024/11/6 16:14
 * @Description: []
 */
@Slf4j
@Service
@SkipAuthorize
public class ConnectionSyncHistoryService {
    @Autowired
    private ConnectionSyncHistoryRepository connectionSyncHistoryRepository;

    @Transactional(rollbackFor = Exception.class)
    public void upsert(@NonNull Long connectionId, @NonNull ConnectionSyncResult syncResult,
            @NonNull Long organizationId,
            ConnectionSyncErrorReason errorReason, String errorMessage) {
        Optional<ConnectionSyncHistoryEntity> historyEntityOpt =
                connectionSyncHistoryRepository.findByConnectionId(connectionId);
        ConnectionSyncHistoryEntity historyEntity;
        if (historyEntityOpt.isPresent()) {
            historyEntity = historyEntityOpt.get();
        } else {
            historyEntity = new ConnectionSyncHistoryEntity();
            historyEntity.setConnectionId(connectionId);
            historyEntity.setOrganizationId(organizationId);
        }
        historyEntity.setLastSyncResult(syncResult);
        historyEntity.setLastSyncTime(new Date(System.currentTimeMillis()));
        historyEntity.setLastSyncErrorReason(errorReason);
        historyEntity.setLastSyncErrorMessage(errorMessage);
        connectionSyncHistoryRepository.save(historyEntity);
    }

}
