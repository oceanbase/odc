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
package com.oceanbase.odc.metadb.notification;

import java.util.List;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/**
 * @author liuyizhuo.lyz
 * @date 2024/1/10
 */
public interface ChannelPropertyRepository extends JpaRepository<ChannelPropertyEntity, Long> {

    @Transactional
    @Modifying
    @Query(value = "delete from notification_channel_property where channel_id=?1", nativeQuery = true)
    int deleteByChannelId(Long channelId);

    @Transactional
    @Modifying
    @Query(value = "delete from notification_channel_property where channel_id=?1 and key_string!=?2",
            nativeQuery = true)
    int deleteByChannelIdAndKeyNotEquals(Long channelId, String key);

    @Transactional
    List<ChannelPropertyEntity> findAllByChannelId(Long channelId);

}
