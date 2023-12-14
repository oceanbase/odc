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

package com.oceanbase.odc.config.druid;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.druid.support.spring.stat.DruidStatInterceptor;

@Aspect
@Component
public class DruidMonitorAspect {

    private final DruidStatInterceptor druidStatInterceptor;

    @Autowired
    public DruidMonitorAspect(DruidStatInterceptor druidStatInterceptor) {
        this.druidStatInterceptor = druidStatInterceptor;
    }

    @Pointcut("within(@org.springframework.stereotype.Controller *) || within(@org.springframework.web.bind.annotation.RestController *)")
    public void controllerMethods() {}

    @Around("@annotation(DruidMonitor)")
    public Object aroundDruidMonitor(ProceedingJoinPoint joinPoint) throws Throwable {
        return druidStatInterceptor.invoke(new JointPointMethodInvocationAdapter(joinPoint));
    }

    @Around("controllerMethods()")
    public Object aroundDruidMonitorClass(ProceedingJoinPoint joinPoint) throws Throwable {
        return druidStatInterceptor.invoke(new JointPointMethodInvocationAdapter(joinPoint));
    }


}
