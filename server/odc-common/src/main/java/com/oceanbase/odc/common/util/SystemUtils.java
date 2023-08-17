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

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @author yizhou.xw
 * @version : SystemUtils.java, v 0.1 2021-02-20 15:08
 */
@Slf4j
public abstract class SystemUtils {
    private static final String UNIX_PROCESS_CLASS_NAME = "java.lang.UNIXProcess";
    private static final String PID_FIELD_NAME = "pid";
    private static final String LOCAL_HOST = "127.0.0.1";
    private static final String ANY_HOST = "0.0.0.0";
    private static final String DEFAULT_HOSTNAME = "localhost";
    private static final Pattern IP_PATTERN =
            Pattern.compile("^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");

    private static volatile InetAddress LOCAL_ADDRESS = null;
    private static volatile String HOSTNAME = null;

    private SystemUtils() {}

    public static String getHostName() {
        if (HOSTNAME != null) {
            return HOSTNAME;
        }
        String hostName = innerGetHostName();
        HOSTNAME = hostName;
        return hostName;
    }

    public static Long getPid() {
        // value like pid@hostname
        String runtimeBeanName = ManagementFactory.getRuntimeMXBean().getName();
        int index = runtimeBeanName.indexOf('@');
        if (index < 0) {
            log.warn("Can not resolve RuntimeMXBean name: {}", runtimeBeanName);
            return 0L;
        }
        return Long.parseLong(runtimeBeanName.substring(0, index));
    }

    public static Long getJVMStartTime() {
        return ManagementFactory.getRuntimeMXBean().getStartTime();
    }

    public static String getLocalIpAddress() {
        InetAddress address = getLocalAddress();
        return address == null ? LOCAL_HOST : address.getHostAddress();
    }

    public static String getEnvOrProperty(@NonNull String propertyName) {
        String content = System.getenv(propertyName);
        if (content != null) {
            return content;
        }
        return System.getProperty(propertyName);
    }

    public static Map<String, Object> getSystemMemoryInfo() {
        Map<String, Object> metric2Messages = new HashMap<>();

        MemoryUsage heapMemoryUsage = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        metric2Messages.putIfAbsent("heap", heapMemoryUsage.toString());

        MemoryUsage nonHeapMemoryUsage = ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage();
        metric2Messages.putIfAbsent("nonHeap", nonHeapMemoryUsage.toString());

        long startTime = ManagementFactory.getRuntimeMXBean().getStartTime();
        metric2Messages.putIfAbsent("startTime", new Date(startTime));

        List<GarbageCollectorMXBean> beanList = ManagementFactory.getGarbageCollectorMXBeans();
        for (GarbageCollectorMXBean bean : beanList) {
            metric2Messages.putIfAbsent("garbageCollector", bean.getCollectionCount());
        }
        return metric2Messages;
    }

    public static Map<String, String> getSystemEnv() {
        return System.getenv();
    }

    public static Properties getSystemProperties() {
        return System.getProperties();
    }

    public static int availableProcessors() {
        return Runtime.getRuntime().availableProcessors();
    }

    public static boolean isOnLinux() {
        String osName = System.getProperty("os.name");
        if (StringUtils.isNotBlank(osName)) {
            return osName.toLowerCase().contains("linux");
        }
        return false;
    }

    public static boolean isOnWindows() {
        String osName = System.getProperty("os.name");
        if (StringUtils.isNotBlank(osName)) {
            return osName.toLowerCase().contains("windows");
        }
        return false;
    }

    public static long getProcessPid(Process process) {
        long pid = -1;
        try {
            if (process.getClass().getName().equals(UNIX_PROCESS_CLASS_NAME)) {
                Field f = process.getClass().getDeclaredField(PID_FIELD_NAME);
                f.setAccessible(true);
                pid = f.getLong(process);
                f.setAccessible(false);
            }
        } catch (Exception e) {
            pid = -1;
        }
        return pid;
    }

    private static String innerGetHostName() {
        String hostName = null;
        try {
            hostName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            log.warn("get hostname from InetAddress failed", e);
        }
        if (StringUtils.isEmpty(hostName)) {
            log.warn("empty hostname from InetAddress, try get from system properties");
            hostName = org.apache.commons.lang3.SystemUtils.getHostName();
        }
        if (StringUtils.isEmpty(hostName)) {
            log.warn("empty hostname from system properties, use localhost instead");
            hostName = DEFAULT_HOSTNAME;
        }
        return hostName;
    }

    private static InetAddress getLocalAddress() {
        if (LOCAL_ADDRESS != null) {
            return LOCAL_ADDRESS;
        }
        InetAddress localAddress = innerGetLocalAddress();
        LOCAL_ADDRESS = localAddress;
        return localAddress;
    }

    private static InetAddress innerGetLocalAddress() {
        InetAddress localAddress = null;
        try {
            localAddress = InetAddress.getLocalHost();
            if (isValidAddress(localAddress)) {
                return localAddress;
            }
        } catch (Throwable e) {
            log.warn("Failed to retrieving ip address {}", e.getMessage());
        }
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            if (interfaces != null) {
                while (interfaces.hasMoreElements()) {
                    try {
                        NetworkInterface network = interfaces.nextElement();
                        Enumeration<InetAddress> addresses = network.getInetAddresses();
                        while (addresses.hasMoreElements()) {
                            try {
                                InetAddress address = addresses.nextElement();
                                if (isValidAddress(address)) {
                                    return address;
                                }
                            } catch (Throwable e) {
                                log.warn("Failed to retrieving ip address, " + e.getMessage(), e);
                            }
                        }
                    } catch (Throwable e) {
                        log.warn("Failed to retrieving ip address, " + e.getMessage(), e);
                    }
                }
            }
        } catch (Throwable e) {
            log.warn("Failed to retrieving ip address, " + e.getMessage(), e);
        }
        log.warn("Could not get local host ip address, will use 127.0.0.1 instead.");
        return localAddress;
    }

    private static boolean isValidAddress(InetAddress address) {
        if (address == null || address.isLoopbackAddress()) {
            return false;
        }
        String name = address.getHostAddress();
        if (name == null) {
            return false;
        }
        if (ANY_HOST.equals(name)) {
            return false;
        }
        if (LOCAL_HOST.equals(name)) {
            return false;
        }
        return IP_PATTERN.matcher(name).matches();
    }

}
