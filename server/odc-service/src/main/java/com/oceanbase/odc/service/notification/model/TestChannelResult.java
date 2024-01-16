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
package com.oceanbase.odc.service.notification.model;

import lombok.Data;

/**
 * @author liuyizhuo.lyz
 * @date 2024/1/4
 */
@Data
public class TestChannelResult {

    private boolean active;
    private String errorMessage;

    public static TestChannelResult ofFail(String errorMessage) {
        TestChannelResult result = new TestChannelResult();
        result.setActive(false);
        result.setErrorMessage(errorMessage);
        return result;
    }

    public static TestChannelResult ofSuccess() {
        TestChannelResult result = new TestChannelResult();
        result.setActive(true);
        return result;
    }

}
