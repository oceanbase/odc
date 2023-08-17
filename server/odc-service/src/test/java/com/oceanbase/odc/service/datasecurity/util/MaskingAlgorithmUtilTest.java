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
package com.oceanbase.odc.service.datasecurity.util;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author gaoda.xy
 * @date 2023/5/19 16:05
 */
public class MaskingAlgorithmUtilTest {

    @Test
    public void test_process_character_collection_single_characters() {
        List<String> collection = Arrays.asList("a", "c", "d", "f", "1", "%", "(");
        String resultStr = MaskingAlgorithmUtil.processCharacterCollection(collection);
        Assert.assertEquals("a,c,d,f,1,%,(", resultStr);
    }

    @Test
    public void test_process_character_collection_range_characters() {
        List<String> collection = Arrays.asList("a", "c", "d", "p~z", "0~4", "%", "(");
        String resultStr = MaskingAlgorithmUtil.processCharacterCollection(collection);
        Assert.assertEquals("a,c,d,p,q,r,s,t,u,v,w,x,y,z,0,1,2,3,4,%,(", resultStr);
    }

    @Test
    public void test_process_character_collection_invalid() {
        List<String> collection = Arrays.asList("a", "c", "d", "p-z", "0-", "-2", "%", "(");
        String resultStr = MaskingAlgorithmUtil.processCharacterCollection(collection);
        Assert.assertEquals("a,c,d,%,(", resultStr);
    }

}
