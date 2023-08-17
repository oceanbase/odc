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
package com.oceanbase.odc.common.crypto;

/**
 * @author yizhou.xw
 * @version : BytesEncryptor.java, v 0.1 2020-04-27 14:48
 */
public interface BytesEncryptor {

    /**
     * encrypt
     * 
     * @param origin origin data
     * @return encrypted data
     */
    byte[] encrypt(byte[] origin);

    /**
     * decrypt
     * 
     * @param encrypted encrypted data
     * @return decrypted data
     */
    byte[] decrypt(byte[] encrypted);

}
