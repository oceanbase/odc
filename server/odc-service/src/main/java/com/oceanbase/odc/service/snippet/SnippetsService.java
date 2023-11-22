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
package com.oceanbase.odc.service.snippet;

import java.util.List;
import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.metadb.snippet.SnippetsDAO;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
@Validated
@SkipAuthorize("personal resource")
public class SnippetsService {

    @Autowired
    private SnippetsDAO snippetsDAO;
    @Autowired
    private AuthenticationFacade authenticationFacade;

    public List<Snippet> list() {
        long userId = authenticationFacade.currentUserId();
        return this.snippetsDAO.list(userId);
    }

    @Transactional(rollbackFor = Exception.class)
    public Snippet create(@NotNull @Valid Snippet snippet) {
        long userId = authenticationFacade.currentUserId();
        snippet.setUserId(userId);
        PreConditions.validNoDuplicated(ResourceType.ODC_SNIPPET, "prefix",
                snippet.getPrefix(), () -> snippetsDAO.queryByUserIdAndName(snippet) != null);
        snippetsDAO.insert(snippet);
        Snippet created = this.snippetsDAO.queryByUserIdAndName(snippet);
        log.info("snippets created, snippetId={}", created.getId());
        return created;
    }

    @Transactional(rollbackFor = Exception.class)
    public Snippet update(@NotNull @Valid Snippet snippet) {
        nullSafeGet(snippet.getId());
        Snippet tmpSnippet = this.snippetsDAO.queryByUserIdAndName(snippet);
        PreConditions.validNoDuplicated(ResourceType.ODC_SNIPPET, "prefix",
                snippet.getPrefix(), () -> null != tmpSnippet && tmpSnippet.getId() != snippet.getId());
        this.snippetsDAO.update(snippet);
        log.info("Snippets updated, snippetId={}", snippet.getId());
        return snippet;
    }

    @Transactional(rollbackFor = Exception.class)
    public Snippet delete(long id) {
        Snippet snippet = nullSafeGet(id);
        long rows = snippetsDAO.delete(id);
        log.info("Snippets deleted, id={}, affectRows={}", id, rows);
        return snippet;
    }

    private Snippet nullSafeGet(long id) {
        Snippet snippet = snippetsDAO.get(id);
        long currentUserId = authenticationFacade.currentUserId();
        PreConditions.validExists(ResourceType.ODC_SNIPPET, "id", id,
                () -> Objects.nonNull(snippet) && currentUserId == snippet.getUserId());
        return snippet;
    }

}
