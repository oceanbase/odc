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

import java.io.IOException;
import java.net.ServerSocket;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author longpeng.zlp
 * @date 2025/1/6 15:27
 */
public class PortDetectorTest {
    @Test
    public void testPortInUse() throws IOException {
        PortDetector portDetector = new PortDetector();
        int port = portDetector.getPort();
        Assert.assertFalse(PortDetector.portInUse(port));
        try (ServerSocket socketServer = new ServerSocket(port)) {
            Assert.assertTrue(PortDetector.portInUse(port));
        }
        Assert.assertNotEquals(port, portDetector.getPort());
    }

    @Test(expected = RuntimeException.class)
    public void testPortAllocateFailed() {
        PortDetector portDetector = new PortDetector();
        portDetector.setPortDetector((p) -> false);
        portDetector.setMinPort(9999);
        portDetector.setMaxPort(10000);
        int i = 0;
        while (i++ < 1000) {
            portDetector.getPort();
        }
    }

    @Test
    public void testPortAllocateExpired() {
        PortDetector portDetector = new PortDetector();
        portDetector.setPortDetector((p) -> false);
        portDetector.setMinPort(9999);
        portDetector.setMaxPort(10005);
        portDetector.setExpiredMs(0);
        int i = 0;
        while (i++ < 1000) {
            portDetector.getPort();
        }
    }
}
