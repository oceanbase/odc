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
package com.oceanbase.odc.server.config;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yizhou.xw
 * @version : BeanCreateFailedAnalyzer.java, v 0.1 2021-04-01 20:30
 */
@Slf4j
public class BeanCreateFailedAnalyzer extends AbstractFailureAnalyzer<BeanCreationException> {
    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, BeanCreationException cause) {
        log.error("bean create failed: ", cause);
        String description = String.format("Bean create failed, beanName=%s, message=%s",
                cause.getBeanName(), cause.getMessage());
        String action = "please check configuration then restart odc-server";
        return new FailureAnalysis(description, action, cause);
    }
}
