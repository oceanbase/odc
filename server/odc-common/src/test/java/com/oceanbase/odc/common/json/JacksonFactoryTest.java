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
package com.oceanbase.odc.common.json;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Data;

public class JacksonFactoryTest {

    private ObjectMapper objectMapper = JacksonFactory.jsonMapper();

    @Test
    public void jsonMapper_long() throws JsonProcessingException {
        String expected = "{\"oint\":-100,\"olongLarge\":\"-9007199254740992\",\"olongSmall\":9007199254740991,"
                + "\"pint\":100,\"plongLarge\":\"9007199254740992\",\"plongSmall\":-9007199254740991}";

        String json = objectMapper.writeValueAsString(new IntTypes());

        Assert.assertEquals(expected, json);
    }

    @Test
    public void jsonMapper_float() throws JsonProcessingException {
        String expected = "{\"odouble1\":0.00,\"odouble2\":0.00,\"ofloat1\":0.00,\"ofloat2\":0.00,\"pdouble1\":0.00,"
                + "\"pdouble2\":0.00,\"pfloat1\":0.00,\"pfloat2\":0.00}";

        String json = objectMapper.writeValueAsString(new FloatTypes());

        Assert.assertEquals(expected, json);
    }

    @Test
    public void jsonMapper_nested() throws JsonProcessingException {
        String expected = "{\"a\":{\"odouble1\":0.00}}";

        String json = objectMapper.writeValueAsString(new WithNestedType());

        Assert.assertEquals(expected, json);
    }

    @Data
    static class IntTypes {

        private long pLongLarge = 9007199254740992L;
        private Long oLongLarge = -9007199254740992L;
        private long pLongSmall = -9007199254740991L;
        private Long oLongSmall = 9007199254740991L;
        private int pInt = 100;
        private Integer oInt = -100;
    }

    @Data
    static class FloatTypes {

        private float pFloat1 = 0;
        private float pFloat2 = 0.0f;
        private Float oFloat1 = 0f;
        private Float oFloat2 = 0.0f;

        private double pDouble1 = 0;
        private double pDouble2 = 0.0;
        private Double oDouble1 = 0d;
        private Double oDouble2 = 0.0d;

    }

    @Data
    static class NestedType {

        private Double oDouble1 = (double) 0;

    }

    @Data
    static class WithNestedType {

        NestedType a = new NestedType();

    }
}
