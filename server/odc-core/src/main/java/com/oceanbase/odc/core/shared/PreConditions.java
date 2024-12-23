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

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.BooleanSupplier;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import com.oceanbase.odc.common.security.PasswordChecker;
import com.oceanbase.odc.common.security.SqlInjectionDetector;
import com.oceanbase.odc.common.util.FilePathTraversalChecker;
import com.oceanbase.odc.common.util.SSRFChecker;
import com.oceanbase.odc.core.shared.constant.ErrorCode;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.constant.LimitMetric;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.AccessDeniedException;
import com.oceanbase.odc.core.shared.exception.BadArgumentException;
import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.core.shared.exception.OverLimitException;

/**
 * for argument pre check, expect to replace {@link org.apache.commons.lang3.Validate}
 * 
 * @author yizhou.xw
 * @version : PreConditions.java, v 0.1 2021-03-19 20:17
 */
public class PreConditions {
    public static <T> T notNull(final T obj, String parameterName) {
        return notNull(obj, parameterName, String.format("parameter %s may not be null", parameterName));
    }

    public static <T> T notNull(final T obj, String parameterName, String message) {
        if (obj == null) {
            throw new BadArgumentException(ErrorCodes.NotNull, new Object[] {parameterName}, message);
        }
        return obj;
    }

    public static <T extends CharSequence> T notEmpty(final T chars, String parameterName) {
        notNull(chars, parameterName);
        if (chars.length() == 0) {
            throw new BadArgumentException(ErrorCodes.NotEmptyString, new Object[] {parameterName},
                    String.format("parameter %s may not be empty", parameterName));
        }
        return chars;
    }

    public static <T extends CharSequence> T notBlank(final T chars, String parameterName) {
        return notBlank(chars, parameterName, String.format("parameter %s may not be blank", parameterName));
    }

    public static <T extends CharSequence> T notBlank(final T chars, String parameterName, String message) {
        notNull(chars, parameterName);
        if (StringUtils.isBlank(chars)) {
            throw new BadArgumentException(ErrorCodes.NotBlankString, new Object[] {parameterName}, message);
        }
        return chars;
    }

    public static <T extends Collection<?>> T notEmpty(final T collection, String parameterName, String message) {
        notNull(collection, parameterName);
        if (collection.isEmpty()) {
            throw new BadArgumentException(ErrorCodes.NotEmptyList, new Object[] {parameterName}, message);
        }
        return collection;
    }

    public static <T> T[] notEmpty(final T[] array, String parameterName) {
        notNull(array, parameterName);
        if (array.length > 0) {
            return array;
        }
        throw new BadArgumentException(ErrorCodes.NotEmptyList, new Object[] {parameterName},
                String.format("array parameter %s may not be empty", parameterName));
    }

    public static <T extends Collection<?>> T notEmpty(final T collection, String parameterName) {
        return notEmpty(collection, parameterName,
                String.format("collection parameter %s may not be empty", parameterName));
    }

    public static int notNegative(int value, String parameterName) {
        if (value < 0) {
            throw new BadArgumentException(ErrorCodes.NotNegative, new Object[] {parameterName},
                    String.format("%s was negative, value=%d", parameterName, value));
        }
        return value;
    }

    public static long notNegative(long value, String parameterName) {
        if (value < 0) {
            throw new BadArgumentException(ErrorCodes.NotNegative, new Object[] {parameterName},
                    String.format("%s was negative, value=%d", parameterName, value));
        }
        return value;
    }

    public static void validPassword(String password) {
        if (!PasswordChecker.checkPassword(password)) {
            throw new BadArgumentException(ErrorCodes.UserInvalidPassword, null, "invalid password");
        }
    }

    public static void validArgumentState(final boolean expression, ErrorCode errorCode, Object[] args,
            String message) {
        if (!expression) {
            if (args == null && null != message) {
                args = new Object[] {message};
            }
            throw new BadArgumentException(errorCode, args, message);
        }
    }

    public static void validHasPermission(final boolean expression, ErrorCode errorCode,
            String message) {
        if (!expression) {
            throw new AccessDeniedException(errorCode, message);
        }
    }

    public static void validRequestState(final boolean expression, ErrorCode errorCode, Object[] args,
            String message) {
        if (!expression) {
            if (args == null && null != message) {
                args = new Object[] {message};
            }
            throw new BadRequestException(errorCode, args, message);
        }
    }

    public static boolean validExists(ResourceType resourceType, String parameterName,
            Object parameterValue, BooleanSupplier existsChecker) {
        if (existsChecker.getAsBoolean()) {
            return true;
        }
        throw new NotFoundException(ErrorCodes.NotFound,
                new Object[] {resourceType.getLocalizedMessage(), parameterName, parameterValue},
                String.format("%s not found by %s=%s", resourceType, parameterName, parameterValue.toString()));
    }

    /**
     * validate if file exists
     * 
     * @param file
     * @return true if file exists, else throw NotFoundException
     */
    public static boolean validExists(File file) {
        notNull(file, "file");
        if (file.exists()) {
            return true;
        }
        throw new NotFoundException(ErrorCodes.NotFound,
                new Object[] {ResourceType.ODC_FILE.getLocalizedMessage(), "fileName", file.getName()},
                String.format("File not found by %s=%s", "fileName", file.getName()));
    }

    public static boolean validNoPathTraversal(String path, String... allowPaths) {
        notEmpty(path, "path");
        notEmpty(allowPaths, "allowPaths");
        File file = new File(path);
        return validNoPathTraversal(file, allowPaths);
    }

    public static boolean validNoPathTraversal(File file, String... allowPaths) {
        notNull(file, "file");
        notEmpty(allowPaths, "allowPaths");
        if (FilePathTraversalChecker.checkPathTraversal(file, Arrays.asList(allowPaths))) {
            return true;
        }
        throw new BadRequestException(ErrorCodes.FilePathTraversalDetected, null,
                "File path traversal detected, fileName=" + file.getName());
    }

    public static boolean validInHostWhiteList(String host, List<String> whiteList) {
        if (SSRFChecker.checkHostInWhiteList(host, whiteList)) {
            return true;
        }
        throw new BadRequestException(ErrorCodes.ConnectHostNotAllowed, null,
                "Host is not in white list, host=" + host);
    }

    public static boolean validInUrlWhiteList(String url, List<String> whiteList) {
        if (SSRFChecker.checkUrlInWhiteList(url, whiteList)) {
            return true;
        }
        throw new BadArgumentException(ErrorCodes.ExternalUrlNotAllowed, new Object[] {url},
                "The URL is not in white list, URL=" + url);
    }

    public static void validNoDuplicated(ResourceType resourceType,
            String parameterName, Object parameterValue, BooleanSupplier duplicatedChecker) {
        if (duplicatedChecker.getAsBoolean()) {
            throw new BadRequestException(ErrorCodes.DuplicatedExists,
                    new Object[] {resourceType.getLocalizedMessage(), parameterName, parameterValue},
                    String.format("%s already exists by %s=%s", resourceType, parameterName,
                            parameterValue.toString()));
        }
    }

    public static boolean validNotSqlInjection(String param, String paramName) {
        if (StringUtils.isEmpty(param)) {
            return true;
        }
        String message = String.format("%s may contain invalid characters", paramName);
        if (SqlInjectionDetector.isSqlInjection(param)) {
            throw new BadArgumentException(ErrorCodes.BadArgument, new Object[] {message}, message);
        }
        return true;
    }

    public static void maxLength(String str, String parameterName, int maxLength) {
        if (StringUtils.length(str) > maxLength) {
            throw new OverLimitException(LimitMetric.SQL_LENGTH, (double) maxLength,
                    String.format("%s has exceeded sql length limit: %s", parameterName, maxLength));
        }
    }

    public static void maxSize(String str, String parameterName, int maxSize) {
        if (StringUtils.getBytes(str, StandardCharsets.UTF_8).length > maxSize) {
            throw new OverLimitException(LimitMetric.SQL_SIZE, (double) maxSize,
                    String.format("%s has exceeded sql size limit: %s", parameterName, maxSize));
        }
    }

    public static void lessThanOrEqualTo(String paramName, LimitMetric limitMetric, long actual, long expected) {
        if (actual > expected) {
            throw new OverLimitException(limitMetric, (double) expected,
                    String.format("actual %s exceeds limit: %s", paramName, expected));
        }
    }

    public static boolean validFileSuffix(String fileName, List<String> safeSuffixList) {
        notBlank(fileName, "fileName");

        String extension = FilenameUtils.getExtension(fileName);
        if (StringUtils.isEmpty(extension)) {
            return true;
        }
        for (String suffix : safeSuffixList) {
            if (StringUtils.isBlank(suffix)) {
                continue;
            }
            if (StringUtils.contains(suffix, extension) || StringUtils.equals(suffix, "*")) {
                return true;
            }
        }
        throw new BadRequestException(ErrorCodes.FileSuffixNotAllowed, new Object[] {fileName},
                "file suffix is illegal");
    }

    public static <T> void validSingleton(Collection<T> collection, String parameterName) {
        notEmpty(collection, parameterName);
        if (collection.size() != 1) {
            throw new BadArgumentException(ErrorCodes.BadArgument,
                    String.format("size of parameter %s is greater than 1", parameterName));
        }
    }
}
