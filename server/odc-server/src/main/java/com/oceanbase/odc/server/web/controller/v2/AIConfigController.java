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
package com.oceanbase.odc.server.web.controller.v2;

import java.sql.SQLException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.oceanbase.odc.service.llm.AIConfigService;
import com.oceanbase.odc.service.llm.model.AIConfig;

@RestController
@RequestMapping("/api/v2/integration/ai")
public class AIConfigController {

    @Autowired
    private AIConfigService aiConfigService;

    @GetMapping("/config")
    public ResponseEntity<AIConfig> getAIConfig() {
        return ResponseEntity.ok(aiConfigService.getAIConfig());
    }

    @PostMapping("/config")
    public ResponseEntity<AIConfig> setAIConfig(@RequestBody AIConfig aiConfig) throws SQLException {
        return ResponseEntity.ok(aiConfigService.setAIConfig(aiConfig));
    }

}
