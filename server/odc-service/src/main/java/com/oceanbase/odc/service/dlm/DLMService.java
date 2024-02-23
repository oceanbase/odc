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
package com.oceanbase.odc.service.dlm;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.oceanbase.odc.service.dlm.model.GetRealSqlListReq;
import com.oceanbase.odc.service.dlm.utils.DataArchiveConditionUtil;

/**
 * @Author：tinker
 * @Date: 2024/2/23 11:48
 * @Descripition:
 */
@Service
public class DLMService {

    public List<String> getRealSqlList(GetRealSqlListReq req) {
        List<String> returnValue = new LinkedList<>();
        String previewSqlTemp = "select * from %s where %s;";
        Date now = new Date();
        req.getTables().forEach(tableConfig -> {
            returnValue.add(String.format(previewSqlTemp, tableConfig.getTableName(), DataArchiveConditionUtil
                    .parseCondition(tableConfig.getConditionExpression(), req.getVariables(), now)));
        });
        return returnValue;
    }
}
