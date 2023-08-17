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

import java.util.Collection;
import java.util.Objects;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.shared.exception.VerifyException;

/**
 * different from PreConditions, Verify does not check parameters. <br>
 * the design inspired from https://github.com/google/guava/wiki/ConditionalFailuresExplained
 * 
 * @author yizhou.xw
 * @version : Verify.java, v 0.1 2021-06-02 15:46
 */
public final class Verify {

    public static void verify(boolean expression, String message) {
        if (!expression) {
            throw new VerifyException(message);
        }
    }

    public static void equals(Object expected, Object value, String parameterName) {
        equals(expected, value, parameterName, null);
    }

    public static void equals(Object expected, Object value, String parameterName, String messagePrefix) {
        if (!Objects.equals(expected, value)) {
            String violation = String.format("%s expected %s but was %s", parameterName, expected, value);
            throw createVerifyException(messagePrefix, violation);
        }
    }

    public static void equals(boolean expected, boolean value, String parameterName) {
        if (expected != value) {
            throw new VerifyException(String.format("%s expected %s but was %s", parameterName, expected, value));
        }
    }

    public static void equals(long expected, long value, String parameterName) {
        if (expected != value) {
            throw new VerifyException(String.format("%s expected %s but was %s", parameterName, expected, value));
        }
    }

    public static void equals(int expected, int value, String parameterName) {
        if (expected != value) {
            throw new VerifyException(String.format("%s expected %s but was %s", parameterName, expected, value));
        }
    }

    public static long notNegative(long value, String parameterName) {
        if (value < 0) {
            throw new VerifyException(String.format("%s was negative, value=%d", parameterName, value));
        }
        return value;
    }

    public static int notNegative(int value, String parameterName) {
        if (value < 0) {
            throw new VerifyException(String.format("%s was negative, value=%d", parameterName, value));
        }
        return value;
    }

    public static int greaterThan(int value, int downValue, String parameterName) {
        if (value <= downValue) {
            throw new VerifyException(
                    String.format("%s was less than or equal to %d, value=%d", parameterName, downValue, value));
        }
        return value;
    }

    public static long greaterThan(long value, long downValue, String parameterName) {
        if (value <= downValue) {
            throw new VerifyException(
                    String.format("%s was less than or equal to %d, value=%d", parameterName, downValue, value));
        }
        return value;
    }

    public static int notLessThan(int value, int downValue, String parameterName) {
        if (value < downValue) {
            throw new VerifyException(String.format("%s was less than %d, value=%d", parameterName, downValue, value));
        }
        return value;
    }

    public static long notLessThan(long value, long downValue, String parameterName) {
        if (value < downValue) {
            throw new VerifyException(String.format("%s was less than %d, value=%d", parameterName, downValue, value));
        }
        return value;
    }

    public static int lessThan(int value, int upValue, String parameterName) {
        if (value >= upValue) {
            throw new VerifyException(
                    String.format("%s was greater than or equal to %d, value=%d", parameterName, upValue, value));
        }
        return value;
    }

    public static long lessThan(long value, long upValue, String parameterName) {
        if (value >= upValue) {
            throw new VerifyException(
                    String.format("%s was greater than or equal to %d, value=%d", parameterName, upValue, value));
        }
        return value;
    }

    public static int notGreaterThan(int value, int upValue, String parameterName) {
        if (value > upValue) {
            throw new VerifyException(String.format("%s was greater than %d, value=%d", parameterName, upValue, value));
        }
        return value;
    }

    public static long notGreaterThan(long value, long upValue, String parameterName) {
        if (value > upValue) {
            throw new VerifyException(String.format("%s was greater than %d, value=%d", parameterName, upValue, value));
        }
        return value;
    }

    public static <T> Collection<T> singleton(Collection<T> collection, String parameterName) {
        notNull(collection, parameterName);
        if (collection.isEmpty()) {
            throw new VerifyException(String.format("%s was empty", parameterName));
        } else if (collection.size() > 1) {
            throw new VerifyException(String.format("%s's size is greater than 1", parameterName));
        }
        return collection;
    }

    public static <T> T notNull(final T obj, String parameterName) {
        return notNull(obj, parameterName, null);
    }

    public static <T> T notNull(final T obj, String parameterName, String messagePrefix) {
        if (obj == null) {
            String violation = String.format("the value of %s expected not null", parameterName);
            throw createVerifyException(messagePrefix, violation);
        }
        return obj;
    }

    public static <T extends CharSequence> T notEmpty(final T chars, String parameterName) {
        notNull(chars, parameterName);
        if (chars.length() == 0) {
            throw new VerifyException(String.format("the value of %s expected not empty", parameterName));
        }
        return chars;
    }

    public static <T extends CharSequence> T notBlank(final T chars, String parameterName) {
        return notBlank(chars, parameterName, null);
    }

    public static <T extends CharSequence> T notBlank(final T chars, String parameterName, String messagePrefix) {
        notNull(chars, parameterName, messagePrefix);
        if (StringUtils.isBlank(chars)) {
            String violation = String.format("the value of %s expected not empty", parameterName);
            throw createVerifyException(messagePrefix, violation);
        }
        return chars;
    }

    public static <T extends Collection<?>> T notEmpty(final T collection, String parameterName) {
        notNull(collection, parameterName);
        if (collection.isEmpty()) {
            throw new VerifyException(String.format("the length of %s expected not empty", parameterName));
        }
        return collection;
    }

    private static VerifyException createVerifyException(String messagePrefix, String violation) {
        return new VerifyException(combineMessage(messagePrefix, violation));
    }

    private static String combineMessage(String prefix, String violation) {
        prefix = prefix == null ? "" : prefix + ":";
        return prefix + violation;
    }

}
