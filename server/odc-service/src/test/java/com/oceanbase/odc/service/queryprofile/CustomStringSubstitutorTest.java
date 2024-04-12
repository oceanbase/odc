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
package com.oceanbase.odc.service.queryprofile;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.oceanbase.odc.service.queryprofile.helper.CustomStringSubstitutor;

/**
 * @author liuyizhuo.lyz
 * @date 2024/4/12
 */
public class CustomStringSubstitutorTest {

    @Test
    public void test_Replace_Success() {
        Map<String, String> variables = ImmutableMap.of(":1", "VALUE1", ":23", "VALUE2");
        String template = "filter([ORDERS.O_ORDERDATE >= :1], [ORDERS.O_ORDERDATE <= :23])";
        Assert.assertEquals(
                "filter([ORDERS.O_ORDERDATE >= VALUE1], [ORDERS.O_ORDERDATE <= VALUE2])",
                CustomStringSubstitutor.replace(template, variables));
    }

    @Test
    public void test_Replace_Fail() {
        Map<String, String> variables = ImmutableMap.of(":1", "VALUE1", ":23", "VALUE2");
        String template = "filter([REGION.R_NAME = :3])";
        Assert.assertEquals(template, CustomStringSubstitutor.replace(template, variables));
    }

}
