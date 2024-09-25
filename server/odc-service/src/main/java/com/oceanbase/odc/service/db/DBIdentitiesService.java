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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.service.db.browser.DBSchemaAccessors;
import com.oceanbase.odc.service.db.model.SchemaIdentities;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;

@Service
@SkipAuthorize("inside connect session")
public class DBIdentitiesService {

    /**
     * 根据给定的连接会话和对象类型列表，获取对象标识列表
     *
     * @param session 连接会话
     * @param types 对象类型列表
     * @return 对象标识列表
     */
    public List<SchemaIdentities> list(ConnectionSession session, List<DBObjectType> types) {
        if (CollectionUtils.isEmpty(types)) {
            return Collections.emptyList();
        }
        DBSchemaAccessor schemaAccessor = DBSchemaAccessors.create(session);
        // 创建一个不区分大小写的TreeMap用于存储所有对象标识
        Map<String, SchemaIdentities> all = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        // 如果对象类型列表包含视图，则获取视图列表
        if (types.contains(DBObjectType.VIEW)) {
            listViews(schemaAccessor, all);
        }
        if (types.contains(DBObjectType.TABLE)) {
            listTables(schemaAccessor, all);
        }
        schemaAccessor.showDatabases().forEach(db -> all.computeIfAbsent(db, SchemaIdentities::of));
        // 返回所有对象标识的列表
        return new ArrayList<>(all.values());
    }

    void listTables(DBSchemaAccessor schemaAccessor, Map<String, SchemaIdentities> all) {
        schemaAccessor.listTables(null, null)
                .forEach(i -> all.computeIfAbsent(i.getSchemaName(), SchemaIdentities::of).add(i));
    }

    /**
     * 将数据库中的所有视图添加到给定的Map中
     *
     * @param schemaAccessor 数据库模式访问器
     * @param all 存储所有视图的Map，key为schema名称，value为对应的SchemaIdentities对象
     */
    void listViews(DBSchemaAccessor schemaAccessor, Map<String, SchemaIdentities> all) {
        // 获取所有用户视图并添加到all中
        schemaAccessor.listAllUserViews()
                .forEach(i -> all.computeIfAbsent(i.getSchemaName(), SchemaIdentities::of).add(i));
        // 获取所有系统视图并添加到all中
        schemaAccessor.listAllSystemViews()
                .forEach(i -> all.computeIfAbsent(i.getSchemaName(), SchemaIdentities::of).add(i));
    }

}
