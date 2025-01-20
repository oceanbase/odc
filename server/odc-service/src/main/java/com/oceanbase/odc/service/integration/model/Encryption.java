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
package com.oceanbase.odc.service.integration.model;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.google.common.base.MoreObjects;
import com.oceanbase.odc.common.json.SensitiveInput;
import com.oceanbase.odc.core.shared.PreConditions;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author gaoda.xy
 * @date 2023/3/27 11:54
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Encryption {

    @NotNull
    private Boolean enabled;

    private EncryptionAlgorithm algorithm;

    @SensitiveInput
    @JsonProperty(access = Access.WRITE_ONLY)
    private String secret;


    public static Encryption empty() {
        return Encryption.builder()
                .enabled(false)
                .algorithm(EncryptionAlgorithm.RAW)
                .secret(null).build();
    }

    public void check() {
        if (!Boolean.TRUE.equals(enabled)) {
            return;
        }
        if (getAlgorithm() != EncryptionAlgorithm.RAW) {
            PreConditions.notNull(secret, "lack of secret");
        }
    }

    public EncryptionAlgorithm getAlgorithm() {
        return MoreObjects.firstNonNull(algorithm, EncryptionAlgorithm.RAW);
    }


    public enum EncryptionAlgorithm {
        /**
         * No encryption. Especially， for sso integration, raw means that each type sso handle the
         * encryption and decryption process themselves, Encryption#secret just store it.
         */
        RAW,

        /**
         * AES192+BASE64 for 4A integration scenario
         */
        AES192_BASE64_4A,

        /**
         * AES256+BASE64
         */
        AES256_BASE64
    }
}
