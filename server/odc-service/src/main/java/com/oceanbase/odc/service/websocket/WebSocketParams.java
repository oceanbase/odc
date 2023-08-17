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
package com.oceanbase.odc.service.websocket;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;

/**
 * @author wenniu.ly
 * @date 2020/12/16
 */

@Getter
@Setter
public class WebSocketParams implements Serializable {

    private String data;

    public WebSocketParams() {

    }

    public WebSocketParams(String data) {
        this.data = data;
    }
}
