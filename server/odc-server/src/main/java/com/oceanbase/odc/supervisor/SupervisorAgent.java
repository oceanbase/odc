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
package com.oceanbase.odc.supervisor;

import org.apache.hadoop.classification.InterfaceStability.Evolving;

import com.oceanbase.odc.common.BootAgentUtil;
import com.oceanbase.odc.common.util.SystemUtils;
import com.oceanbase.odc.server.module.Modules;
import com.oceanbase.odc.service.task.constants.JobEnvKeyConstants;
import com.oceanbase.odc.supervisor.runtime.SupervisorApplication;

import lombok.extern.slf4j.Slf4j;
import sun.misc.Signal;
import sun.misc.SignalHandler;

/**
 * @author longpeng.zlp
 * @date 2024/11/26 15:43
 */
@Slf4j
@Evolving
public class SupervisorAgent {
    public static void main(String[] args) {
        BootAgentUtil.setLogPathSysProperty();
        // set log4j xml
        BootAgentUtil.setLog4JConfigXml(SupervisorAgent.class.getClassLoader(), "log4j2-supervisor.xml");
        log.info("Supervisor agent started");
        SupervisorApplication supervisorApplication = null;
        try {
            Modules.load();
            // config it
            supervisorApplication = new SupervisorApplication(getListenPort());
            supervisorApplication.start(args);
            // register signal handler
            Signal.handle(new Signal("INT"), new DefaultSignalHandler(supervisorApplication));
            Signal.handle(new Signal("TERM"), new DefaultSignalHandler(supervisorApplication));
            // wait stop
            supervisorApplication.waitStop();
        } catch (Throwable e) {
            log.error("Supervisor agent stopped", e);
        } finally {
            if (null != supervisorApplication) {
                supervisorApplication.stop();
            }
        }
        log.info("Supervisor agent stopped.");
    }

    public static int getListenPort() {
        String supervisorListenPort = SystemUtils.getEnvOrProperty(JobEnvKeyConstants.ODC_SUPERVISOR_LISTEN_PORT);
        if (null == supervisorListenPort) {
            throw new RuntimeException("supervisor endpoint must be given");
        }
        return Integer.valueOf(supervisorListenPort);
    }

    private static final class DefaultSignalHandler implements SignalHandler {
        private final SupervisorApplication supervisorApplication;

        public DefaultSignalHandler(SupervisorApplication supervisorApplication) {
            this.supervisorApplication = supervisorApplication;
        }

        @Override
        public void handle(Signal signal) {
            if (null != supervisorApplication) {
                log.info("receive signal [{}]-[{}], try stop supervisor", signal.getName(), signal.getNumber());
                supervisorApplication.stop();
            } else {
                log.info("receive signal [{}]-[{}], supervisor not init yet", signal.getName(), signal.getNumber());
            }
        }
    }
}
