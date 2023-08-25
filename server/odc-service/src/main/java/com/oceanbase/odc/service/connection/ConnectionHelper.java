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

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import com.oceanbase.odc.service.connection.model.ConnectionStringParseResult;
import com.oceanbase.odc.service.connection.model.GenerateConnectionStringReq;
import com.oceanbase.odc.service.connection.model.ParseConnectionStringReq;
import com.oceanbase.odc.service.connection.util.MySQLClientArgsParser;

/**
 * @author yizhou.xw
 * @version : ConnectionHelper.java, v 0.1 2021-07-23 17:53
 */
@Component
@Validated
public class ConnectionHelper {

    public ConnectionStringParseResult parseConnectionStr(@NotNull @Valid ParseConnectionStringReq req) {
        ConnectionStringParseResult result = MySQLClientArgsParser.parse2(req.getConnStr());
        result.setPassword(null);
        return result;
    }

    public String generateConnectionStr(@NotNull @Valid GenerateConnectionStringReq req) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("obclient -h")
                .append(req.getHost())
                .append(" -P")
                .append(req.getPort())
                .append(" -u");

        if (req.getUsername().startsWith("\"") && req.getUsername().endsWith("\"")) {
            stringBuilder.append("'");
        }
        stringBuilder.append(req.getUsername());
        if (StringUtils.isNotBlank(req.getTenantName())) {
            stringBuilder.append("@").append(req.getTenantName());
        }
        if (StringUtils.isNotBlank(req.getClusterName())) {
            stringBuilder.append("#").append(req.getClusterName());
        }
        if (req.getUsername().startsWith("\"") && req.getUsername().endsWith("\"")) {
            stringBuilder.append("'");
        }
        if (StringUtils.isNotBlank(req.getDefaultSchema())) {
            stringBuilder.append(" -D");
            if (req.getDefaultSchema().startsWith("\"") && req.getDefaultSchema().endsWith("\"")) {
                stringBuilder.append("'");
            }
            stringBuilder.append(req.getDefaultSchema());
            if (req.getDefaultSchema().startsWith("\"") && req.getDefaultSchema().endsWith("\"")) {
                stringBuilder.append("'");
            }
        }
        stringBuilder.append(" -p");
        return stringBuilder.toString();
    }

}
