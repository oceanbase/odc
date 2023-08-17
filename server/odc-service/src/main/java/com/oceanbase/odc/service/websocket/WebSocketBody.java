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

/**
 * @author wenniu.ly
 * @date 2020/12/16
 */

@Getter
public class WebSocketBody implements Serializable {

    private String id;
    private String method;
    private WebSocketParams params;

    public WebSocketBody setId(String id) {
        this.id = id;
        return this;
    }

    public WebSocketBody setMethod(String method) {
        this.method = method;
        return this;
    }

    public WebSocketBody setParams(WebSocketParams params) {
        this.params = params;
        return this;
    }
}


