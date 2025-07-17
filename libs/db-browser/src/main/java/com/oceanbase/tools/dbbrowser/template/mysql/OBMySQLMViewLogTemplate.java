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
package com.oceanbase.tools.dbbrowser.template.mysql;

import java.text.SimpleDateFormat;

import org.apache.commons.lang3.Validate;

import com.oceanbase.tools.dbbrowser.model.DBMViewLogPurgeSchedule;
import com.oceanbase.tools.dbbrowser.model.DBMaterializedViewLog;
import com.oceanbase.tools.dbbrowser.template.BaseMViewLogTemplate;
import com.oceanbase.tools.dbbrowser.util.MySQLSqlBuilder;
import com.oceanbase.tools.dbbrowser.util.SqlBuilder;

/**
 * @description:
 * @author: zijia.cj
 * @date: 2025/7/15 19:04
 * @since: 4.4.0
 */
public class OBMySQLMViewLogTemplate extends BaseMViewLogTemplate {

    @Override
    protected SqlBuilder sqlBuilder() {
        return new MySQLSqlBuilder();
    }

    @Override
    protected void fillPurgeSchedule(DBMaterializedViewLog dbMViewLog, SqlBuilder sqlBuilder) {
        DBMViewLogPurgeSchedule purgeSchedule = dbMViewLog.getPurgeSchedule();
        if (null == purgeSchedule) {
            return;
        }
        Validate.notNull(purgeSchedule.getStartStrategy(), "Start strategy of purge schedule can not be null");
        switch (purgeSchedule.getStartStrategy()) {
            case START_NOW:
                sqlBuilder.line().append("PURGE START WITH sysdate()");
                sqlBuilder.line().append("NEXT sysdate() + INTERVAL ").append(purgeSchedule.getInterval()).append(" ")
                        .append(purgeSchedule.getUnit());
                break;
            case START_AT:
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String formattedDate = formatter.format(purgeSchedule.getStartWith());
                sqlBuilder.line().append("PURGE START WITH TIMESTAMP '").append(formattedDate).append("'");
                sqlBuilder.line().append("NEXT TIMESTAMP '").append(formattedDate).append("' + INTERVAL ")
                        .append(purgeSchedule.getInterval()).append(" ").append(purgeSchedule.getUnit());
                break;
            default:
                throw new IllegalArgumentException("Unsupported start strategy: " + purgeSchedule.getStartStrategy());
        }
    }

}
