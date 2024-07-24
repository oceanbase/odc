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
package com.oceanbase.odc.service.monitor;

import java.io.IOException;
import java.util.List;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

public class ActuatorEndpointFilter extends OncePerRequestFilter implements InitializingBean {

    private final AntPathMatcher antPathMatcher = new AntPathMatcher();
    private final MonitorProperties properties;
    private final List<MeterClear> meterClears;

    public ActuatorEndpointFilter(MonitorProperties properties, List<MeterClear> meterClears) {
        this.properties = properties;
        this.meterClears = meterClears;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!isActuatorEndpoint(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            clear();
        }

    }

    private boolean isActuatorEndpoint(String requestURI) {
        List<String> endpointPath = properties.getActuator().getEndpointPath();
        for (String endpoint : endpointPath) {
            if (antPathMatcher.match(endpoint, requestURI)) {
                return true;
            }
        }
        return false;
    }

    private void clear() {
        meterClears.forEach(MeterClear::clearAfterPull);
    }
}
