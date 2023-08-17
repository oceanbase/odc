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
package com.oceanbase.odc.core.sql.util;

import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.odc.core.sql.util.OracleDateFormatSymbols.DateFormatPattern;

/**
 * Test cases for {@link OracleDateFormatSymbols}
 *
 * @author yh263208
 * @date 2022-11-01 19:33
 * @since ODC_release_4.1.0
 */
public class OracleDateFormatSymbolsTest {

    @Test
    public void firstMatchedPattern_notThingMatched_returnUnDefine() {
        OracleDateFormatSymbols symbols = new OracleDateFormatSymbols();
        DateFormatPattern pattern = symbols.matchPattern("abcdeft", 0);
        Assert.assertEquals(DateFormatPattern.UNDEFINE, pattern);
    }

    @Test
    public void firstMatchedPattern_matchLongestPattern_matchSucceed() {
        OracleDateFormatSymbols symbols = new OracleDateFormatSymbols();
        DateFormatPattern pattern = symbols.matchPattern("yyyy HH24:MI:SSSSS TDZ TDR", 13);
        Assert.assertEquals(DateFormatPattern.SSSSS, pattern);
    }

    @Test
    public void firstMatchedPattern_upperCaseAndLowerCaseMixed_matchSucceed() {
        OracleDateFormatSymbols symbols = new OracleDateFormatSymbols();
        DateFormatPattern pattern = symbols.matchPattern("yyyy HH24:MI:Ss TDZ TDR", 13);
        Assert.assertEquals(DateFormatPattern.SS, pattern);
    }

}
