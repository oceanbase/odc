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

    /**
     * 记录登录历史
     *
     * @param loginHistory 登录历史对象
     * @return 是否记录成功
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean record(LoginHistory loginHistory) {
        Long userId = loginHistory.getUserId();
        long current = System.currentTimeMillis();
        // 使用锁对象进行同步，保证线程安全
        synchronized (lockObject) {
            // 获取用户上一次登录的时间
            Date loginTime = userId2LastLoginTime.get(userId);
            if (loginTime != null) {
                // 计算当前时间和上一次登录时间的时间差（分钟）
                long intervalMin = TimeUnit.MINUTES.convert(current - loginTime.getTime(), TimeUnit.MILLISECONDS);
                // 如果时间差小于最大登录记录时间（minutes），则不记录当前登录历史
                if (intervalMin < maxLoginRecordTimeMinutes) {
                    return false;
                }
            }
            // 更新用户的上一次登录时间
            userId2LastLoginTime.put(userId, new Date());
        }
        // 将登录历史对象转换为实体对象并保存到数据库中
        LoginHistoryEntity saved = loginHistoryRepository.saveAndFlush(loginHistory.toEntity());
        // 记录日志
        log.info("Login history saved, history={}", saved);
        return true;
    }

    public Map<Long, LastSuccessLoginHistory> lastSuccessLoginHistoryByUserIds(@NotEmpty List<Long> userIds) {
        return loginHistoryRepository.lastSuccessLoginHistoryByUserIds(userIds).stream()
                .collect(Collectors.toMap(LastSuccessLoginHistory::getUserId, t -> t));
    }

}
