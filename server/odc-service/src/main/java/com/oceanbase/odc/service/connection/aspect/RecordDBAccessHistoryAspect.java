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
package com.oceanbase.odc.service.connection.aspect;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections4.CollectionUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;
import com.oceanbase.odc.core.shared.constant.OrganizationType;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.flow.model.CreateFlowInstanceReq;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author: ysj
 * @Date: 2025/3/20 11:28
 * @Since: 4.3.4
 * @Description:
 */
@Aspect
@Component
@Slf4j
public class RecordDBAccessHistoryAspect {

    @Autowired
    private AuthenticationFacade authenticationFacade;
    @Autowired
    private DatabaseService databaseService;
    @Autowired
    private HttpServletRequest request;

    @Around("execution(* com.oceanbase.odc.server.web.controller.v2.FlowInstanceController.createFlowInstance(..))")
    public Object aroundCreateFlowInstance(ProceedingJoinPoint joinPoint) throws Throwable {
        return checkAndExecute(joinPoint, () -> {
            Object[] args = joinPoint.getArgs();
            for (Object arg : args) {
                if (arg instanceof CreateFlowInstanceReq) {
                    CreateFlowInstanceReq createFlowInstanceReq = (CreateFlowInstanceReq) arg;
                    if (createFlowInstanceReq.getDatabaseId() != null) {
                        databaseService
                                .recordDatabaseAccessHistory(Sets.newHashSet(createFlowInstanceReq.getDatabaseId()));
                    }
                }
            }
            return null;
        });
    }

    @Around("execution(* com.oceanbase.odc.server.web.controller.v2.ConnectSessionController.createSessionByDatabase(..))")
    public Object aroundCreateSessionByDatabase(ProceedingJoinPoint joinPoint) throws Throwable {
        return checkAndExecute(joinPoint, () -> {
            MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
            Object[] args = joinPoint.getArgs();
            String[] parameterNames = methodSignature.getParameterNames();
            Set<Long> databaseIds = new HashSet<>();
            for (int i = 0; i < parameterNames.length; i++) {
                if (parameterNames[i].equals("databaseId") && args[i] instanceof Long) {
                    databaseIds.add((Long) args[i]);
                } else if (parameterNames[i].equals("recordDbAccessHistory") && args[i] instanceof Boolean
                        && !((Boolean) args[i])) {
                    return null;
                }
            }
            if (CollectionUtils.isNotEmpty(databaseIds)) {
                databaseService.recordDatabaseAccessHistory(databaseIds);
            }
            return null;
        });
    }

    private Object checkAndExecute(ProceedingJoinPoint joinPoint, Supplier<Void> executingSupplier) throws Throwable {
        Object result = joinPoint.proceed();
        if (authenticationFacade.currentOrganization().getType() == OrganizationType.INDIVIDUAL) {
            return result;
        }
        String methodName = joinPoint.getSignature().getName();
        log.info("Start to record database access history with url={},methodName={}, method={}",
                request.getRequestURI(), methodName, request.getMethod());
        try {
            executingSupplier.get();
        } catch (Throwable e) {
            log.warn("Record database access history failed", e);
        }
        return result;
    }
}
