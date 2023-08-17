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
package com.oceanbase.odc.service.session;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.exception.UnexpectedException;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.lab.ResourceService;
import com.oceanbase.odc.service.lab.model.LabProperties;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@SkipAuthorize("readonly service")
public class SessionLimitService {

    private final AtomicInteger currentPosition = new AtomicInteger(0);
    private final Queue<String> waitQueue = new ConcurrentLinkedQueue<>();
    private final Map<String, Integer> queueIndexMap = new ConcurrentHashMap<>();
    private final Map<String, Long> allowCreateSessionUserMap = new ConcurrentHashMap<>();
    private final Object lock = new Object();
    @Autowired
    private SessionProperties sessionProperties;

    @Autowired
    private LabProperties labProperties;

    @Autowired
    private AuthenticationFacade authenticationFacade;

    @Autowired(required = false)
    private ResourceService resourceService;

    private volatile int lastModifiedPosition = -1;

    private Map<String, AtomicInteger> userId2SessionCountMap = new ConcurrentHashMap<>();

    public boolean isResourceAvailable() {
        long userMaxCount = sessionProperties.getUserMaxCount();
        return userMaxCount <= 0 || allowCreateSessionUserMap.size() < userMaxCount;
    }

    /**
     * 是否允许创建session
     * 
     * @param userId
     * @return
     */
    public boolean allowCreateSession(String userId) {
        allowCreateSessionUserMap.computeIfPresent(userId, (k, v) -> System.currentTimeMillis());
        if (allowCreateSessionUserMap.containsKey(userId)) {
            return true;
        }
        synchronized (lock) {
            if (isResourceAvailable() && waitQueue.isEmpty()) {
                updateTotalUserCountMap(userId);
                return true;
            }
            // 当前有用户排队，或userMap已达上限，入队列并拒绝创建
            addIfAbsent(userId);
            log.info("user {} request rejected, total user count: {}, current waiting number: {}", userId,
                    allowCreateSessionUserMap.size(), queryWaitingNum(userId));
            return false;
        }
    }

    /**
     * 获取当前排队状态
     * 
     * @return
     */
    public SessionLimitResp getUserLineupStatus() {
        if (allowCreateSession(authenticationFacade.currentUserIdStr())) {
            if (null != resourceService) {
                resourceService.createResource(authenticationFacade.currentUser());
            }
            return new SessionLimitResp(true, 0, 0);
        } else {
            int waitNum = queryWaitingNum(authenticationFacade.currentUserIdStr());
            return new SessionLimitResp(false, waitNum, waitNum);
        }
    }

    /**
     * 用户创建Session，记录最后操作时间
     * 
     * @param userId
     */
    public void updateTotalUserCountMap(String userId) {
        allowCreateSessionUserMap.put(userId, System.currentTimeMillis());
    }

    /**
     * 移除过期User
     */
    public void revokeInactiveUser() {
        List<String> inactiveUserIdList = getInactiveUserIdList();
        inactiveUserIdList.forEach(userId -> {
            if (allowCreateSessionUserMap.containsKey(userId)
                    && System.currentTimeMillis() - allowCreateSessionUserMap.get(userId) >= labProperties
                            .getUserExpiredTime()) {
                allowCreateSessionUserMap.remove(userId);
                log.info("revoke inactive user successfully, userId={}", userId);
            }
        });
    }

    /**
     * 移除指定User，若不存在该user则无操作
     */
    public void revokeUser(String userId) {
        allowCreateSessionUserMap.remove(userId);
    }

    /**
     * 资源空闲时，从队列里拉取user至map中
     */
    public void pollUserFromWaitQueue() {
        while (isResourceAvailable() && !waitQueue.isEmpty()) {
            String head = waitQueue.poll();
            log.info("allowCreateSessionUserMap is available, add user userId={} into map", head);
            lastModifiedPosition = queueIndexMap.remove(head);
            updateTotalUserCountMap(head);
        }
    }

    public int queryWaitingNum(String userId) {
        return queueIndexMap.get(userId) - lastModifiedPosition;
    }

    public int incrementSessionCount(String userId) {
        PreConditions.notNull(userId, "userId");
        return this.userId2SessionCountMap.computeIfAbsent(userId, t -> new AtomicInteger(0)).incrementAndGet();
    }

    public void decrementSessionCount(String userId) {
        PreConditions.notNull(userId, "userId");
        userId2SessionCountMap.computeIfPresent(userId, (id, sessionCount) -> {
            if (sessionCount.decrementAndGet() < 0) {
                log.warn("session count is less than 0");
                throw new UnexpectedException("session count is less than 0");
            }
            return sessionCount;
        });
    }

    /**
     * 用户入队列，记录index
     * 
     * @param userId
     */
    private void addIfAbsent(String userId) {
        queueIndexMap.computeIfAbsent(userId, k -> {
            waitQueue.add(userId);
            return currentPosition.getAndIncrement();
        });
    }

    private List<String> getInactiveUserIdList() {
        List<String> inactiveUserIdList = new ArrayList<>();
        userId2SessionCountMap.forEach((userId, sessionCount) -> {
            if (sessionCount.get() < 1) {
                inactiveUserIdList.add(userId);
            }
        });
        return inactiveUserIdList;
    }

    @Getter
    public static class SessionLimitResp {
        private final boolean status;
        private final int waitNum;
        private final int estimatedWaitTime;

        public SessionLimitResp(boolean status, int waitNum, int estimatedWaitTime) {
            this.status = status;
            this.waitNum = waitNum;
            this.estimatedWaitTime = estimatedWaitTime;
        }
    }

}
