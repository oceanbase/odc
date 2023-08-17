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

import com.oceanbase.tools.dbbrowser.model.DBFunction;
import com.oceanbase.tools.dbbrowser.model.DBPLObjectIdentity;

/**
 * {@link FunctionExtensionPoint}
 *
 * @author jingtian
 * @date 2023/6/14
 * @since 4.2.0
 */
public interface FunctionExtensionPoint extends ExtensionPoint {

    List<DBPLObjectIdentity> list(Connection connection, String schemaName);

    DBFunction getDetail(Connection connection, String schemaName, String functionName);

    void drop(Connection connection, String schemaName, String functionName);

    String generateCreateTemplate(DBFunction function);
}
