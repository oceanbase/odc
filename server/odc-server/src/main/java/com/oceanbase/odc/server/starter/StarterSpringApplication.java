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
package com.oceanbase.odc.server.starter;

import java.util.Arrays;
import java.util.HashSet;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

public class StarterSpringApplication extends SpringApplication {

    public StarterSpringApplication(Class<?>... primarySources) {
        super(primarySources);
    }

    public static ConfigurableApplicationContext run(Class<?> primarySource, String... args) {
        return run(new Class<?>[] {primarySource}, args);
    }

    public static ConfigurableApplicationContext run(Class<?>[] primarySources, String[] args) {
        return new StarterSpringApplication(primarySources).run(args);
    }

    @Override
    protected void logStartupProfileInfo(ConfigurableApplicationContext context) {
        super.logStartupProfileInfo(context);
        String[] activeProfiles = context.getEnvironment().getActiveProfiles();
        if (activeProfiles.length != 0) {
            Starters.load(new HashSet<>(Arrays.asList(activeProfiles)));
        }
    }

}
