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
package com.oceanbase.odc.service.ai.chat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.sql.execute.model.SqlTuple;
import com.oceanbase.odc.service.ai.chat.SqlCopilotService.DatabaseInfo;
import com.oceanbase.odc.service.ai.knowledgebase.utils.CopilotSchemaAccessor;
import com.oceanbase.odc.service.common.util.ResourceIDParser;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.db.browser.DBSchemaAccessors;
import com.oceanbase.odc.service.session.ConnectSessionService;
import com.oceanbase.odc.service.session.util.DBSchemaExtractor;
import com.oceanbase.odc.service.session.util.DBSchemaExtractor.DBSchemaIdentity;

import lombok.extern.slf4j.Slf4j;

/**
 * @author: liuyizhuo.lyz
 * @date: 2025/8/4
 */
@Component
@Slf4j
public class DatabaseCacheService {
    private final String regex = "(?i)\\bFROM\\b\\s+(\\w+)|\\bJOIN\\b\\s+(\\w+)";
    private final Pattern pattern = Pattern.compile(regex);
    @Autowired
    private DatabaseService databaseService;
    @Autowired
    private ConnectSessionService sessionService;

    private LoadingCache<Long, DatabaseInfo> databaseId2BasicInfoCache = Caffeine.newBuilder()
            .maximumSize(1000).expireAfterWrite(6, TimeUnit.HOURS)
            .build(this::getDatabaseInfoById);

    public DatabaseInfo get(Long databaseId) {
        return databaseId2BasicInfoCache.get(databaseId);
    }

    private DatabaseInfo getDatabaseInfoById(Long databaseId) {
        if (Objects.isNull(databaseId)) {
            return null;
        }
        Database database = databaseService.detail(databaseId);
        return new DatabaseInfo(database.getId(), database.getDialectType(), database.getName());
    }

    public ConnectionSession getSession(String sid, Long databaseId) {
        ConnectionSession session = null;
        if (StringUtils.isNotBlank(sid) && !"None".equalsIgnoreCase(sid)) {
            if (sid.startsWith("sid")) {
                sid = ResourceIDParser.parse(sid).getSid();
            }
            session = sessionService.nullSafeGet(sid, true);
        } else if (Objects.nonNull(databaseId)) {
            session = sessionService.create(null, databaseId);
        }
        return session;
    }

    public String getSplicedTableDDLText(Long databaseId, String sql, String sid) {
        ConnectionSession session = getSession(sid, databaseId);
        if (session == null) {
            return "";
        }
        try {
            DatabaseInfo databaseInfo = get(databaseId);
            PreConditions.notNull(databaseInfo, "databaseInfo");
            Set<String> tableNames = new HashSet<>(extractTableNamesFromSql(sql,
                    databaseInfo.getDialectType(), databaseInfo.getDatabaseName()));

            List<String> tableDDLs = CopilotSchemaAccessor.listTableDDLWithOptions(tableNames,
                    databaseInfo.getDatabaseName(), DBSchemaAccessors.create(session), true);
            StringBuilder ddlTextBuilder = new StringBuilder();
            for (String ddl : tableDDLs) {
                ddlTextBuilder.append(ddl).append('\n');
            }
            return ddlTextBuilder.toString();
        } finally {
            if (Objects.isNull(sid)) {
                session.expire();
            }
        }
    }

    public List<String> extractTableNamesFromSql(String sql, @NotNull DialectType dialectType, String databaseName) {
        List<String> tableNames = new ArrayList<>();
        try {
            tableNames = DBSchemaExtractor.listDBSchemasWithSqlTypes(
                    Collections.singletonList(SqlTuple.newTuple(sql)), dialectType, databaseName)
                    .keySet().stream()
                    .map(DBSchemaIdentity::getTable)
                    .filter(StringUtils::isNotBlank).collect(Collectors.toList());
            // if failed to extract table names by sql parser, try to extract table names by regex
            if (CollectionUtils.isEmpty(tableNames)) {
                Matcher matcher = pattern.matcher(sql);
                while (matcher.find()) {
                    if (matcher.group(1) != null) {
                        tableNames.add(matcher.group(1));
                    } else if (matcher.group(2) != null) {
                        tableNames.add(matcher.group(2));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to extract table names from sql, sql={}", sql, e);
        }
        return tableNames;
    }
}
