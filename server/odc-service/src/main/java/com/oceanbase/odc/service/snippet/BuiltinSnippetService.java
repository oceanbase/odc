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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;
import com.oceanbase.odc.common.util.ResourceUtils;
import com.oceanbase.odc.common.util.ResourceUtils.ResourceInfo;
import com.oceanbase.odc.common.util.YamlUtils;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.service.session.ConnectSessionService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class BuiltinSnippetService {

    private static final String BUILTIN_SNIPPET_DIR = "builtin-snippet";
    private final List<BuiltinSnippet> builtinSnippets = new ArrayList<>();

    @Autowired
    private ConnectSessionService sessionService;

    @PostConstruct
    public void init() {
        List<ResourceInfo> resourceInfos = ResourceUtils.listResourcesFromDirectory(BUILTIN_SNIPPET_DIR).stream()
                .filter(r -> r.getResourceName().endsWith(".yml"))
                .collect(Collectors.toList());
        for (ResourceInfo resourceInfo : resourceInfos) {
            String fileName = BUILTIN_SNIPPET_DIR + "/" + resourceInfo.getResourceName();
            List<BuiltinSnippet> snippets = YamlUtils.fromYamlList(fileName, BuiltinSnippet.class);
            builtinSnippets.addAll(snippets);
        }
        log.info("Found {} builtin snippet files, {} builtin snippets loaded",
                resourceInfos.size(), builtinSnippets.size());
    }

    public List<BuiltinSnippet> listAll() {
        return builtinSnippets.stream().map(BuiltinSnippet::copy).collect(Collectors.toList());
    }

    public List<BuiltinSnippet> listBySession(String sessionId) {
        ConnectionSession connectionSession = sessionService.nullSafeGet(sessionId, true);
        String tenantName = ConnectionSessionUtil.getTenantName(connectionSession);
        boolean isSysTenant = "sys".equals(tenantName);
        String version = ConnectionSessionUtil.getVersion(connectionSession);
        Set<DialectType> supportsDialectTypes = supportsDialectTypes(connectionSession.getConnectType());
        return listAll().stream()
                .filter(snippet -> supportsDialectTypes.contains(snippet.getDialectType()))
                .filter(snippet -> !snippet.isForSysTenantOnly() || isSysTenant)
                .filter(snippet -> Objects.isNull(snippet.getMinVersion())
                        || snippet.getMinVersion().compareTo(version) <= 0)
                .filter(snippet -> Objects.isNull(snippet.getMaxVersion())
                        || snippet.getMaxVersion().compareTo(version) >= 0)
                .collect(Collectors.toList());
    }

    List<BuiltinSnippet> listByConnectType(ConnectType connectType) {
        Set<DialectType> dialectTypes = supportsDialectTypes(connectType);
        return listAll().stream()
                .filter(snippet -> dialectTypes.contains(snippet.getDialectType()))
                .collect(Collectors.toList());
    }

    public Set<DialectType> supportsDialectTypes(ConnectType connectType) {
        switch (connectType) {
            case OB_ORACLE:
            case CLOUD_OB_ORACLE:
                return Sets.newHashSet(DialectType.ORACLE, DialectType.OB_ORACLE);
            case OB_MYSQL:
            case CLOUD_OB_MYSQL:
            case ODP_SHARDING_OB_MYSQL:
                return Sets.newHashSet(DialectType.MYSQL, DialectType.OB_MYSQL);
            case DORIS:
                return Sets.newHashSet(DialectType.DORIS, DialectType.MYSQL);
            default:
                return Sets.newHashSet(connectType.getDialectType());
        }
    }
}
