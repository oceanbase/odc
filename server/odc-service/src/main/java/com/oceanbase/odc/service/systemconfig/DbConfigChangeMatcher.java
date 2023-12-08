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
package com.oceanbase.odc.service.systemconfig;

import static com.oceanbase.odc.core.alarm.AlarmEventNames.SYSTEM_CONFIG_CHANGED;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.ListUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.oceanbase.odc.core.alarm.AlarmUtils;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.metadb.config.SystemConfigDAO;
import com.oceanbase.odc.metadb.config.SystemConfigEntity;
import com.oceanbase.odc.service.config.model.Configuration;
import com.oceanbase.odc.service.config.util.OrganizationConfigUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@SkipAuthorize
public class DbConfigChangeMatcher extends SystemConfigRefreshMatcher implements InitializingBean {

    @Autowired
    private SystemConfigDAO systemConfigDAO;

    private List<Configuration> lastVersion = new ArrayList<>();

    private List<Configuration> currentVersion = new ArrayList<>();

    @SkipAuthorize("public readonly resource")
    public List<Configuration> listAll() {
        return queryByKeyPrefix("");
    }

    @SkipAuthorize("odc internal usage")
    public List<Configuration> queryByKeyPrefix(String keyPrefix) {
        List<SystemConfigEntity> configEntities = systemConfigDAO.queryByKeyPrefix(keyPrefix);
        return OrganizationConfigUtil.convertDO2DTO(configEntities);
    }

    @Override
    public boolean needRefresh() {
        this.currentVersion = listAll();
        boolean dbConfigChange = !ListUtils.isEqualList(currentVersion, lastVersion);
        if (dbConfigChange) {
            SetView<Configuration> difference = Sets.difference(new HashSet<>(currentVersion),
                    new HashSet<>(lastVersion));
            Set<String> differenceKey = difference.stream().map(Configuration::getKey).collect(Collectors.toSet());
            AlarmUtils.info(SYSTEM_CONFIG_CHANGED, differenceKey.toString());
            log.info("db config change, difference key:" + differenceKey);
        }
        return dbConfigChange;
    }

    @Override
    protected void onRefreshed() {
        if (!ListUtils.isEqualList(currentVersion, lastVersion)) {
            this.lastVersion = this.currentVersion;
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.lastVersion = listAll();
        log.info("initial lastVersion, configurationsCount={}", this.lastVersion.size());
    }

}
