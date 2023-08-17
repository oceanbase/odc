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
package com.oceanbase.odc.core.shared.constant;

/**
 * 加密算法
 * 
 * @author yizhou.xw
 * @version : Cipher.java, v 0.1 2021-04-02 13:07
 */
public enum Cipher {
    /**
     * 不加密
     */
    RAW,

    /**
     * 一种更安全的密码 HASH 算法， refer more from https://en.wikipedia.org/wiki/Bcrypt
     */
    BCRYPT,

    /**
     * 带 salt 的 AES 对称加密，256位
     */
    AES256SALT,
}
