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
package com.oceanbase.odc.service.dlm.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.oceanbase.odc.service.dlm.model.OffsetConfig;
import com.oceanbase.odc.service.dlm.model.Operator;

import cn.hutool.core.lang.Assert;

/**
 * @Author：tinker
 * @Date: 2023/5/30 10:39
 * @Descripition:
 */
public class DataArchiveConditionUtilTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testParseCondition() {
        Date date = new Date();
        String condition = "select * from test where gmt_create>'${start}' and gmt_create<'${end}'";
        OffsetConfig config = new OffsetConfig();
        config.setName("start");
        config.setPattern("yyyy-MM-dd|+10M");
        OffsetConfig config2 = new OffsetConfig();
        config2.setName("end");
        config2.setOperator(Operator.PLUS);
        config2.setDateFormatPattern("yyyy-MM-dd");
        config2.setUnit("d");
        config2.setValue(1L);
        List<OffsetConfig> variables = new LinkedList<>();
        variables.add(config);
        variables.add(config2);
        String result = DataArchiveConditionUtil.parseCondition(condition, variables, date);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.MONTH, 10);
        String expect = condition.replace("${start}", sdf.format(calendar.getTime()));
        calendar.setTime(date);
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        expect = expect.replace("${end}", sdf.format(calendar.getTime()));
        Assert.equals(expect, result);
    }

    @Test
    public void testNotFoundVariable() {
        Date date = new Date();
        String condition = "select * from test where gmt_create>'${start}' and gmt_create<'${end}'";
        OffsetConfig config = new OffsetConfig();
        config.setName("start");
        config.setPattern("yyyy-MM-dd HH:mm:ss|+10M");
        List<OffsetConfig> variables = new LinkedList<>();
        variables.add(config);

        thrown.expect(IllegalArgumentException.class);
        DataArchiveConditionUtil.parseCondition(condition, variables, date);
    }

}
