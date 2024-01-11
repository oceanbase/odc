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
package com.oceanbase.odc.service.notification;

import java.util.Objects;
import java.util.Optional;

import javax.transaction.Transactional;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.oceanbase.odc.core.authority.util.Authenticated;
import com.oceanbase.odc.core.authority.util.PreAuthenticate;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.AccessDeniedException;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.core.shared.exception.NotImplementedException;
import com.oceanbase.odc.metadb.notification.ChannelEntity;
import com.oceanbase.odc.metadb.notification.ChannelPropertyRepository;
import com.oceanbase.odc.metadb.notification.ChannelRepository;
import com.oceanbase.odc.service.iam.UserService;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.notification.helper.ChannelMapper;
import com.oceanbase.odc.service.notification.model.Channel;
import com.oceanbase.odc.service.notification.model.ChannelSpecs;
import com.oceanbase.odc.service.notification.model.QueryChannelParams;
import com.oceanbase.odc.service.notification.model.TestChannelResult;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @author liuyizhuo.lyz
 * @date 2024/1/4
 */
@Service
@Authenticated
@Slf4j
@Validated
public class NotificationService {

    @Autowired
    private ChannelRepository channelRepository;
    @Autowired
    private ChannelPropertyRepository channelPropertyRepository;
    @Autowired
    private UserService userService;
    @Autowired
    private AuthenticationFacade authenticationFacade;
    @Autowired
    private ChannelMapper channelMapper;

    @PreAuthenticate(hasAnyResourceRole = {"OWNER"}, resourceType = "ODC_PROJECT", indexOfIdParam = 0)
    public Page<Channel> listChannels(@NotNull Long projectId, @NotNull QueryChannelParams queryParams,
            @NotNull Pageable pageable) {
        Specification<ChannelEntity> specs = ChannelSpecs.projectIdEquals(projectId)
                .and(ChannelSpecs.nameLike(queryParams.getFuzzyChannelName()))
                .and(ChannelSpecs.typeIn(queryParams.getChannelTypes()));

        Page<Channel> channels = channelRepository.findAll(specs, pageable).map(channelMapper::fromEntity);
        userService.assignCreatorNameByCreatorId(channels.getContent(), Channel::getCreatorId, Channel::setCreatorName);
        return channels;
    }

    @Transactional
    @PreAuthenticate(hasAnyResourceRole = {"OWNER"}, resourceType = "ODC_PROJECT", indexOfIdParam = 0)
    public Channel detailChannel(@NotNull Long projectId, @NotNull Long channelId) {
        return channelMapper.fromEntityWithConfig(nullSafeGetChannel(channelId));
    }

    @Transactional
    @PreAuthenticate(hasAnyResourceRole = {"OWNER"}, resourceType = "ODC_PROJECT", indexOfIdParam = 0)
    public Channel createChannel(@NotNull Long projectId, @NotNull Channel channel) {
        PreConditions.notBlank(channel.getName(), "channel.name");
        PreConditions.notNull(channel.getType(), "channel.type");
        PreConditions.validNoDuplicated(ResourceType.ODC_NOTIFICATION_CHANNEL, "channel.name", channel.getName(),
                () -> existsChannel(projectId, channel.getName()));

        ChannelEntity entity = channelMapper.toEntity(channel);
        entity.setCreatorId(authenticationFacade.currentUserId());
        entity.setOrganizationId(authenticationFacade.currentOrganizationId());
        entity.setProjectId(projectId);

        return channelMapper.fromEntity(channelRepository.save(entity));
    }

    @Transactional
    @PreAuthenticate(hasAnyResourceRole = {"OWNER"}, resourceType = "ODC_PROJECT", indexOfIdParam = 0)
    public Channel updateChannel(@NotNull Long projectId, @NotNull Channel channel) {
        PreConditions.notNull(channel.getId(), "channel.id");
        ChannelEntity entity = nullSafeGetChannel(channel.getId());
        if (!Objects.equals(projectId, entity.getProjectId())) {
            throw new AccessDeniedException("Channel does not belong to this project");
        }

        channelPropertyRepository.deleteByChannelId(entity.getId());

        ChannelEntity toBeSaved = channelMapper.toEntity(channel);
        toBeSaved.setCreatorId(authenticationFacade.currentUserId());
        toBeSaved.setOrganizationId(authenticationFacade.currentOrganizationId());
        toBeSaved.setProjectId(projectId);

        return channelMapper.fromEntity(channelRepository.save(toBeSaved));
    }

    @Transactional
    @PreAuthenticate(hasAnyResourceRole = {"OWNER"}, resourceType = "ODC_PROJECT", indexOfIdParam = 0)
    public Channel deleteChannel(@NotNull Long projectId, @NotNull Long channelId) {
        ChannelEntity entity = nullSafeGetChannel(channelId);
        if (!Objects.equals(projectId, entity.getProjectId())) {
            throw new AccessDeniedException("Channel does not belong to this project");
        }

        channelRepository.deleteById(channelId);

        return channelMapper.fromEntity(entity);
    }

    @SkipAuthorize("Any user could test channel")
    public TestChannelResult testChannel(@NotNull Channel channel) {
        throw new NotImplementedException();
    }

    @PreAuthenticate(hasAnyResourceRole = {"OWNER"}, resourceType = "ODC_PROJECT", indexOfIdParam = 0)
    public Boolean existsChannel(@NotNull Long projectId, @NotBlank String channelName) {
        Optional<ChannelEntity> optional = channelRepository.findByProjectIdAndName(projectId, channelName);
        return optional.isPresent();
    }

    private ChannelEntity nullSafeGetChannel(@NonNull Long channelId) {
        Optional<ChannelEntity> optional = channelRepository.findById(channelId);
        return optional
                .orElseThrow(() -> new NotFoundException(ResourceType.ODC_NOTIFICATION_CHANNEL, "id", channelId));
    }

}
