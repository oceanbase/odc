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
package com.oceanbase.odc.service.feature;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.stereotype.Service;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.common.util.VersionUtils;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.metadb.feature.VersionDiffConfigDAO;
import com.oceanbase.odc.service.config.SystemConfigService;
import com.oceanbase.odc.service.config.model.Configuration;
import com.oceanbase.odc.service.feature.model.DataTypeUnit;
import com.oceanbase.odc.service.feature.model.VersionDiffConfig;

import lombok.Data;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@SkipAuthorize("inside connect session")
public class VersionDiffConfigService {
    private static final String SUPPORT_PREFIX = "support";
    private static final String SUPPORT_PROCEDURE = "support_procedure";
    private static final String SUPPORT_FUNCTION = "support_function";
    private static final String SUPPORT_PL_DEBUG = "support_pl_debug";
    private static final String COLUMN_DATA_TYPE = "column_data_type";
    private static final String ARM_OB_PREFIX = "aarch64";
    private static final String ARM_OB_SUPPORT_PL_DEBUG_MIN_VERSION = "3.2.3";

    @Autowired
    private VersionDiffConfigDAO versionDiffConfigDAO;

    @Autowired
    private SystemConfigService systemConfigService;

    public List<DataTypeUnit> getDatatypeList(@NonNull ConnectionSession connectionSession) {
        return getDatatypeList(connectionSession, COLUMN_DATA_TYPE);
    }

    public List<DataTypeUnit> getDatatypeList(@NonNull ConnectionSession connectionSession, String configKey) {
        List<DataTypeUnit> datatypes = new ArrayList<>();

        VersionDiffConfig config = new VersionDiffConfig();
        config.setConfigKey(configKey);
        config.setDbMode(getDbMode(connectionSession));
        List<VersionDiffConfig> list = this.versionDiffConfigDAO.query(config);

        String currentVersion = ConnectionSessionUtil.getVersion(connectionSession);
        for (VersionDiffConfig diffConfig : list) {
            if (VersionUtils.isGreaterThanOrEqualsTo(currentVersion, diffConfig.getMinVersion())) {
                String configValues = diffConfig.getConfigValue();
                String[] arrays = configValues.split(",");
                for (String unit : arrays) {
                    String[] segs = unit.split(":");
                    DataTypeUnit dataTypeUnit = new DataTypeUnit();
                    dataTypeUnit.setDatabaseType(segs[0].trim());
                    dataTypeUnit.setShowType(segs[1].trim());
                    // oracle mode，date类型默认展示DATE时间组件，如果是有时分秒格式，则展示TIMESTAMP组件
                    if (connectionSession.getDialectType() == DialectType.OB_ORACLE
                            && dataTypeUnit.getShowType().equalsIgnoreCase("DATE")
                            && this.isHourFormat(connectionSession)) {
                        dataTypeUnit.setShowType("TIMESTAMP");
                    }
                    datatypes.add(dataTypeUnit);
                }
            }
        }
        return datatypes;
    }

    public List<OBSupport> getSupportFeatures(ConnectionSession connectionSession) {
        List<OBSupport> obSupportList = new ArrayList<>();
        VersionDiffConfig config = new VersionDiffConfig();
        config.setDbMode(getDbMode(connectionSession));
        List<VersionDiffConfig> list = this.versionDiffConfigDAO.query(config);
        String currentVersion = ConnectionSessionUtil.getVersion(connectionSession);
        boolean supportsProcedure =
                AllFeatures.getByConnectType(connectionSession.getConnectType()).supportsProcedure();
        List<Configuration> systemConfigs = systemConfigService.listAll();
        for (VersionDiffConfig diffConfig : list) {
            String configKey = diffConfig.getConfigKey().toLowerCase();
            if (configKey.startsWith(SUPPORT_PREFIX)) {
                OBSupport obSupport = new OBSupport();
                obSupportList.add(obSupport);
                obSupport.setSupportType(configKey);
                if (systemConfigs.stream().anyMatch(configuration -> configuration.getKey().equalsIgnoreCase(configKey)
                        && "false".equalsIgnoreCase(configuration.getValue()))) {
                    obSupport.setSupport(false);
                    continue;
                }
                if (VersionUtils.isGreaterThanOrEqualsTo(currentVersion, diffConfig.getMinVersion())) {
                    String configValue = diffConfig.getConfigValue();
                    if ("true".equalsIgnoreCase(configValue)) {
                        obSupport.setSupport(true);
                    }
                }
                // fix support_procedure for oceanbase-ce version
                if ((SUPPORT_PROCEDURE.equalsIgnoreCase(configKey) || SUPPORT_FUNCTION.equalsIgnoreCase(configKey))
                        && DialectType.OB_MYSQL == connectionSession.getDialectType()
                        && obSupport.isSupport()) {
                    if (!supportsProcedure) {
                        obSupport.setSupport(false);
                    }
                }
                if (SUPPORT_PL_DEBUG.equalsIgnoreCase(configKey) && !isPLDebugSupport(connectionSession)) {
                    obSupport.setSupport(false);
                }
            }
        }
        return obSupportList;
    }

    private boolean isHourFormat(ConnectionSession connectionSession) {
        JdbcOperations jdbcOperations =
                connectionSession.getSyncJdbcExecutor(ConnectionSessionConstants.CONSOLE_DS_KEY);
        try {
            String sql = "show variables like 'nls_date_format'";
            List<String> lines = jdbcOperations.query(sql, (rs, rowNum) -> rs.getString(2));
            String format = lines.get(0);
            if (format.toLowerCase().contains("hh")) {
                return true;
            }
        } catch (Exception e) {
            log.warn("failed to query nls_date_format, reason={}", e.getMessage());
        }
        return false;
    }

    private boolean isPLDebugSupport(ConnectionSession connectionSession) {
        if (DialectType.OB_ORACLE != connectionSession.getDialectType()) {
            return false;
        }
        String version = ConnectionSessionUtil.getVersion(connectionSession);
        String architecture = ConnectionSessionUtil.getArchitecture(connectionSession);
        if (StringUtils.startsWith(architecture, ARM_OB_PREFIX)
                && !VersionUtils.isGreaterThanOrEqualsTo(version, ARM_OB_SUPPORT_PL_DEBUG_MIN_VERSION)) {
            log.info("Current ob version={} does not reach the minimum version={} for PLDebug under aarch observer",
                    version, ARM_OB_SUPPORT_PL_DEBUG_MIN_VERSION);
            return false;
        }
        return true;
    }

    private String getDbMode(ConnectionSession connectionSession) {
        ConnectType connectType = connectionSession.getConnectType();
        if (connectType.isODPSharding()) {
            return connectType.name();
        } else {
            return connectType.getDialectType().name();
        }
    }

    @Data
    public static class OBSupport {
        private String supportType;
        private boolean support;
    }

}
