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

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;

import com.oceanbase.odc.common.lang.Pair;

import lombok.NonNull;

/**
 * {@link ListUtils}
 *
 * @author yh263208
 * @date 2022-03-18 12:25
 * @since ODC_release_3.3.0
 */
public class ListUtils {

    public static <T> List<List<T>> cartesianProduct(List<List<T>> sets) {
        int[] indexes = new int[sets.size()];
        for (int i = 0; i < indexes.length; i++) {
            List<T> set = sets.get(i);
            int length = set.size();
            if (length == 0) {
                return Collections.emptyList();
            }
            indexes[i] = length - 1;
        }
        List<List<T>> returnVal = new LinkedList<>();
        while (indexes[0] >= 0) {
            List<T> list = new LinkedList<>();
            for (int i = 0; i < indexes.length; i++) {
                List<T> set = sets.get(i);
                int index = set.size() - (indexes[i] + 1);
                list.add(set.get(index));
            }
            for (int i = indexes.length - 1; i >= 0; i--) {
                indexes[i]--;
                if (indexes[i] < 0 && i != 0) {
                    indexes[i] = sets.get(i).size() - 1;
                } else {
                    break;
                }
            }
            returnVal.add(list);
        }
        return returnVal;
    }

    public static <T> void sortByTopoOrder(List<T> targets, @NonNull Comparator<T> comparator) {
        if (CollectionUtils.isEmpty(targets)) {
            return;
        }
        List<T> vertexes = new LinkedList<>();
        Iterator<T> iter = targets.iterator();
        while (iter.hasNext()) {
            T value = iter.next();
            if (vertexes.contains(value)) {
                continue;
            }
            vertexes.add(value);
            iter.remove();
        }
        Map<T, List<Pair<T, T>>> vertex2InEdges = new HashMap<>();
        Map<T, List<Pair<T, T>>> vertex2OutEdges = new HashMap<>();
        List<T> tempTargets = new LinkedList<>(vertexes);
        int size = tempTargets.size();
        for (int i = 0; i < size; i++) {
            T from = tempTargets.get(i);
            for (int j = i; j < size; j++) {
                T to = tempTargets.get(j);
                int result = comparator.compare(from, to);
                if (result == 0) {
                    continue;
                }
                Pair<T, T> edge;
                List<Pair<T, T>> ins;
                List<Pair<T, T>> outs;
                if (result > 0) {
                    edge = new Pair<>(from, to);
                    outs = vertex2OutEdges.computeIfAbsent(from, k -> new LinkedList<>());
                    ins = vertex2InEdges.computeIfAbsent(to, k -> new LinkedList<>());
                } else {
                    edge = new Pair<>(to, from);
                    outs = vertex2OutEdges.computeIfAbsent(to, k -> new LinkedList<>());
                    ins = vertex2InEdges.computeIfAbsent(from, k -> new LinkedList<>());
                }
                outs.add(edge);
                ins.add(edge);
            }
        }
        tempTargets = new LinkedList<>();
        Set<Pair<T, T>> deletedEdges = new HashSet<>();
        while (true) {
            T target = null;
            for (T vertex : vertexes) {
                List<Pair<T, T>> inEdges = vertex2InEdges.getOrDefault(vertex, new LinkedList<>())
                        .stream().filter(e -> !deletedEdges.contains(e)).collect(Collectors.toList());
                if (!tempTargets.contains(vertex) && inEdges.isEmpty()) {
                    target = vertex;
                    break;
                }
            }
            if (target == null) {
                break;
            }
            tempTargets.add(target);
            deletedEdges.addAll(vertex2OutEdges.getOrDefault(target, new LinkedList<>()));
        }
        if (tempTargets.size() < vertexes.size()) {
            throw new IllegalStateException("Circular reference detected");
        }
        iter = targets.iterator();
        while (iter.hasNext()) {
            T value = iter.next();
            int i = tempTargets.indexOf(value);
            if (i < 0) {
                throw new IllegalStateException("Unknown error");
            }
            tempTargets.add(i, value);
            iter.remove();
        }
        if (CollectionUtils.isNotEmpty(targets)) {
            throw new IllegalStateException("Unknown error");
        }
        targets.addAll(tempTargets);
    }

}
