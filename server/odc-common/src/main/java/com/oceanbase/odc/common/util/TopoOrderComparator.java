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
package com.oceanbase.odc.common.util;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;

import lombok.NonNull;

/**
 * {@link TopoOrderComparator}
 *
 * @author yh263208
 * @date 2022-08-26 13:25
 * @since ODC_release_3.4.0
 */
public class TopoOrderComparator<T> implements Comparator<T> {

    private final Map<T, Set<T>> item2ReliedItems;

    public TopoOrderComparator(Map<T, Set<T>> item2ReliedItems) {
        this.item2ReliedItems = item2ReliedItems;
    }

    public TopoOrderComparator() {
        this.item2ReliedItems = new HashMap<>();
    }

    @Override
    public int compare(T first, T second) {
        return isReached(first, second) ? 1 : (isReached(second, first) ? -1 : 0);
    }

    public void addAll(@NonNull T target, Set<T> reliedItems) {
        if (CollectionUtils.isEmpty(reliedItems)) {
            return;
        }
        Set<T> value = this.item2ReliedItems.computeIfAbsent(target, k -> new HashSet<>());
        value.addAll(reliedItems);
    }

    private boolean isReached(T begin, T end) {
        Set<T> nextSteps = item2ReliedItems.get(begin);
        if (CollectionUtils.isEmpty(nextSteps)) {
            return false;
        }
        return nextSteps.contains(end);
    }

}
