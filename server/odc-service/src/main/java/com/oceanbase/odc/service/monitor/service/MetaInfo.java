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
 * ODC的内存元数据信息，主要用于ODC的内存资源水位监控
 *
 * @author yh263208
 * @date 2020-11-02 16:02
 * @since ODC_release_2.3
 */
public class MetaInfo {
    /**
     * 与jvm相关的内存指标
     */
    private MetaMemory jvmMemInfo;
    /**
     * 与宿主机相关的指标
     */
    private MetaMemory physicalMemInfo;
    /**
     * 当前操作系统版本
     */
    private String osEdition;
    /**
     * 当前的线程总数
     */
    private long threadCount;
    /**
     * 可用的处理器个数
     */
    private int availableProcessors;

    public MetaMemory getJvmMemInfo() {
        return jvmMemInfo;
    }

    public void setJvmMemInfo(MetaMemory jvmMemInfo) {
        this.jvmMemInfo = jvmMemInfo;
    }

    public MetaMemory getPhysicalMemInfo() {
        return physicalMemInfo;
    }

    public void setPhysicalMemInfo(MetaMemory physicalMemInfo) {
        this.physicalMemInfo = physicalMemInfo;
    }

    public String getOsEdition() {
        return osEdition;
    }

    public void setOsEdition(String osEdition) {
        this.osEdition = osEdition;
    }

    public long getThreadCount() {
        return threadCount;
    }

    public void setThreadCount(long threadCount) {
        this.threadCount = threadCount;
    }

    public int getAvailableProcessors() {
        return availableProcessors;
    }

    public void setAvailableProcessors(int availableProcessors) {
        this.availableProcessors = availableProcessors;
    }
}
