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
package com.oceanbase.odc.service.task.supervisor;

import java.net.ServerSocket;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.Function;

import lombok.Setter;

/**
 * detect unused port for process in range
 * 
 * @author longpeng.zlp
 * @date 2024/10/25 16:25
 */
public class PortDetector {
    private static final PortDetector PORT_DETECTOR = new PortDetector();
    @Setter
    private int maxPort;
    @Setter
    private int minPort;
    @Setter
    private int expiredMs;
    @Setter
    private Function<Integer, Boolean> portDetector;

    private PriorityQueue<AllocatedPortInfo> allocatedPortInfos = new PriorityQueue<>((ap1, ap2) -> {
        return Long.compare(ap1.getAllocatedTime(), ap2.getAllocatedTime());
    });
    private Set<Integer> allocatedSets = new HashSet<>();

    protected PortDetector() {
        maxPort = 10240;
        minPort = 9000;
        // 10s
        expiredMs = 10000;
        portDetector = PortDetector::portInUse;
    }

    public synchronized int getPort() {
        long currentTimeMS = System.currentTimeMillis();
        // expire allocated tasks port
        while (!allocatedPortInfos.isEmpty()
                && (currentTimeMS - allocatedPortInfos.peek().getAllocatedTime()) >= expiredMs) {
            AllocatedPortInfo allocatedPortInfo = allocatedPortInfos.poll();
            allocatedSets.remove(allocatedPortInfo.getPort());
        }
        // go through and find available port
        for (int i = minPort; i <= maxPort; ++i) {
            if (allocatedSets.contains(i)) {
                continue;
            }
            if (portInUse(i)) {
                continue;
            }
            allocatedSets.add(i);
            allocatedPortInfos.add(new AllocatedPortInfo(i, System.currentTimeMillis()));
            return i;
        }
        throw new RuntimeException("port allocate failed");
    }

    public static boolean portInUse(int port) {
        ServerSocket socketServer = null;
        try {
            socketServer = new ServerSocket(port);
        } catch (Throwable e) {
            return true;
        } finally {
            if (null != socketServer) {
                try {
                    socketServer.close();
                } catch (Throwable e) {
                }
            }
        }
        return false;
    }

    public static PortDetector getInstance() {
        return PORT_DETECTOR;
    }

    private static final class AllocatedPortInfo {
        private final int port;
        private final long allocatedTime;

        private AllocatedPortInfo(int port, long allocatedTime) {
            this.port = port;
            this.allocatedTime = allocatedTime;
        }

        public int getPort() {
            return port;
        }

        public long getAllocatedTime() {
            return allocatedTime;
        }
    }
}
