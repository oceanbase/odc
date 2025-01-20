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
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.oceanbase.odc.common.i18n.I18n;
import com.oceanbase.odc.common.util.ExceptionUtils;
import com.oceanbase.odc.common.util.StringUtils;
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
import com.oceanbase.odc.service.notification.helper.ChannelConfigValidator;
import com.oceanbase.odc.service.notification.helper.ChannelMapper;
import com.oceanbase.odc.service.notification.helper.PolicyMapper;
import com.oceanbase.odc.service.notification.model.BaseChannelConfig;
import com.oceanbase.odc.service.notification.model.Channel;
import com.oceanbase.odc.service.notification.model.Message;
import com.oceanbase.odc.service.notification.model.MessageSendResult;
import com.oceanbase.odc.service.notification.model.NotificationPolicy;
import com.oceanbase.odc.service.notification.model.QueryChannelParams;
import com.oceanbase.odc.service.notification.model.QueryMessageParams;
import com.oceanbase.odc.service.notification.model.WebhookChannelConfig;

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
@DependsOn("policyMetadataRepository")
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
    private MessageSenderMapper messageSenderMapper;
    @Autowired
    private ChannelConfigValidator validator;

    private Map<Long, NotificationPolicy> metaPolicies;

    @SkipAuthorize("odc internal usage")
    @PostConstruct
    public void init() {
        metaPolicies = policyMetadataRepository.findAllOrderByCategoryAndName().stream()
                .map(PolicyMetadataEntity::toPolicy)
                .collect(Collectors.toMap(
                        NotificationPolicy::getPolicyMetadataId, policy -> policy, (p1, p2) -> p1, LinkedHashMap::new));
    }

    @PreAuthenticate(hasAnyResourceRole = {"OWNER"}, resourceType = "ODC_PROJECT", indexOfIdParam = 0)
    public Page<Channel> listChannels(@NotNull Long projectId, @NotNull QueryChannelParams queryParams,
            @NotNull Pageable pageable) {
        Page<Channel> channels = channelRepository.find(queryParams, pageable).map(channelMapper::fromEntity);
        userService.assignCreatorNameByCreatorId(channels.getContent(), Channel::getCreatorId, Channel::setCreatorName);
        return channels;
    }

    @Transactional(rollbackFor = Exception.class)
    @PreAuthenticate(hasAnyResourceRole = {"OWNER"}, resourceType = "ODC_PROJECT", indexOfIdParam = 0)
    public Channel detailChannel(@NotNull Long projectId, @NotNull Long channelId) {
        return channelMapper.fromEntityWithConfig(nullSafeGetChannel(channelId, projectId));
    }

    @Transactional(rollbackFor = Exception.class)
    @PreAuthenticate(hasAnyResourceRole = {"OWNER"}, resourceType = "ODC_PROJECT", indexOfIdParam = 0)
    public Channel createChannel(@NotNull Long projectId, @NotNull Channel channel) {
        PreConditions.notBlank(channel.getName(), "channel.name");
        PreConditions.notNull(channel.getType(), "channel.type");
        PreConditions.validNoDuplicated(ResourceType.ODC_NOTIFICATION_CHANNEL, "channel.name", channel.getName(),
                () -> existsChannel(projectId, channel.getName()));
        validator.validate(channel.getType(), channel.getChannelConfig());

        if (StringUtils.isEmpty(channel.getChannelConfig().getTitleTemplate())) {
            channel.getChannelConfig().setTitleTemplate("${taskType}-${taskStatus}");
        }
        ChannelEntity entity = channelMapper.toEntity(channel);
        entity.setCreatorId(authenticationFacade.currentUserId());
        entity.setOrganizationId(authenticationFacade.currentOrganizationId());
        entity.setProjectId(projectId);

        return channelMapper.fromEntity(channelRepository.save(entity));
    }

    @Transactional(rollbackFor = Exception.class)
    @PreAuthenticate(hasAnyResourceRole = {"OWNER"}, resourceType = "ODC_PROJECT", indexOfIdParam = 0)
    public Channel updateChannel(@NotNull Long projectId, @NotNull Channel channel) {
        PreConditions.notNull(channel.getId(), "channel.id");
        validator.validate(channel.getType(), channel.getChannelConfig());
        ChannelEntity entity = nullSafeGetChannel(channel.getId(), projectId);

        BaseChannelConfig channelConfig = channel.getChannelConfig();
        if (channelConfig instanceof WebhookChannelConfig && ((WebhookChannelConfig) channelConfig).getSign() == null) {
            String sign = ((WebhookChannelConfig) channelMapper.fromEntityWithConfig(entity).getChannelConfig())
                    .getSign();
            ((WebhookChannelConfig) channelConfig).setSign((sign));
        }

        ChannelEntity toBeSaved = channelMapper.toEntity(channel);
        channelRepository.update(toBeSaved);

        channelPropertyRepository.deleteByChannelId(channel.getId());
        toBeSaved.getProperties().forEach(property -> channelPropertyRepository.save(property));

        return channelMapper.fromEntity(nullSafeGetChannel(channel.getId()));
    }

    @Transactional(rollbackFor = Exception.class)
    @PreAuthenticate(hasAnyResourceRole = {"OWNER"}, resourceType = "ODC_PROJECT", indexOfIdParam = 0)
    public Channel deleteChannel(@NotNull Long projectId, @NotNull Long channelId) {
        ChannelEntity entity = nullSafeGetChannel(channelId, projectId);

        channelRepository.deleteById(channelId);
        relationRepository.deleteByChannelId(channelId);

        return channelMapper.fromEntity(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    @PreAuthenticate(hasAnyResourceRole = {"OWNER"}, resourceType = "ODC_PROJECT", indexOfIdParam = 0)
    public MessageSendResult testChannel(@NotNull Long projectId, @NotNull Channel channel) {
        PreConditions.notNull(channel.getType(), "channel.type");
        PreConditions.notNull(channel.getChannelConfig(), "channel.config");
        validator.validate(channel.getType(), channel.getChannelConfig());

        BaseChannelConfig channelConfig = channel.getChannelConfig();
        if (channelConfig instanceof WebhookChannelConfig && ((WebhookChannelConfig) channelConfig).getSign() == null
                && channel.getId() != null) {
            ChannelEntity entity = nullSafeGetChannel(channel.getId(), projectId);
            String sign = ((WebhookChannelConfig) channelMapper.fromEntityWithConfig(entity).getChannelConfig())
                    .getSign();
            ((WebhookChannelConfig) channelConfig).setSign((sign));
        }

        MessageSender sender = messageSenderMapper.get(channel);
        String testMessage = I18n.translate(CHANNEL_TEST_MESSAGE_KEY, null, LocaleContextHolder.getLocale());
        try {
            return sender.send(Message.builder()
                    .title(testMessage)
                    .content(testMessage)
                    .channel(channel).build());
        } catch (Exception e) {
            return MessageSendResult.ofFail(ExceptionUtils.getRootCauseMessage(e));
        }
    }

    @PreAuthenticate(hasAnyResourceRole = {"OWNER"}, resourceType = "ODC_PROJECT", indexOfIdParam = 0)
    public Boolean existsChannel(@NotNull Long projectId, @NotBlank String channelName) {
        Optional<ChannelEntity> optional = channelRepository.findByProjectIdAndName(projectId, channelName);
        return optional.isPresent();
    }

    @PreAuthenticate(hasAnyResourceRole = {"OWNER"}, resourceType = "ODC_PROJECT", indexOfIdParam = 0)
    public List<NotificationPolicy> listPolicies(@NotNull Long projectId) {
        Map<Long, NotificationPolicy> policies = new LinkedHashMap<>(metaPolicies);

        List<NotificationPolicyEntity> actual = policyRepository.findByProjectId(projectId);
        if (CollectionUtils.isNotEmpty(actual)) {
            for (NotificationPolicyEntity entity : actual) {
                Long metadataId = entity.getPolicyMetadataId();
                NotificationPolicy policy = PolicyMapper.fromEntity(entity);
                policy.setEventName(policies.get(metadataId).getEventName());
                policy.setChannels(getChannelsByPolicyId(policy.getId()));
                policies.put(metadataId, policy);
            }
        }
        return new ArrayList<>(policies.values());
    }

    @PreAuthenticate(hasAnyResourceRole = {"OWNER"}, resourceType = "ODC_PROJECT", indexOfIdParam = 0)
    public NotificationPolicy detailPolicy(@NotNull Long projectId, @NotNull Long policyId) {
        NotificationPolicyEntity entity = nullSafeGetPolicy(policyId);
        if (!Objects.equals(projectId, entity.getProjectId())) {
            throw new AccessDeniedException("Policy does not belong to this project");
        }

        NotificationPolicy policy = PolicyMapper.fromEntity(entity);
        policy.setChannels(getChannelsByPolicyId(policy.getId()));
        return policy;
    }

    @Transactional(rollbackFor = Exception.class)
    @PreAuthenticate(hasAnyResourceRole = {"OWNER"}, resourceType = "ODC_PROJECT", indexOfIdParam = 0)
    public List<NotificationPolicy> batchUpdatePolicies(@NotNull Long projectId,
            @NotEmpty List<NotificationPolicy> policies) {
        List<NotificationPolicy> toBeCreated = new ArrayList<>();
        List<NotificationPolicy> toBeUpdated = new ArrayList<>();
        for (NotificationPolicy policy : policies) {
            if (Objects.nonNull(policy.getId())) {
                toBeUpdated.add(policy);
            } else {
                PreConditions.notNull(policy.getPolicyMetadataId(), "policy.metadataId");
                toBeCreated.add(policy);
            }
        }
        List<NotificationPolicy> results = new ArrayList<>();
        results.addAll(innerBatchCreatePolicies(projectId, toBeCreated));
        results.addAll(innerBatchUpdatePolicies(projectId, toBeUpdated));
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

    private List<NotificationPolicy> innerBatchCreatePolicies(Long projectId, List<NotificationPolicy> policies) {
        if (CollectionUtils.isEmpty(policies)) {
            return Collections.emptyList();
        }

        List<NotificationPolicyEntity> toBeCreated = new ArrayList<>();
        for (NotificationPolicy policy : policies) {
            NotificationPolicy metadata = metaPolicies.get(policy.getPolicyMetadataId());
            PreConditions.validExists(ResourceType.ODC_NOTIFICATION_POLICY,
                    "policy metadata", policy.getPolicyMetadataId(), () -> Objects.nonNull(metadata));

            NotificationPolicyEntity entity = new NotificationPolicyEntity();
            entity.setCreatorId(authenticationFacade.currentUserId());
            entity.setOrganizationId(authenticationFacade.currentOrganizationId());
            entity.setProjectId(projectId);
            entity.setPolicyMetadataId(policy.getPolicyMetadataId());
            entity.setMatchExpression(metadata.getMatchExpression());
            entity.setEnabled(policy.isEnabled());

            toBeCreated.add(entity);
        }

        List<NotificationPolicyEntity> saved = policyRepository.batchCreate(toBeCreated);

        List<Channel> channels = policies.get(0).getChannels();
        if (CollectionUtils.isNotEmpty(channels)) {
            innerBatchCreateRelation(projectId,
                    saved.stream().collect(Collectors.toMap(NotificationPolicyEntity::getId, e -> channels)));
        }
        return saved.stream().map(PolicyMapper::fromEntity).collect(Collectors.toList());
    }

    private List<NotificationPolicy> innerBatchUpdatePolicies(Long projectId, List<NotificationPolicy> policies) {
        if (CollectionUtils.isEmpty(policies)) {
            return Collections.emptyList();
        }

        Set<Long> policyIds = policies.stream().map(NotificationPolicy::getId).collect(Collectors.toSet());
        List<NotificationPolicyEntity> entities = policyRepository.findByIdIn(policyIds);
        for (NotificationPolicyEntity entity : entities) {
            if (!Objects.equals(projectId, entity.getProjectId())) {
                throw new AccessDeniedException("Policy does not belong to this project");
            }
        }

        policyRepository.updateStatusByIds(policies.get(0).isEnabled(), policyIds);

        relationRepository.deleteByNotificationPolicyIds(policyIds);
        innerBatchCreateRelation(projectId, policies.stream()
                .filter(policy -> CollectionUtils.isNotEmpty(policy.getChannels()))
                .collect(Collectors.toMap(NotificationPolicy::getId, NotificationPolicy::getChannels)));
        return entities.stream().map(PolicyMapper::fromEntity).collect(Collectors.toList());
    }

    private void innerBatchCreateRelation(Long projectId, Map<Long, List<Channel>> policyId2Channels) {
        if (MapUtils.isEmpty(policyId2Channels)) {
            return;
        }
        // check access
        Set<Long> channelIds = policyId2Channels.values().stream()
                .flatMap(Collection::stream)
                .map(Channel::getId)
                .collect(Collectors.toSet());
        List<ChannelEntity> channelEntities = channelRepository.findByIdIn(channelIds);
        for (ChannelEntity channelEntity : channelEntities) {
            if (!Objects.equals(projectId, channelEntity.getProjectId())) {
                throw new AccessDeniedException("Channel does not belong to this project, id=" + channelEntity.getId());
            }
        }

        List<NotificationChannelRelationEntity> relations = new ArrayList<>();
        for (Entry<Long, List<Channel>> entry : policyId2Channels.entrySet()) {
            for (Channel channel : entry.getValue()) {
                NotificationChannelRelationEntity relation = new NotificationChannelRelationEntity();
                relation.setChannelId(channel.getId());
                relation.setNotificationPolicyId(entry.getKey());
                relation.setOrganizationId(authenticationFacade.currentOrganizationId());
                relation.setCreatorId(authenticationFacade.currentUserId());
                relations.add(relation);
            }
        }
        relationRepository.batchCreate(relations);
    }

    private ChannelEntity nullSafeGetChannel(@NonNull Long channelId) {
        Optional<ChannelEntity> optional = channelRepository.findById(channelId);
        return optional
                .orElseThrow(() -> new NotFoundException(ResourceType.ODC_NOTIFICATION_CHANNEL, "id", channelId));
    }

    private ChannelEntity nullSafeGetChannel(@NonNull Long channelId, @NonNull Long projectId) {
        return channelRepository.findByIdAndProjectId(channelId, projectId)
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
