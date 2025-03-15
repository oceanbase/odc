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
package com.oceanbase.odc.plugin.schema.api;

import java.sql.Connection;
import java.util.List;

import org.pf4j.ExtensionPoint;

import com.oceanbase.tools.dbbrowser.model.DBMViewRefreshParameter;
import com.oceanbase.tools.dbbrowser.model.DBMaterializedView;
import com.oceanbase.tools.dbbrowser.model.DBObjectIdentity;

/**
 * @description:
 * @author: zijia.cj
 * @date: 2025/3/4 14:41
 * @since: 4.3.4
 */
public interface MViewExtensionPoint extends ExtensionPoint {
    List<DBObjectIdentity> list(Connection connection, String schemaName);

    DBMaterializedView getDetail(Connection connection, String schemaName, String mViewName);

    void drop(Connection connection, String schemaName, String mViewName);

    String generateCreateTemplate(DBMaterializedView mView);

    Boolean refresh(Connection connection, DBMViewRefreshParameter parameter);

}
