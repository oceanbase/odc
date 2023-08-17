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

import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.metadb.connection.ConnectionLabelDAO;
import com.oceanbase.odc.metadb.connection.ConnectionLabelRelationRepository;
import com.oceanbase.odc.service.connection.model.ConnectionLabel;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
@SkipAuthorize("personal resource")
public class ConnectionLabelService {

    @Autowired
    private ConnectionLabelDAO connectionLabelDAO;
    @Autowired
    private AuthenticationFacade authenticationFacade;
    @Autowired
    private ConnectionLabelRelationRepository labelRelationRepository;

    public List<ConnectionLabel> list() {
        return this.connectionLabelDAO.list(authenticationFacade.currentUserId());
    }

    @Transactional(rollbackFor = Exception.class)
    public ConnectionLabel delete(long id) {
        ConnectionLabel connectionLabel = nullSafeGet(id);
        long affectedRows = this.connectionLabelDAO.delete(id);
        this.labelRelationRepository.deleteByLabelId(id);
        log.info("session label deleted, id={}, affectedRows={}, deleted={}",
                id, affectedRows, connectionLabel);
        return connectionLabel;
    }

    @Transactional(rollbackFor = Exception.class)
    public ConnectionLabel create(ConnectionLabel connectionLabel) {
        long userId = authenticationFacade.currentUserId();
        connectionLabel.setUserId(userId);
        String labelName = connectionLabel.getLabelName();
        try {
            long affectedRows = this.connectionLabelDAO.insert(connectionLabel);
            log.info("session label created, odcSessionLabel={}, affectedRows={}", connectionLabel, affectedRows);
        } catch (DuplicateKeyException duplicateExp) {
            PreConditions.validNoDuplicated(ResourceType.ODC_CONNECT_LABEL, "labelName", labelName, () -> true);
        }
        log.info("session label created, created={}", connectionLabel);
        return connectionLabel;
    }

    @Transactional(rollbackFor = Exception.class)
    public ConnectionLabel update(ConnectionLabel connectionLabel) {
        nullSafeGet(connectionLabel.getId());
        long affectedRows = this.connectionLabelDAO.update(connectionLabel);
        log.info("session label updated, odcSessionLabel={}, affectedRows={}", connectionLabel, affectedRows);
        return connectionLabel;
    }

    private ConnectionLabel nullSafeGet(long id) {
        ConnectionLabel connectionLabel = connectionLabelDAO.get(id);
        long currentUserId = authenticationFacade.currentUserId();
        PreConditions.validExists(ResourceType.ODC_CONNECT_LABEL, "id", id,
                () -> Objects.nonNull(connectionLabel) && currentUserId == connectionLabel.getUserId());
        return connectionLabel;
    }

}
