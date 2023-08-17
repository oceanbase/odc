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
package com.oceanbase.odc.core.datamasking.algorithm;

import com.oceanbase.odc.core.datamasking.data.Data;
import com.oceanbase.odc.core.datamasking.util.HashUtils;
import com.oceanbase.odc.core.datamasking.util.Sm3Util;

import lombok.extern.slf4j.Slf4j;

/**
 * @author wenniu.ly
 * @date 2022/8/27
 */

@Slf4j
public class Hash implements Algorithm {
    private HashType hashType;

    public Hash(String hashTypeStr) {
        try {
            this.hashType = HashType.valueOf(hashTypeStr);
        } catch (Exception e) {
            log.warn("Unsupported hash type: {}", hashTypeStr);
            throw new IllegalArgumentException(String.format("Unsupported hash type:  %s", hashTypeStr));
        }
    }

    @Override
    public Data mask(Data data) {
        String original = data.getValue();
        String result;
        switch (hashType) {
            case MD5:
                result = HashUtils.md5(original);
                break;
            case SHA256:
                result = HashUtils.sha256(original);
                break;
            case SHA512:
                result = HashUtils.sha512(original);
                break;
            case SM3:
                result = Sm3Util.encrypt(original);
                break;
            default:
                result = original;
        }
        data.setValue(result);
        return data;
    }

    @Override
    public AlgorithmEnum getType() {
        return AlgorithmEnum.HASH;
    }

    public enum HashType {
        MD5,
        SHA256,
        SHA512,
        SM3
    }
}
