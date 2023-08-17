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
package com.oceanbase.odc.core.shared;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.oceanbase.odc.core.shared.exception.VerifyException;

/**
 * Test cases for {@link Verify}
 *
 * @author yh263208
 * @date 2022-03-03 12:06
 * @since ODC_release_3.3.0
 */
public class VerifyTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void verify_FalseExpression_ExceptionThrown() {
        String message = "test";
        thrown.expectMessage(message);
        thrown.expect(VerifyException.class);
        Verify.verify(1 == 2, message);
    }

    @Test
    public void notNegative_NegativeValueVerify_ExceptionThrown() {
        String parameterName = "name";
        int value = -1;
        thrown.expectMessage(String.format("%s was negative, value=%d", parameterName, value));
        thrown.expect(VerifyException.class);
        Verify.notNegative(value, parameterName);
    }

    @Test
    public void notNegative_ZeroValueVerify_NothingThrown() {
        String parameterName = "name";
        int value = 0;
        Assert.assertEquals(value, Verify.notNegative(value, parameterName));
    }

    @Test
    public void greaterThan_GreaterValueSet_NothingThrown() {
        int value = 5;
        Assert.assertEquals(value, Verify.greaterThan(value, 3, "Test"));
    }

    @Test
    public void greaterThan_EqualValueSet_ExceptionThrown() {
        int value = 5;
        String parameterName = "Test";
        thrown.expectMessage(String.format("%s was less than or equal to %d, value=%d", parameterName, value, value));
        thrown.expect(VerifyException.class);
        Verify.greaterThan(value, value, parameterName);
    }

    @Test
    public void greaterThan_LessValueSet_ExceptionThrown() {
        int value = 5;
        int downValue = 8;
        String parameterName = "Test";
        thrown.expectMessage(
                String.format("%s was less than or equal to %d, value=%d", parameterName, downValue, value));
        thrown.expect(VerifyException.class);
        Verify.greaterThan(value, downValue, parameterName);
    }

    @Test
    public void notLessThan_GreaterValueSet_NothingThrown() {
        int value = 5;
        Assert.assertEquals(value, Verify.notLessThan(value, 3, "Test"));
    }

    @Test
    public void notLessThan_EqualValueSet_NothingThrown() {
        int value = 5;
        String parameterName = "Test";
        Assert.assertEquals(value, Verify.notLessThan(value, value, parameterName));
    }

    @Test
    public void notLessThan_LessValueSet_ExceptionThrown() {
        int value = 5;
        int downValue = 8;
        String parameterName = "Test";
        thrown.expectMessage(String.format("%s was less than %d, value=%d", parameterName, downValue, value));
        thrown.expect(VerifyException.class);
        Verify.notLessThan(value, downValue, parameterName);
    }

    @Test
    public void lessThan_LessValueSet_NothingThrown() {
        int value = 5;
        Assert.assertEquals(value, Verify.lessThan(value, 8, "Test"));
    }

    @Test
    public void lessThan_EqualValueSet_ExceptionThrown() {
        int value = 5;
        String parameterName = "Test";
        thrown.expectMessage(
                String.format("%s was greater than or equal to %d, value=%d", parameterName, value, value));
        thrown.expect(VerifyException.class);
        Verify.lessThan(value, value, parameterName);
    }

    @Test
    public void lessThan_GreaterValueSet_ExceptionThrown() {
        int value = 8;
        int upValue = 5;
        String parameterName = "Test";
        thrown.expectMessage(
                String.format("%s was greater than or equal to %d, value=%d", parameterName, upValue, value));
        thrown.expect(VerifyException.class);
        Verify.lessThan(value, upValue, parameterName);
    }

    @Test
    public void notGreaterThan_LessValueSet_NothingThrown() {
        int value = 5;
        Assert.assertEquals(value, Verify.notGreaterThan(value, 8, "Test"));
    }

    @Test
    public void notGreaterThan_EqualValueSet_NothingThrown() {
        int value = 5;
        String parameterName = "Test";
        Assert.assertEquals(value, Verify.notGreaterThan(value, value, parameterName));
    }

    @Test
    public void notGreaterThan_GreaterValueSet_ExceptionThrown() {
        int value = 8;
        int upValue = 5;
        String parameterName = "Test";
        thrown.expectMessage(String.format("%s was greater than %d, value=%d", parameterName, upValue, value));
        thrown.expect(VerifyException.class);
        Verify.notGreaterThan(value, upValue, parameterName);
    }

    @Test
    public void singleton_NullCollection_ExceptionThrown() {
        String parameterName = "Test";
        thrown.expect(VerifyException.class);
        thrown.expectMessage(String.format("the value of %s expected not null", parameterName));
        Verify.singleton(null, parameterName);
    }

    @Test
    public void singleton_SingletonCollection_NothingThrown() {
        String parameterName = "Test";
        List<String> target = Collections.singletonList("David");
        Assert.assertEquals(target, Verify.singleton(target, parameterName));
    }

    @Test
    public void singleton_EmptyCollection_ExceptionThrown() {
        String parameterName = "Test";
        thrown.expect(VerifyException.class);
        thrown.expectMessage(String.format("%s was empty", parameterName));
        Verify.singleton(Collections.emptyList(), parameterName);
    }

    @Test
    public void singleton_GreaterCollection_ExceptionThrown() {
        String parameterName = "Test";
        thrown.expect(VerifyException.class);
        thrown.expectMessage(String.format("%s's size is greater than 1", parameterName));
        Verify.singleton(Arrays.asList("a", "b"), parameterName);
    }

    @Test
    public void testEquals_String() {
        String parameterName = "Test";
        thrown.expect(VerifyException.class);
        thrown.expectMessage("Test expected Hello but was World");
        Verify.equals("Hello", "World", parameterName);
    }

    @Test
    public void testEquals_boolean() {
        String parameterName = "Test";
        thrown.expect(VerifyException.class);
        thrown.expectMessage("Test expected true but was false");
        Verify.equals(true, false, parameterName);
    }
}
