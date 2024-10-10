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

import java.lang.management.ManagementFactory;
import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.service.common.response.OdcResult;
import com.sun.management.OperatingSystemMXBean;

/**
 * ODC中与监控相关的服务
 *
 * @author yh263208
 * @date 20202-11-02 16:26
 * @since ODC_release_2.3
 */
@Service
@SkipAuthorize("public readonly")
public class MonitorService {
    /**
     * 常量，用于查询操作系统名称
     */
    private static String OS_NAME_CONSTANT = "os.name";
    /**
     * 内存转换的进率
     */
    private static long MEM_RADIX = 1024;

    /**
     * 获取操作系统的名称
     *
     * @return 返回操作系统的名称
     */
    public String getOsName() {
        return System.getProperty(OS_NAME_CONSTANT);
    }

    /**
     * 获取可用的处理器数目
     *
     * @return 返回处理器数目
     */
    public int getAvailableProcessors() {
        Runtime rt = Runtime.getRuntime();
        return rt.availableProcessors();
    }

    /**
     * 获取当前的活跃线程数
     *
     * @return 返回线程数目
     */
    public int getActiveThreadCount() {
        ThreadGroup parentThread;
        int totalThread = 0;
        for (parentThread = Thread.currentThread().getThreadGroup(); parentThread.getParent() != null; parentThread =
                parentThread.getParent()) {
            totalThread = parentThread.activeCount();
        }
        return totalThread;
    }

    /**
     * 获取与JVM内存相关的监控指标
     *
     * @param destType 内存指标单位
     * @return 返回结果
     */
    public MetaMemory getJvmMemInfo(MemUnitType destType) {
        MetaMemory mem = new MetaMemory();
        Runtime rt = Runtime.getRuntime();
        mem.setMemActive(convertMemMetric(rt.totalMemory() - rt.freeMemory(), MemUnitType.BYTE, destType));
        mem.setMemFree(convertMemMetric(rt.freeMemory(), MemUnitType.BYTE, destType));
        mem.setMemMax(convertMemMetric(rt.maxMemory(), MemUnitType.BYTE, destType));
        mem.setMemTotal(convertMemMetric(rt.totalMemory(), MemUnitType.BYTE, destType));
        return mem;
    }

    /**
     * 获取与系统内存相关的监控指标
     *
     * @param destType 内存指标单位
     * @return 返回结果
     */
    public MetaMemory getPhysicalMemoryInfo(MemUnitType destType) {
        MetaMemory mem = new MetaMemory();
        OperatingSystemMXBean osmxb = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        long memFree = osmxb.getFreePhysicalMemorySize();
        long memTotal = osmxb.getTotalPhysicalMemorySize();

        mem.setMemActive(convertMemMetric(memTotal - memFree, MemUnitType.BYTE, destType));
        mem.setMemTotal(convertMemMetric(memTotal, MemUnitType.BYTE, destType));
        mem.setMemFree(convertMemMetric(memFree, MemUnitType.BYTE, destType));
        return mem;
    }

    /**
     * 获取ODC系统元信息
     *
     * @param type 内存水位的单位
     * @return 返回系统元信息
     */
    public OdcResult<MetaInfo> getOdcMetaInfo(MemUnitType type) {
        MetaInfo status = new MetaInfo();
        status.setAvailableProcessors(getAvailableProcessors());
        status.setJvmMemInfo(getJvmMemInfo(type));
        // 操作系统级内存情况查询
        status.setPhysicalMemInfo(getPhysicalMemoryInfo(type));
        // 获得线程总数
        status.setThreadCount(getActiveThreadCount());
        status.setOsEdition(getOsName());
        return new OdcResult<>(status);
    }

    /**
     * 进制转换方法
     *
     * @param srcMem 源内存大小
     * @param srcType 源内存大小单位
     * @param destType 目标内存大小单位
     * @return 返回转换后的内存
     */
    private long convertMemMetric(long srcMem, MemUnitType srcType, MemUnitType destType) {
        int typeDistance = srcType.getTag() - destType.getTag();
        if (typeDistance == 0) {
            return srcMem;
        } else if (typeDistance < 0) {
            BigDecimal srcMemValue = new BigDecimal(srcMem);
            while ((typeDistance++) < 0) {
                srcMemValue = srcMemValue.divide(new BigDecimal(MEM_RADIX));
            }
            return srcMemValue.longValue();
        } else {
            while ((typeDistance--) > 0) {
                srcMem *= MEM_RADIX;
            }
            return srcMem;
        }
    }
}
