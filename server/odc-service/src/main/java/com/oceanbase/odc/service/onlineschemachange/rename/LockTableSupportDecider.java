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
package com.oceanbase.odc.service.onlineschemachange.rename;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.collections4.CollectionUtils;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.util.StringUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * @author longpeng.zlp
 * @date 2024/12/2 16:30
 */
@Slf4j
public class LockTableSupportDecider {
    public static final LockTableSupportDecider DEFAULT_LOCK_TABLE_DECIDER =
            new LockTableSupportDecider(Arrays.asList("4\\.2\\.[5-9].*"));
    private final List<String> lockTableVersionMatchers;

    private LockTableSupportDecider(List<String> lockTableVersionMatchers) {
        this.lockTableVersionMatchers = lockTableVersionMatchers;
    }

    public boolean supportLockTable(String dbVersion) {
        log.info("decide if version = {} support lock table with candidates = {}", dbVersion, lockTableVersionMatchers);
        if (CollectionUtils.isEmpty(lockTableVersionMatchers)) {
            // empty not support
            return false;
        }
        for (String candidate : lockTableVersionMatchers) {
            Pattern pattern = Pattern.compile(candidate);
            if (pattern.matcher(dbVersion).find()) {
                return true;
            }
        }
        return false;
    }

    /**
     * create with array in matchers
     * 
     * @param versionMatchers
     * @return
     */
    public static LockTableSupportDecider createWithList(List<String> versionMatchers) {
        return new LockTableSupportDecider(versionMatchers);
    }

    /**
     * create with string in format json array
     *
     * @param versionMatchersInJsonArray string in json array eg "["v1", "v2", ...]"
     * @return if versionMatchersInJsonArray is empty return DEFAULT_LOCK_TABLE_DECIDER otherwise build
     *         with input string
     */
    public static LockTableSupportDecider createWithJsonArrayWithDefaultValue(String versionMatchersInJsonArray) {
        if (StringUtils.isBlank(versionMatchersInJsonArray)) {
            return DEFAULT_LOCK_TABLE_DECIDER;
        }
        List<String> ret = JsonUtils.fromJsonList(versionMatchersInJsonArray, String.class);
        return new LockTableSupportDecider(ret);
    }
}
