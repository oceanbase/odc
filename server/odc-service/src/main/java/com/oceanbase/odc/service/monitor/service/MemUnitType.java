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
 * ODC资源水位监控中内存指标的单位
 *
 * @author yh263208
 * @date 2020-11-02 16:36
 * @since ODC_release_2.3
 */
public enum MemUnitType {
    /**
     * 字节单位
     */
    BYTE {
        @Override
        public int getTag() {
            return 0;
        }
    },
    /**
     * 千字节单位
     */
    K_BYTE {
        @Override
        public int getTag() {
            return 1;
        }
    },
    /**
     * 兆字节单位
     */
    M_BYTE {
        @Override
        public int getTag() {
            return 2;
        }
    },
    /**
     * 吉字节单位
     */
    G_BYTE {
        @Override
        public int getTag() {
            return 3;
        }
    };

    abstract public int getTag();
}
