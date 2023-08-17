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
package com.oceanbase.odc.core.datamasking.algorithm.pseudonymization;

import java.util.Random;

import com.oceanbase.odc.core.datamasking.algorithm.Algorithm;
import com.oceanbase.odc.core.datamasking.algorithm.AlgorithmEnum;
import com.oceanbase.odc.core.datamasking.data.Data;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author wenniu.ly
 * @date 2022/8/27
 */

@Slf4j
public class Pseudonymization implements Algorithm {
    private final Random rand = new Random(System.currentTimeMillis());
    private final Alphabet alphabet;
    @Setter
    private String prefix = "";
    @Setter
    private String suffix = "";

    public Pseudonymization(String characters) {
        try {
            this.alphabet = new Alphabet(characters);
        } catch (Exception e) {
            log.warn("Unsupported pseudo characters: {}", characters);
            throw new IllegalArgumentException(String.format("Unsupported pseudo characters: %s", characters));
        }
    }

    public Pseudonymization(Alphabet alphabet) {
        this.alphabet = alphabet;
    }

    @Override
    public Data mask(Data data) {
        String currentValue = String.valueOf(data.getValue());
        String pseudonym = generateNewPseudonym(currentValue);
        data.setValue(pseudonym);
        return data;
    }

    @Override
    public AlgorithmEnum getType() {
        return AlgorithmEnum.PSEUDO;
    }

    /**
     * reseed the random number generator with the current system time
     */
    public void randomize() {
        rand.setSeed(System.currentTimeMillis());
    }

    private String generateNewPseudonym(String currentValue) {
        StringBuilder result = new StringBuilder();
        result.append(prefix);
        for (int i = 0; i < currentValue.length(); i++) {
            if (alphabet.getSymbols().contains(currentValue.charAt(i))) {
                result.append(alphabet.getSymbol(rand.nextInt(alphabet.length())));
            } else {
                result.append(currentValue.charAt(i));
            }
        }
        result.append(suffix);
        return result.toString();
    }
}
