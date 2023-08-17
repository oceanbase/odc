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
package com.oceanbase.odc.core.migrate.resource;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.oceanbase.odc.core.migrate.resource.checker.NumericRangeChecker;

/**
 * test cases for {@link NumericRangeChecker}
 *
 * @author yh263208
 * @date 2022-06-23 18:09
 * @since ODC_release_3.3.2
 */
public class NumericRangeCheckerTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void range_illegalRangeStr_expThrown() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Range string is illegal, 12");
        NumericRangeChecker checker = new NumericRangeChecker();
        checker.contains("12", "12");
    }

    @Test
    public void range_illegalRangeStrLeakLeft_expThrown() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Range string is illegal, 12]");
        NumericRangeChecker checker = new NumericRangeChecker();
        checker.contains("12]", "12");
    }

    @Test
    public void range_illegalRangeStrRightLeft_expThrown() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Range string is illegal, [12");
        NumericRangeChecker checker = new NumericRangeChecker();
        checker.contains("[12", "12");
    }

    @Test
    public void range_illegalRangeStrWrongNumFormat_expThrown() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Range string is illegal, d12]");
        NumericRangeChecker checker = new NumericRangeChecker();
        checker.contains("d12]", "13");
    }

    @Test
    public void range_minBiggerThanMax_expThrown() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Maximum is not bigger than Minimum, [12-5]");
        NumericRangeChecker checker = new NumericRangeChecker();
        checker.contains("[12-5]", "1");
    }

    @Test
    public void isRangeStr_illegalInput_returnFalse() {
        NumericRangeChecker checker = new NumericRangeChecker();
        Assert.assertFalse(checker.supports("asdasd"));
    }

    @Test
    public void isRangeStr_rightInput_returnTrue() {
        NumericRangeChecker checker = new NumericRangeChecker();
        Assert.assertFalse(checker.supports("[1,4]"));
        Assert.assertTrue(checker.supports("[1-4]"));
    }

    @Test
    public void contains_betweenMinAndMax_returnTrue() {
        NumericRangeChecker checker = new NumericRangeChecker();
        Assert.assertTrue(checker.contains("[12-15]", 15));
    }

    @Test
    public void contains_excludeRightAndNotBetweenMinAndMax_returnFalse() {
        NumericRangeChecker checker = new NumericRangeChecker();
        Assert.assertFalse(checker.contains("[12-15]", 16));
    }

    @Test
    public void contains_excludeLeftAndNotBetweenMinAndMax_returnFalse() {
        NumericRangeChecker checker = new NumericRangeChecker();
        Assert.assertFalse(checker.contains("[12-15]", 11));
    }

}
