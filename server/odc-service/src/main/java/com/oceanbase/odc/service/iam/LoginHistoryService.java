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
package com.oceanbase.odc.service.iam;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.validation.constraints.NotEmpty;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.metadb.iam.LastSuccessLoginHistory;
import com.oceanbase.odc.metadb.iam.LoginHistoryEntity;
import com.oceanbase.odc.metadb.iam.LoginHistoryRepository;
import com.oceanbase.odc.service.iam.model.LoginHistory;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Validated
@SkipAuthorize("odc internal usage")
public class LoginHistoryService {

    private final Object lockObject = new Object();
    private final Map<Long, Date> userId2LastLoginTime = new HashMap<>();
    private final Integer maxLoginRecordTimeMinutes;
    @Autowired
    private LoginHistoryRepository loginHistoryRepository;

    public LoginHistoryService(
            @Value("${odc.security.max-login-record-time-minutes:15}") String maxLoginRecordTimeMinutes) {
        this.maxLoginRecordTimeMinutes = Integer.valueOf(maxLoginRecordTimeMinutes);
    }

    @Transactional(rollbackFor = Exception.class)
    public void record(LoginHistory loginHistory) {
        Long userId = loginHistory.getUserId();
        long current = System.currentTimeMillis();
        synchronized (lockObject) {
            Date loginTime = userId2LastLoginTime.get(userId);
            if (loginTime != null) {
                long intervalMin = TimeUnit.MINUTES.convert(current - loginTime.getTime(), TimeUnit.MILLISECONDS);
                if (intervalMin < maxLoginRecordTimeMinutes) {
                    return;
                }
            }
            userId2LastLoginTime.put(userId, new Date());
        }
        LoginHistoryEntity saved = loginHistoryRepository.saveAndFlush(loginHistory.toEntity());
        log.info("Login history saved, history={}", saved);
    }

    public Map<Long, LastSuccessLoginHistory> lastSuccessLoginHistoryByUserIds(@NotEmpty List<Long> userIds) {
        return loginHistoryRepository.lastSuccessLoginHistoryByUserIds(userIds).stream()
                .collect(Collectors.toMap(LastSuccessLoginHistory::getUserId, t -> t));
    }

}
