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

import static com.oceanbase.odc.service.notification.constant.Constants.CHANNEL_TEST_MESSAGE_KEY;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.sql.DataSource;
import javax.transaction.Transactional;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.oceanbase.odc.common.i18n.I18n;
import com.oceanbase.odc.common.util.ExceptionUtils;
import com.oceanbase.odc.core.authority.util.Authenticated;
import com.oceanbase.odc.core.authority.util.PreAuthenticate;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.AccessDeniedException;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.metadb.notification.ChannelEntity;
import com.oceanbase.odc.metadb.notification.ChannelPropertyRepository;
import com.oceanbase.odc.metadb.notification.ChannelRepository;
import com.oceanbase.odc.metadb.notification.MessageEntity;
import com.oceanbase.odc.metadb.notification.MessageRepository;
import com.oceanbase.odc.metadb.notification.NotificationChannelRelationEntity;
import com.oceanbase.odc.metadb.notification.NotificationPolicyChannelRelationRepository;
import com.oceanbase.odc.metadb.notification.NotificationPolicyEntity;
import com.oceanbase.odc.metadb.notification.NotificationPolicyRepository;
import com.oceanbase.odc.metadb.notification.PolicyMetadataEntity;
import com.oceanbase.odc.metadb.notification.PolicyMetadataRepository;
import com.oceanbase.odc.service.iam.UserService;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.notification.helper.ChannelMapper;
import com.oceanbase.odc.service.notification.helper.PolicyMapper;
import com.oceanbase.odc.service.notification.model.Channel;
import com.oceanbase.odc.service.notification.model.Message;
import com.oceanbase.odc.service.notification.model.NotificationPolicy;
import com.oceanbase.odc.service.notification.model.QueryChannelParams;
import com.oceanbase.odc.service.notification.model.QueryMessageParams;
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
    private NotificationPolicyRepository policyRepository;
    @Autowired
    private PolicyMetadataRepository policyMetadataRepository;
    @Autowired
    private MessageRepository messageRepository;
    @Autowired
    private NotificationPolicyChannelRelationRepository relationRepository;
    @Autowired
    private DataSource dataSource;
    @Autowired
    private UserService userService;
    @Autowired
    private AuthenticationFacade authenticationFacade;
    @Autowired
    private ChannelMapper channelMapper;
    @Autowired
    private PolicyMapper policyMapper;
    @Autowired
    private ChannelFactory channelFactory;


    @PreAuthenticate(hasAnyResourceRole = {"OWNER"}, resourceType = "ODC_PROJECT", indexOfIdParam = 0)
    public Page<Channel> listChannels(@NotNull Long projectId, @NotNull QueryChannelParams queryParams,
            @NotNull Pageable pageable) {
        Page<Channel> channels = channelRepository.find(queryParams, pageable).map(channelMapper::fromEntity);
        userService.assignCreatorNameByCreatorId(channels.getContent(), Channel::getCreatorId, Channel::setCreatorName);
        return channels;
    }

    @Transactional
    @PreAuthenticate(hasAnyResourceRole = {"OWNER"}, resourceType = "ODC_PROJECT", indexOfIdParam = 0)
    public Channel detailChannel(@NotNull Long projectId, @NotNull Long channelId) {
        Channel channel = channelMapper.fromEntityWithConfig(nullSafeGetChannel(channelId));
        if (!Objects.equals(projectId, channel.getProjectId())) {
            throw new AccessDeniedException("Channel does not belong to this project");
        }
        return channel;
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
        relationRepository.deleteByChannelId(channelId);

        return channelMapper.fromEntity(entity);
    }

    @SkipAuthorize("Any user could test channel")
    public TestChannelResult testChannel(@NotNull Channel channel) {
        PreConditions.notNull(channel.getType(), "channel.type");
        PreConditions.notNull(channel.getChannelConfig(), "channel.config");
        MessageChannel sender = channelFactory.generate(channel);

        String testMessage = I18n.translate(CHANNEL_TEST_MESSAGE_KEY, null, LocaleContextHolder.getLocale());
        try {
            sender.send(Message.builder().content(testMessage).build());
        } catch (Exception e) {
            return TestChannelResult.ofFail(ExceptionUtils.getRootCauseMessage(e));
        }
        return TestChannelResult.ofSuccess();
    }

    @PreAuthenticate(hasAnyResourceRole = {"OWNER"}, resourceType = "ODC_PROJECT", indexOfIdParam = 0)
    public Boolean existsChannel(@NotNull Long projectId, @NotBlank String channelName) {
        Optional<ChannelEntity> optional = channelRepository.findByProjectIdAndName(projectId, channelName);
        return optional.isPresent();
    }

    @PreAuthenticate(hasAnyResourceRole = {"OWNER"}, resourceType = "ODC_PROJECT", indexOfIdParam = 0)
    public List<NotificationPolicy> listPolicies(@NotNull Long projectId) {
        TreeMap<Long, NotificationPolicy> meta = policyMetadataRepository.findAll(Sort.by("id")).stream()
                .map(PolicyMetadataEntity::toPolicy)
                .collect(Collectors.toMap(
                        NotificationPolicy::getPolicyMetadataId, policy -> policy, (p1, p2) -> p1, TreeMap::new));

        List<NotificationPolicyEntity> actual = policyRepository.findByProjectId(projectId);
        if (CollectionUtils.isNotEmpty(actual)) {
            for (NotificationPolicyEntity policyInDb : actual) {
                NotificationPolicy policyInMeta = meta.get(policyInDb.getPolicyMetadataId());
                policyInMeta.setId(policyInDb.getId());
                policyInMeta.setEnabled(policyInDb.isEnabled());
                policyInMeta.setChannels(getChannelsByPolicyId(policyInDb.getId()));
            }
        }
        return new ArrayList<>(meta.values());
    }

    @PreAuthenticate(hasAnyResourceRole = {"OWNER"}, resourceType = "ODC_PROJECT", indexOfIdParam = 0)
    public NotificationPolicy detailPolicy(@NotNull Long projectId, @NotNull Long policyId) {
        NotificationPolicyEntity entity = nullSafeGetPolicy(policyId);
        if (!Objects.equals(projectId, entity.getProjectId())) {
            throw new AccessDeniedException("Policy does not belong to this project");
        }

        NotificationPolicy policy = policyMapper.fromEntity(entity);
        policy.setChannels(getChannelsByPolicyId(policy.getId()));
        return policy;
    }

    @Transactional
    @PreAuthenticate(hasAnyResourceRole = {"OWNER"}, resourceType = "ODC_PROJECT", indexOfIdParam = 0)
    public List<NotificationPolicy> batchUpdatePolicies(@NotNull Long projectId,
            @NotEmpty List<NotificationPolicy> policies) {
        List<NotificationPolicy> results = new ArrayList<>();
        for (NotificationPolicy policy : policies) {
            if (Objects.nonNull(policy.getId())) {
                results.add(innerUpdatePolicy(projectId, policy));
            } else {
                PreConditions.notNull(policy.getPolicyMetadataId(), "policy.metadataId");
                results.add(innerCreatePolicy(projectId, policy));
            }
        }
        return results;
    }

    @PreAuthenticate(hasAnyResourceRole = {"OWNER"}, resourceType = "ODC_PROJECT", indexOfIdParam = 0)
    public Page<Message> listMessages(@NotNull Long projectId, @NotNull QueryMessageParams queryParams,
            @NotNull Pageable pageable) {
        return messageRepository.find(queryParams, pageable).map(Message::fromEntity);
    }

    @PreAuthenticate(hasAnyResourceRole = {"OWNER"}, resourceType = "ODC_PROJECT", indexOfIdParam = 0)
    public Message detailMessage(@NotNull Long projectId, @NotNull Long messageId) {
        Message message = Message.fromEntity(nullSafeGetMessage(messageId));
        if (!Objects.equals(projectId, message.getProjectId())) {
            throw new AccessDeniedException("Message does not belong to this project");
        }
        return message;
    }

    private NotificationPolicy innerCreatePolicy(Long projectId, NotificationPolicy policy) {
        Optional<PolicyMetadataEntity> metadata = policyMetadataRepository.findById(policy.getPolicyMetadataId());
        PreConditions.validExists(ResourceType.ODC_NOTIFICATION_POLICY,
                "policy metadata", policy.getPolicyMetadataId(), metadata::isPresent);

        NotificationPolicyEntity entity = new NotificationPolicyEntity();
        entity.setCreatorId(authenticationFacade.currentUserId());
        entity.setOrganizationId(authenticationFacade.currentOrganizationId());
        entity.setProjectId(projectId);
        entity.setPolicyMetadataId(policy.getPolicyMetadataId());
        entity.setMatchExpression(metadata.get().getMatchExpression());
        entity.setEnabled(policy.isEnabled());

        NotificationPolicyEntity saved = policyRepository.save(entity);
        innerCreateRelation(projectId, saved.getId(), policy.getChannels());
        return policyMapper.fromEntity(saved);
    }

    private NotificationPolicy innerUpdatePolicy(Long projectId, NotificationPolicy policy) {
        NotificationPolicyEntity entity = nullSafeGetPolicy(policy.getId());
        if (!Objects.equals(projectId, entity.getProjectId())) {
            throw new AccessDeniedException("Policy does not belong to this project");
        }

        if (!Objects.equals(entity.isEnabled(), policy.isEnabled())) {
            entity.setEnabled(policy.isEnabled());
            policyRepository.save(entity);
        }
        relationRepository.deleteByNotificationPolicyId(entity.getId());
        innerCreateRelation(projectId, entity.getId(), policy.getChannels());
        return policyMapper.fromEntity(entity);
    }

    private void innerCreateRelation(Long projectId, Long policyId, List<Channel> channels) {
        if (CollectionUtils.isEmpty(channels)) {
            return;
        }
        for (Channel channel : channels) {
            ChannelEntity entity = nullSafeGetChannel(channel.getId());
            if (!Objects.equals(projectId, entity.getProjectId())) {
                throw new AccessDeniedException("Channel does not belong to this project");
            }
            NotificationChannelRelationEntity relation = new NotificationChannelRelationEntity();
            relation.setChannelId(entity.getId());
            relation.setNotificationPolicyId(policyId);
            relation.setOrganizationId(authenticationFacade.currentOrganizationId());
            relation.setCreatorId(authenticationFacade.currentUserId());
            relationRepository.save(relation);
        }
    }

    private ChannelEntity nullSafeGetChannel(@NonNull Long channelId) {
        Optional<ChannelEntity> optional = channelRepository.findById(channelId);
        return optional
                .orElseThrow(() -> new NotFoundException(ResourceType.ODC_NOTIFICATION_CHANNEL, "id", channelId));
    }

    private NotificationPolicyEntity nullSafeGetPolicy(@NonNull Long policyId) {
        Optional<NotificationPolicyEntity> optional = policyRepository.findById(policyId);
        return optional
                .orElseThrow(() -> new NotFoundException(ResourceType.ODC_NOTIFICATION_POLICY, "id", policyId));
    }

    private MessageEntity nullSafeGetMessage(@NonNull Long messageId) {
        Optional<MessageEntity> optional = messageRepository.findById(messageId);
        return optional
                .orElseThrow(() -> new NotFoundException(ResourceType.ODC_NOTIFICATION_MESSAGE, "id", messageId));
    }

    private List<Channel> getChannelsByPolicyId(Long policyId) {
        return channelRepository.findByPolicyId(policyId)
                .stream()
                .map(channelMapper::fromEntity)
                .collect(Collectors.toList());
    }

}
