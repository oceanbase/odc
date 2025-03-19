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
package com.oceanbase.tools.dbbrowser.schema.mysql;

import java.util.List;

import org.springframework.jdbc.core.JdbcOperations;

import com.oceanbase.tools.dbbrowser.model.DBMViewRefreshParameter;
import com.oceanbase.tools.dbbrowser.model.DBMaterializedView;
import com.oceanbase.tools.dbbrowser.model.DBObjectIdentity;
import com.oceanbase.tools.dbbrowser.model.DBTableConstraint;

/**
 * @description: applicable to OB [4.3.2,4.3.5.1)
 * @author: zijia.cj
 * @date: 2025/3/4 19:00
 * @since: 4.3.4
 */
public class OBMySQLBetween432And4351SchemaAccessor extends OBMySQLSchemaAccessor {

    public OBMySQLBetween432And4351SchemaAccessor(JdbcOperations jdbcOperations) {
        super(jdbcOperations);
    }

    @Override
    public List<DBObjectIdentity> listMViews(String schemaName) {
        throw new UnsupportedOperationException("not support yet");
    }

    @Override
    public List<DBObjectIdentity> listAllMViewsLike(String viewNameLike) {
        throw new UnsupportedOperationException("not support yet");
    }

    @Override
    public Boolean refreshMVData(DBMViewRefreshParameter parameter) {
        throw new UnsupportedOperationException("not support yet");
    }

    @Override
    public DBMaterializedView getMView(String schemaName, String mViewName) {
        throw new UnsupportedOperationException("not support yet");
    }

    @Override
    public List<DBTableConstraint> listMViewConstraints(String schemaName, String mViewName) {
        throw new UnsupportedOperationException("not support yet");
    }

}
