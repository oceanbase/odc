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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.service.dlm.model.DataArchiveTableConfig;
import com.oceanbase.odc.service.dlm.model.GetRealSqlListReq;
import com.oceanbase.odc.service.dlm.model.OffsetConfig;

import cn.hutool.core.lang.Assert;

/**
 * @Author：tinker
 * @Date: 2024/2/23 16:22
 * @Descripition:
 */
public class DLMServiceTest extends ServiceTestEnv {

    @Autowired
    private DLMService dlmService;

    @Test
    public void getRealSqlList() {
        GetRealSqlListReq req = new GetRealSqlListReq();
        List<DataArchiveTableConfig> tableConfigs = new LinkedList<>();
        DataArchiveTableConfig config = new DataArchiveTableConfig();
        config.setTableName("test");
        config.setConditionExpression("create_time > '${param1}'");
        tableConfigs.add(config);
        List<OffsetConfig> params = new LinkedList<>();
        OffsetConfig param = new OffsetConfig();
        param.setPattern("yyyy-MM-dd|-0d");
        param.setName("param1");
        params.add(param);
        req.setTables(tableConfigs);
        req.setVariables(params);
        List<String> realSqlList = dlmService.getRealSqlList(req);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String expect = String.format("select * from %s where create_time > '%s';", "test", sdf.format(new Date()));
        Assert.equals(realSqlList.get(0), expect);
    }

}
