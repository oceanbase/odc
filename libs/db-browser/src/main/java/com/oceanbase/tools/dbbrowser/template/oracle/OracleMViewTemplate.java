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
package com.oceanbase.tools.dbbrowser.template.oracle;

import static com.oceanbase.tools.dbbrowser.model.DBConstraintType.PRIMARY_KEY;

import java.text.SimpleDateFormat;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import com.oceanbase.tools.dbbrowser.editor.oracle.OracleConstraintEditor;
import com.oceanbase.tools.dbbrowser.editor.oracle.OracleDBTablePartitionEditor;
import com.oceanbase.tools.dbbrowser.template.BaseMViewTemplate;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import com.oceanbase.tools.dbbrowser.editor.DBTableConstraintEditor;
import com.oceanbase.tools.dbbrowser.editor.DBTablePartitionEditor;
import com.oceanbase.tools.dbbrowser.editor.mysql.MySQLConstraintEditor;
import com.oceanbase.tools.dbbrowser.editor.mysql.OBMySQLDBTablePartitionEditor;
import com.oceanbase.tools.dbbrowser.model.DBColumnGroupElement;
import com.oceanbase.tools.dbbrowser.model.DBMaterializedView;
import com.oceanbase.tools.dbbrowser.model.DBMaterializedViewRefreshSchedule;
import com.oceanbase.tools.dbbrowser.model.DBTableConstraint;
import com.oceanbase.tools.dbbrowser.model.DBView;
import com.oceanbase.tools.dbbrowser.template.DBObjectTemplate;
import com.oceanbase.tools.dbbrowser.util.OracleSqlBuilder;
import com.oceanbase.tools.dbbrowser.util.SqlBuilder;

/**
 * @description:
 * @author: zijia.cj
 * @date: 2025/3/11 20:50
 * @since: 4.3.4
 */
public class OracleMViewTemplate extends BaseMViewTemplate {

    public OracleMViewTemplate() {
        super(new OracleViewTemplate(), new OracleConstraintEditor(), new OracleDBTablePartitionEditor());
    }

    @Override
    protected SqlBuilder sqlBuilder() {
        return new OracleSqlBuilder();
    }

    @Override
    protected void fillRefreshSchedule(DBMaterializedView dbMView, SqlBuilder sqlBuilder) {
        if (Objects.nonNull(dbMView.getRefreshSchedule())) {
            DBMaterializedViewRefreshSchedule refreshSchedule = dbMView.getRefreshSchedule();
            if(refreshSchedule.getUnit()== DBMaterializedViewRefreshSchedule.TimeUnit.WEEK){
                throw new IllegalArgumentException("Oracle does not support refresh schedule by week");
            }
            if (refreshSchedule.getStartStrategy() == DBMaterializedViewRefreshSchedule.StartStrategy.START_NOW) {
                sqlBuilder.line().append("START WITH CURRENT_DATE");
                sqlBuilder.line().append("NEXT CURRENT_DATE + INTERVAL '").append(refreshSchedule.getInterval()).append("' ")
                    .append(refreshSchedule.getUnit());
            } else if (refreshSchedule.getStartStrategy() == DBMaterializedViewRefreshSchedule.StartStrategy.START_AT) {
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String formattedDate = formatter.format(refreshSchedule.getStartWith());
                sqlBuilder.line().append("START WITH TO_DATE('").append(formattedDate).append("', 'YYYY-MM-DD HH24:MI:SS')");
                sqlBuilder.line().append("NEXT TIMESTAMP TO_DATE('").append(formattedDate).append("', 'YYYY-MM-DD HH24:MI:SS') + INTERVAL '")
                    .append(refreshSchedule.getInterval()).append("' ").append(refreshSchedule.getUnit());
            }
        }
    }

}
