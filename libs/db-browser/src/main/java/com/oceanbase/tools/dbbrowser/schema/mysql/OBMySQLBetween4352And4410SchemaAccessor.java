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

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

import org.springframework.jdbc.core.JdbcOperations;

import com.oceanbase.tools.dbbrowser.model.DBExternalResource;
import com.oceanbase.tools.dbbrowser.model.DBExternalResourceStream;
import com.oceanbase.tools.dbbrowser.model.DBExternalResourceUploadParam;
import com.oceanbase.tools.dbbrowser.model.DBObjectIdentity;

/**
 * @description: applicable to OB [4.3.5.2,4.4.1.0)
 * @author: zijia.cj
 * @date: 2025/8/21 15:17
 * @since: 4.4.0
 */
public class OBMySQLBetween4352And4410SchemaAccessor extends OBMySQLSchemaAccessor {

    public OBMySQLBetween4352And4410SchemaAccessor(JdbcOperations jdbcOperations) {
        super(jdbcOperations);
    }

    @Override
    public List<DBObjectIdentity> listExternalResources(String schemaName) {
        throw new UnsupportedOperationException("not support yet");
    }

    @Override
    public DBExternalResource getExternalResource(String schemaName, String name, Charset charset) throws IOException {
        throw new UnsupportedOperationException("not support yet");
    }

    @Override
    public Boolean deleteExternalResource(String schemaName, String name) {
        throw new UnsupportedOperationException("not support yet");
    }

    @Override
    public Boolean uploadExternalResource(DBExternalResourceUploadParam param) throws IOException {
        throw new UnsupportedOperationException("not support yet");
    }

    @Override
    public DBExternalResourceStream downloadExternalResource(String schemaName, String name, Charset charset)
            throws IOException {
        throw new UnsupportedOperationException("not support yet");
    }

}
