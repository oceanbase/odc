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
package com.oceanbase.odc.core.migrate.resource.util;

import java.util.LinkedList;
import java.util.List;

import com.oceanbase.odc.common.util.MapperUtils.PathMatcher;
import com.oceanbase.odc.core.migrate.resource.checker.ExpressionChecker;
import com.oceanbase.odc.core.migrate.resource.checker.ListChecker;
import com.oceanbase.odc.core.migrate.resource.checker.NumericRangeChecker;
import com.oceanbase.odc.core.migrate.resource.checker.StringCheck;
import com.oceanbase.odc.core.migrate.resource.checker.WildcardChecker;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Match path by prefix
 *
 * @author yh263208
 * @date 2022-03-20 00:48
 * @since ODC_release_3.3.1
 * @see PathMatcher
 */
@Slf4j
public class PrefixPathMatcher implements PathMatcher {

    private final String target;
    private final List<ExpressionChecker> checkers = new LinkedList<>();

    public PrefixPathMatcher(@NonNull String target) {
        this.target = target;
        checkers.add(new NumericRangeChecker());
        checkers.add(new StringCheck());
        checkers.add(new ListChecker());
        checkers.add(new WildcardChecker());
    }

    @Override
    public boolean match(@NonNull List<Object> prefix, Object current) {
        if (current == null) {
            return false;
        }
        List<Object> prefixes = new LinkedList<>(prefix);
        prefixes.add(current);
        String[] targets = target.split("\\.");
        if (prefixes.size() != targets.length) {
            return false;
        }
        for (int i = 0; i < targets.length; i++) {
            String expression = targets[i];
            Object value = prefixes.get(i);
            boolean matched = false;
            for (ExpressionChecker checker : checkers) {
                if (checker.supports(expression) && checker.contains(expression, value)) {
                    if (log.isDebugEnabled()) {
                        log.debug("Expression checker hits, target={}, expression={}, value={}, checker={}",
                                target, expression, value, checker.getClass().getSimpleName());
                    }
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                return false;
            }
        }
        return true;
    }

}
