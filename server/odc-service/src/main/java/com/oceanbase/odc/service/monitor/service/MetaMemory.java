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
package com.oceanbase.odc.service.monitor.service;

/**
 * 用于封装ODC资源水位监控中与内存相关的指标
 *
 * @author yh263208
 * @date 2020-11-02 16:32
 * @since ODC_release_2.3
 */
public class MetaMemory {
    /**
     * 当前的总内存
     */
    private long memTotal;
    /**
     * 能获取到的最大内存，通常比memTotal要大
     */
    private long memMax;
    /**
     * 空闲的内存
     */
    private long memFree;
    /**
     * 使用中的内存
     */
    private long memActive;

    public long getMemTotal() {
        return memTotal;
    }

    public void setMemTotal(long memTotal) {
        this.memTotal = memTotal;
    }

    public long getMemMax() {
        return memMax;
    }

    public void setMemMax(long memMax) {
        this.memMax = memMax;
    }

    public long getMemFree() {
        return memFree;
    }

    public void setMemFree(long memFree) {
        this.memFree = memFree;
    }

    public long getMemActive() {
        return memActive;
    }

    public void setMemActive(long memActive) {
        this.memActive = memActive;
    }
}
