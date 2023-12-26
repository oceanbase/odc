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
package com.oceanbase.odc.service.schedule.flowtask;

import java.util.List;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.core.sql.parser.AbstractSyntaxTreeFactories;
import com.oceanbase.odc.core.sql.parser.AbstractSyntaxTreeFactory;
import com.oceanbase.odc.service.common.util.SqlUtils;
import com.oceanbase.odc.service.connection.ConnectionService;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.flow.model.CreateFlowInstanceReq;
import com.oceanbase.odc.service.flow.processor.FlowTaskPreprocessor;
import com.oceanbase.odc.service.flow.processor.Preprocessor;
import com.oceanbase.odc.service.resultset.ResultSetExportTaskParameter;
import com.oceanbase.tools.dbbrowser.parser.ParserUtil;
import com.oceanbase.tools.dbbrowser.parser.constant.GeneralSqlType;
import com.oceanbase.tools.dbbrowser.parser.result.BasicResult;

@FlowTaskPreprocessor(type = TaskType.EXPORT_RESULT_SET)
public class ResultSetExportPreprocessor implements Preprocessor {

    private final static Pattern FILE_NAME_PATTERN = Pattern.compile("^[\\u4e00-\\u9fa5_a-zA-Z0-9._ -]+$");
    public static final String DEFAULT_DELIMITER = ";";

    @Autowired
    private ConnectionService connectionService;

    @Override
    public void process(CreateFlowInstanceReq req) {
        ResultSetExportTaskParameter parameter = (ResultSetExportTaskParameter) req.getParameters();
        ConnectionConfig connectionConfig =
                connectionService.getForConnectionSkipPermissionCheck(req.getConnectionId());

        preCheckSql(parameter, connectionConfig.getDialectType());

        PreConditions.validArgumentState(FILE_NAME_PATTERN.matcher(parameter.getFileName()).matches(),
                ErrorCodes.BadArgument, null,
                "File name must contain only letters, numbers, Chinese characters and \"._-\" ");
    }

    private void preCheckSql(ResultSetExportTaskParameter parameter, DialectType dialectType) {
        String sql = parameter.getSql();
        /*
         * verify single query
         */
        List<String> sqls = SqlUtils.split(dialectType, sql, DEFAULT_DELIMITER);
        PreConditions.validSingleton(sqls, "query sql");
        /*
         * verify sql type
         */
        AbstractSyntaxTreeFactory factory = AbstractSyntaxTreeFactories.getAstFactory(dialectType, 0);
        if (factory == null) {
            throw new UnsupportedOperationException("Unsupported dialect type, " + dialectType);
        }
        BasicResult r = factory.buildAst(sql).getParseResult();
        Verify.verify(ParserUtil.getGeneralSqlType(r) == GeneralSqlType.DQL, "Invalid sql type, query must be DQL!");

        parameter.setSql(sqls.get(0));
    }

}
