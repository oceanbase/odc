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
package com.oceanbase.odc.core.datamasking.integration;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * @author wenniu.ly
 * @date 2022/8/24
 */

public class CSVDataProcessors {
    private List<CSVDataProcessor> processors = new ArrayList<>();
    private int consumerIndex;
    private Function<String, String> function;

    public CSVDataProcessors() {

    }

    public void addProcessor(CSVDataProcessor processor) {
        processors.add(processor);
    }

    public void registerFunction(int index, Function<String, String> function) {
        this.consumerIndex = index;
        this.function = function;
    }

    public CSVData process(CSVData data) {
        int maxSize = Objects.nonNull(data) ? processors.size() + 1 : processors.size();
        for (int i = 0; i < maxSize; i++) {
            if (i == consumerIndex) {
                String newVal = function.apply(data.getContent());
                data.setContent(newVal);
            } else {
                data = processors.get(i).process(data);
            }
        }
        return data;
    }
}
