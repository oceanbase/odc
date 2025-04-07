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

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @author keyang
 * @date 2025/04/02
 * @since 4.3.3
 */
public class LocalHostLoader {
    public static final LocalHostLoader INSTANCE = new LocalHostLoader();
    private volatile InetAddress localAddress = null;

    private LocalHostLoader() {}

    public InetAddress getLocalHost() throws UnknownHostException {
        if (localAddress == null) {
            synchronized (this) {
                if (localAddress == null) {
                    localAddress = InetAddress.getLocalHost();
                }
            }
        }
        return localAddress;
    }
}
